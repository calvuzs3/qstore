package net.calvuz.qstore.data.repository

import android.util.Log
import net.calvuz.qstore.data.local.database.ArticleImageDao
import net.calvuz.qstore.data.local.entity.ArticleImageEntity
import net.calvuz.qstore.data.local.storage.ImageStorageManager
import net.calvuz.qstore.data.mapper.ArticleImageMapper
import net.calvuz.qstore.data.opencv.FeatureExtractor
import net.calvuz.qstore.data.opencv.OpenCVManager
import net.calvuz.qstore.domain.model.ArticleImage
import net.calvuz.qstore.domain.repository.ImageRecognitionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.data.opencv.ConfigurableImageMatcher
import javax.inject.Inject

/**
 * Implementazione completa del repository per riconoscimento immagini con OpenCV
 */
class ImageRecognitionRepositoryImpl @Inject constructor(
    private val articleImageDao: ArticleImageDao,
    private val imageStorageManager: ImageStorageManager,
    private val featureExtractor: FeatureExtractor,
    private val imageMatcher: ConfigurableImageMatcher,
    private val openCVManager: OpenCVManager,
    private val mapper: ArticleImageMapper
) : ImageRecognitionRepository {

    override suspend fun saveArticleImage(
        articleUuid: String,
        imageData: ByteArray
    ): Result<ArticleImage> {
        return try {
            // Verifica OpenCV inizializzato
            if (!openCVManager.isInitialized()) {
                return Result.failure(IllegalStateException("OpenCV not initialized"))
            }

            // 1. Salva immagine su file system
            val imagePath = imageStorageManager.saveImage(imageData, articleUuid)
                .getOrElse { return Result.failure(it) }

            // 2. Estrai features OpenCV
            val featuresData = featureExtractor.extractFeatures(imageData)
                .getOrElse {
                    // Cleanup: elimina immagine se estrazione fallisce
                    imageStorageManager.deleteImage(imagePath)
                    return Result.failure(it)
                }

            // 3. Crea entity e salva su database
            val entity = ArticleImageEntity(
                articleUuid = articleUuid,
                imagePath = imagePath,
                featuresData = featuresData,
                createdAt = System.currentTimeMillis()
            )

            val imageId = articleImageDao.insert(entity)

            // 4. Ritorna domain model con ID generato
            val savedEntity = entity.copy(id = imageId)
            val articleImage = mapper.toDomain(savedEntity)

            Result.success(articleImage)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getArticleImages(articleUuid: String): Result<List<ArticleImage>> {
        return try {
            val entities = articleImageDao.getByArticleUuid(articleUuid)
            val images = mapper.toDomainList(entities)
            Result.success(images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getArticleImageById(imageId: Long): Result<ArticleImage?> {
        return try {
            val entity = articleImageDao.getById(imageId)
            val domainModel = entity?.let { mapper.toDomain(it) }
            Result.success(domainModel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeArticleImages(articleUuid: String): Flow<List<ArticleImage>> {
        return articleImageDao.observeByArticleUuid(articleUuid).map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override suspend fun deleteImage(imageId: Long): Result<Unit> {
        return try {
            val image = articleImageDao.getById(imageId)
            if (image != null) {
                // Elimina file fisico
                imageStorageManager.deleteImage(image.imagePath)
                    .getOrElse {
                        // Log warning ma continua comunque
                    }

                // Elimina da database
                articleImageDao.delete(image)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Image not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteImages(articleUuid: String): Result<Int> {
        return try {
            // Recupera tutte le immagini
            val images = articleImageDao.getByArticleUuid(articleUuid)

            // Elimina tutti i file
            images.forEach { image ->
                imageStorageManager.deleteImage(image.imagePath)
            }

            // Elimina da DB
            val deletedCount = articleImageDao.deleteByArticleUuid(articleUuid)

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchArticlesByImage(
        imageData: ByteArray,
        threshold: Double
    ): Result<List<String>> {
        return try {
            Log.d("ImageSearch", "🔍 START - Threshold: $threshold")

            // Verifica OpenCV inizializzato
            if (!openCVManager.isInitialized()) {
                Log.e("ImageSearch", "❌ OpenCV not initialized")
                return Result.failure(IllegalStateException("OpenCV not initialized"))
            }

            // 1. Estrai features dall'immagine query
            Log.d("ImageSearch", "📸 Extracting query features...")
            val queryFeatures = featureExtractor.extractFeatures(imageData)
                .getOrElse {
                    Log.e("ImageSearch", "❌ Failed to extract features: ${it.message}")
                    return Result.failure(it)
                }
            Log.d("ImageSearch", "✅ Query features extracted: ${queryFeatures.size} bytes")

            val queryDescriptors = featureExtractor.deserializeDescriptors(queryFeatures)
                .getOrElse {
                    Log.e("ImageSearch", "❌ Failed to deserialize: ${it.message}")
                    return Result.failure(it)
                }
            Log.d("ImageSearch", "✅ Query descriptors: ${queryDescriptors.rows()} features")

            // 2. Ottieni tutte le immagini salvate
            val allImages = articleImageDao.getAll()
            Log.d("ImageSearch", "📦 Database images: ${allImages.size}")

            if (allImages.isEmpty()) {
                Log.w("ImageSearch", "⚠️ No images in database")
                queryDescriptors.release()
                return Result.success(emptyList())
            }

            // 3. Deserializza tutti i descriptors
            val databaseDescriptors = allImages.mapNotNull { image ->
                featureExtractor.deserializeDescriptors(image.featuresData)
                    .onSuccess {
                        Log.d("ImageSearch", "  ✓ Image ${image.id}: ${it.rows()} features")
                    }
                    .onFailure {
                        Log.e("ImageSearch", "  ✗ Image ${image.id}: Failed to deserialize")
                    }
                    .getOrNull()
            }
            Log.d("ImageSearch", "✅ Deserialized ${databaseDescriptors.size}/${allImages.size} images")

            // 4. Trova best matches
            Log.d("ImageSearch", "🔄 Finding matches...")
            val matchResults = imageMatcher.findBestMatches(
                queryDescriptors,
                databaseDescriptors,
                threshold
            ).getOrElse {
                Log.e("ImageSearch", "❌ Matching failed: ${it.message}")
                queryDescriptors.release()
                databaseDescriptors.forEach { it.release() }
                return Result.failure(it)
            }

            Log.d("ImageSearch", "🎯 Matches found: ${matchResults.size}")
            matchResults.forEach { result ->
                Log.d("ImageSearch", "  Match: index=${result.index}, similarity=${result.similarity}")
            }

            // 5. Mappa indici a articleUuid
            val matchedArticleUuids = matchResults.map { result ->
                allImages[result.index].articleUuid
            }.distinct()

            Log.d("ImageSearch", "✅ Final articles: ${matchedArticleUuids.size}")

            // 6. Cleanup OpenCV Mats
            queryDescriptors.release()
            databaseDescriptors.forEach { it.release() }

            Result.success(matchedArticleUuids)

        } catch (e: Exception) {
            Log.e("ImageSearch", "💥 Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getImagePath(imageId: Long): Result<String?> {
        return try {
            val image = articleImageDao.getById(imageId)
            val fullPath = image?.let {
                imageStorageManager.getFullPath(it.imagePath)
            }
            Result.success(fullPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package net.calvuz.qstore.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager per gestione immagini su file system interno
 */
@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imagesDir: File by lazy {
        File(context.filesDir, IMAGES_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Salva un'immagine e ritorna il path relativo
     *
     * @param imageData ByteArray dell'immagine (JPEG/PNG)
     * @param articleUuid UUID dell'articolo (per organizzare le immagini)
     * @return Path relativo dell'immagine salvata
     */
    fun saveImage(imageData: ByteArray, articleUuid: String): Result<String> {
        return try {
            // Crea directory per l'articolo se non esiste
            val articleDir = File(imagesDir, articleUuid).apply {
                if (!exists()) {
                    mkdirs()
                }
            }

            // Genera nome file unico
            val fileName = "${UUID.randomUUID()}.jpg"
            val imageFile = File(articleDir, fileName)

            // Decodifica e ricomprimi per normalizzare il formato
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return Result.failure(IllegalArgumentException("Invalid image data"))

            // Salva come JPEG con compressione
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            bitmap.recycle()

            // Ritorna path relativo: articleUuid/fileName
            val relativePath = "$articleUuid/$fileName"
            Result.success(relativePath)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Legge un'immagine dal path relativo
     *
     * @param relativePath Path relativo (es: "uuid/image.jpg")
     * @return ByteArray dell'immagine
     */
    fun readImage(relativePath: String): Result<ByteArray> {
        return try {
            val imageFile = File(imagesDir, relativePath)

            if (!imageFile.exists()) {
                return Result.failure(IllegalArgumentException("Image not found: $relativePath"))
            }

            val imageData = imageFile.readBytes()
            Result.success(imageData)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Legge un'immagine come Bitmap
     */
     fun readImageAsBitmap(relativePath: String): Result<Bitmap> {
        return try {
            val imageFile = File(imagesDir, relativePath)

            if (!imageFile.exists()) {
                return Result.failure(IllegalArgumentException("Image not found: $relativePath"))
            }

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: return Result.failure(IllegalArgumentException("Failed to decode image"))

            Result.success(bitmap)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un'immagine
     */
     fun deleteImage(relativePath: String): Result<Unit> {
        return try {
            val imageFile = File(imagesDir, relativePath)

            if (imageFile.exists()) {
                imageFile.delete()
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina tutte le immagini di un articolo
     */
     fun deleteAllImagesForArticle(articleUuid: String): Result<Unit> {
        return try {
            val articleDir = File(imagesDir, articleUuid)

            if (articleDir.exists() && articleDir.isDirectory) {
                articleDir.deleteRecursively()
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene il path completo di un'immagine
     */
    fun getFullPath(relativePath: String): String {
        return File(imagesDir, relativePath).absolutePath
    }

    /**
     * Ottiene lo spazio occupato dalle immagini (in bytes)
     */
     fun getUsedSpace(): Long {
        return try {
            imagesDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        private const val IMAGES_DIRECTORY = "article_images"
        private const val JPEG_QUALITY = 100 // 0-100
    }
}
package net.calvuz.qstore.data.opencv

import android.graphics.BitmapFactory
import android.util.Log
import net.calvuz.qstore.data.local.database.ArticleImageDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validatore per sistema di riconoscimento immagini
 * Fornisce strumenti per debug e validazione della qualità
 */
@Singleton
class ImageRecognitionValidator @Inject constructor(
    private val featureExtractor: FeatureExtractor,
    private val configurableImageMatcher: ConfigurableImageMatcher, // Cambiato da ImageMatcher
    private val openCVManager: OpenCVManager,
    private val articleImageDao: ArticleImageDao
) {

    /**
     * Valida la qualità di un'immagine per il riconoscimento
     */
    fun validateImageQuality(imageData: ByteArray): ImageQualityResult {
        try {
            // 1. Verifica formato immagine
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return ImageQualityResult(
                    isValid = false,
                    score = 0.0,
                    issues = listOf("Formato immagine non valido")
                )

            val issues = mutableListOf<String>()
            var qualityScore = 1.0

            // 2. Controlla risoluzione
            val width = bitmap.width
            val height = bitmap.height
            val totalPixels = width * height

            when {
                totalPixels < MIN_PIXELS -> {
                    issues.add("Risoluzione troppo bassa (${width}x${height})")
                    qualityScore -= 0.3
                }
                totalPixels > MAX_PIXELS -> {
                    issues.add("Risoluzione molto alta, verrà ridimensionata")
                    qualityScore -= 0.1
                }
            }

            // 3. Controlla aspect ratio
            val aspectRatio = maxOf(width, height).toDouble() / minOf(width, height)
            if (aspectRatio > MAX_ASPECT_RATIO) {
                issues.add("Immagine troppo allungata (ratio: $aspectRatio)")
                qualityScore -= 0.2
            }

            // 4. Testa estrazione features
            val featuresResult = featureExtractor.extractFeaturesFromBitmap(bitmap)
            bitmap.recycle()

            when {
                featuresResult.isFailure -> {
                    issues.add("Impossibile estrarre features: ${featuresResult.exceptionOrNull()?.message}")
                    qualityScore = 0.0
                }
                else -> {
                    val featuresData = featuresResult.getOrNull()!!
                    val descriptors = featureExtractor.deserializeDescriptors(featuresData)
                        .getOrNull()

                    if (descriptors != null) {
                        val featureCount = descriptors.rows()
                        descriptors.release()

                        when {
                            featureCount < MIN_FEATURES -> {
                                issues.add("Poche features rilevate ($featureCount < $MIN_FEATURES)")
                                qualityScore -= 0.4
                            }
                            featureCount < IDEAL_FEATURES -> {
                                issues.add("Features sufficienti ma non ottimali ($featureCount)")
                                qualityScore -= 0.2
                            }
                            else -> {
                                Log.d(TAG, "✅ Ottime features: $featureCount")
                            }
                        }
                    } else {
                        issues.add("Errore deserializzazione features")
                        qualityScore -= 0.5
                    }
                }
            }

            // 5. Validazione formato file
            val fileSize = imageData.size
            when {
                fileSize < MIN_FILE_SIZE -> {
                    issues.add("File troppo piccolo (${fileSize / 1024}KB)")
                    qualityScore -= 0.2
                }
                fileSize > MAX_FILE_SIZE -> {
                    issues.add("File molto grande (${fileSize / 1024 / 1024}MB)")
                    qualityScore -= 0.1
                }
            }

            qualityScore = qualityScore.coerceIn(0.0, 1.0)

            return ImageQualityResult(
                isValid = qualityScore > 0.2,
                score = qualityScore,
                issues = issues,
                featureCount = null,
                resolution = "$width x $height",
                fileSize = fileSize
            )

        } catch (e: Exception) {
            Log.e(TAG, "Errore validazione immagine", e)
            return ImageQualityResult(
                isValid = false,
                score = 0.0,
                issues = listOf("Errore interno: ${e.message}")
            )
        }
    }

    /**
     * Testa la performance di matching contro database esistente
     */
    suspend fun testMatchingPerformance(testImageData: ByteArray): MatchingTestResult {
        val startTime = System.currentTimeMillis()

        try {
            // 1. Valida immagine test
            val qualityResult = validateImageQuality(testImageData)
            if (!qualityResult.isValid) {
                return MatchingTestResult(
                    success = false,
                    error = "Immagine non valida: ${qualityResult.issues.joinToString()}"
                )
            }

            // 2. Estrai features da immagine test
            val testFeatures = featureExtractor.extractFeatures(testImageData)
                .getOrElse {
                    return MatchingTestResult(
                        success = false,
                        error = "Estrazione features fallita: ${it.message}"
                    )
                }

            val testDescriptors = featureExtractor.deserializeDescriptors(testFeatures)
                .getOrElse {
                    return MatchingTestResult(
                        success = false,
                        error = "Deserializzazione fallita: ${it.message}"
                    )
                }

            // 3. Recupera tutte le immagini dal database
            val allImages = articleImageDao.getAll()
            val databaseDescriptors = allImages.mapNotNull { image ->
                featureExtractor.deserializeDescriptors(image.featuresData).getOrNull()
            }

            // 4. Testa matching con matcher configurabile
            val matchingStartTime = System.currentTimeMillis()
            val matchResults = configurableImageMatcher.findBestMatches(
                testDescriptors,
                databaseDescriptors,
                0.3 // Soglia bassa per test
            ).getOrElse {
                return MatchingTestResult(
                    success = false,
                    error = "Matching fallito: ${it.message}"
                )
            }
            val matchingTime = System.currentTimeMillis() - matchingStartTime

            // 5. Cleanup
            testDescriptors.release()
            databaseDescriptors.forEach { it.release() }

            val totalTime = System.currentTimeMillis() - startTime

            return MatchingTestResult(
                success = true,
                totalImagesInDb = allImages.size,
                validDescriptors = databaseDescriptors.size,
                matchesFound = matchResults.size,
                bestSimilarity = matchResults.maxOfOrNull { it.similarity } ?: 0.0,
                totalTimeMs = totalTime,
                matchingTimeMs = matchingTime,
                imageQuality = qualityResult
            )

        } catch (e: Exception) {
            return MatchingTestResult(
                success = false,
                error = "Errore test: ${e.message}"
            )
        }
    }

    /**
     * Analizza la distribuzione di qualità nel database
     */
    suspend fun analyzeDatabaseQuality(): DatabaseAnalysisResult {
        try {
            val allImages = articleImageDao.getAll()
            val qualityScores = mutableListOf<Double>()
            val featureCounts = mutableListOf<Int>()
            val issues = mutableListOf<String>()

            var validImages = 0
            var corruptedImages = 0

            for (image in allImages) {
                try {
                    val descriptors = featureExtractor.deserializeDescriptors(image.featuresData)
                        .getOrNull()

                    if (descriptors != null) {
                        val featureCount = descriptors.rows()
                        featureCounts.add(featureCount)
                        descriptors.release()

                        val quality = when {
                            featureCount >= IDEAL_FEATURES -> 1.0
                            featureCount >= MIN_FEATURES -> 0.7
                            else -> 0.3
                        }
                        qualityScores.add(quality)
                        validImages++
                    } else {
                        corruptedImages++
                        issues.add("Immagine corrotta: ID ${image.id}")
                    }
                } catch (e: Exception) {
                    corruptedImages++
                    issues.add("Errore immagine ID ${image.id}: ${e.message}")
                }
            }

            val averageQuality = if (qualityScores.isNotEmpty()) qualityScores.average() else 0.0
            val averageFeatures = if (featureCounts.isNotEmpty()) featureCounts.average() else 0.0

            return DatabaseAnalysisResult(
                totalImages = allImages.size,
                validImages = validImages,
                corruptedImages = corruptedImages,
                averageQuality = averageQuality,
                averageFeatures = averageFeatures.toInt(),
                qualityDistribution = qualityScores,
                issues = issues
            )

        } catch (e: Exception) {
            return DatabaseAnalysisResult(
                totalImages = 0,
                validImages = 0,
                corruptedImages = 0,
                averageQuality = 0.0,
                averageFeatures = 0,
                qualityDistribution = emptyList(),
                issues = listOf("Errore analisi: ${e.message}")
            )
        }
    }

    /**
     * Risultato validazione qualità immagine
     */
    data class ImageQualityResult(
        val isValid: Boolean,
        val score: Double, // 0.0 - 1.0
        val issues: List<String>,
        val featureCount: Int? = null,
        val resolution: String? = null,
        val fileSize: Int? = null
    ) {
        fun getQualityLabel(): String = when {
            score >= 0.8 -> "Eccellente"
            score >= 0.6 -> "Buona"
            score >= 0.4 -> "Accettabile"
            score >= 0.2 -> "Scarsa"
            else -> "Inadeguata"
        }
    }

    /**
     * Risultato test performance matching
     */
    data class MatchingTestResult(
        val success: Boolean,
        val error: String? = null,
        val totalImagesInDb: Int = 0,
        val validDescriptors: Int = 0,
        val matchesFound: Int = 0,
        val bestSimilarity: Double = 0.0,
        val totalTimeMs: Long = 0,
        val matchingTimeMs: Long = 0,
        val imageQuality: ImageQualityResult? = null
    )

    /**
     * Risultato analisi database
     */
    data class DatabaseAnalysisResult(
        val totalImages: Int,
        val validImages: Int,
        val corruptedImages: Int,
        val averageQuality: Double,
        val averageFeatures: Int,
        val qualityDistribution: List<Double>,
        val issues: List<String>
    )

    companion object {
        private const val TAG = "ImageValidator"

        // Parametri qualità immagine
        private const val MIN_PIXELS = 50_000      // ~224x224
        private const val MAX_PIXELS = 2_000_000   // ~1414x1414
        private const val MAX_ASPECT_RATIO = 3.0   // 3:1 max
        private const val MIN_FILE_SIZE = 5_000    // 5KB
        private const val MAX_FILE_SIZE = 10_000_000 // 10MB

        // Parametri features
        private const val MIN_FEATURES = 30
        private const val IDEAL_FEATURES = 200
    }
}
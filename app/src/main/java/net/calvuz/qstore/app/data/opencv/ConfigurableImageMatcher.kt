package net.calvuz.qstore.app.data.opencv

import android.util.Log
import kotlinx.coroutines.flow.first
import net.calvuz.qstore.settings.domain.model.RecognitionSettings
import net.calvuz.qstore.settings.domain.repository.RecognitionSettingsRepository
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.DescriptorMatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * ImageMatcher con parametri configurabili da settings
 */
@Singleton
class ConfigurableImageMatcher @Inject constructor(
    private val openCVManager: OpenCVManager,
    private val settingsRepository: RecognitionSettingsRepository
) {
    private val matcher: BFMatcher by lazy {
        BFMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING, false)
    }

    /**
     * Confronta due set di features usando impostazioni correnti
     */
    suspend fun matchFeatures(descriptors1: Mat, descriptors2: Mat): Result<Double> {
        if (!openCVManager.isInitialized()) {
            return Result.failure(IllegalStateException("OpenCV not initialized"))
        }

        val settings = settingsRepository.getSettings().first()

        return try {
            // Verifica che ci siano descriptors
            if (descriptors1.empty() || descriptors2.empty()) {
                Log.d(TAG, "Empty descriptors")
                return Result.success(0.0)
            }

            // Verifica numero minimo di features (configurabile)
            if (descriptors1.rows() < settings.minFeatures || descriptors2.rows() < settings.minFeatures) {
                Log.d(TAG, "Insufficient features: ${descriptors1.rows()}, ${descriptors2.rows()} < ${settings.minFeatures}")
                return Result.success(0.0)
            }

            // Usa KNN matching con k=2 per ratio test
            val matches = mutableListOf<MatOfDMatch>()
            matcher.knnMatch(descriptors1, descriptors2, matches, 2)

            if (matches.isEmpty()) {
                Log.d(TAG, "No KNN matches found")
                return Result.success(0.0)
            }

            // Applica Lowe's ratio test con soglia configurabile
            val goodMatches = applyRatioTest(matches, settings.loweRatioThreshold, settings.absoluteDistanceThreshold)
            Log.d(TAG, "Good matches after ratio test: ${goodMatches.size}")

            if (goodMatches.isEmpty()) {
                return Result.success(0.0)
            }

            // Calcola similarity con pesi configurabili
            val similarity = calculateAdvancedSimilarity(
                goodMatches,
                descriptors1.rows(),
                descriptors2.rows(),
                settings
            )

            Log.d(TAG, "Advanced similarity: $similarity")

            // Cleanup
            matches.forEach { it.release() }

            Result.success(similarity)

        } catch (e: Exception) {
            Log.e(TAG, "Match exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Trova i migliori match con parametri configurabili
     */
    suspend fun findBestMatches(
        queryDescriptors: Mat,
        databaseDescriptors: List<Mat>,
        threshold: Double? = null
    ): Result<List<MatchResult>> {
        val settings = settingsRepository.getSettings().first()
        val actualThreshold = threshold ?: settings.defaultMatchingThreshold

        Log.d(TAG, "findBestMatches - Query: ${queryDescriptors.rows()} features, DB: ${databaseDescriptors.size} images, Threshold: $actualThreshold")

        if (!openCVManager.isInitialized()) {
            Log.e(TAG, "OpenCV not initialized!")
            return Result.failure(IllegalStateException("OpenCV not initialized"))
        }

        return try {
            val results = mutableListOf<MatchResult>()

            // Pre-filtra database descriptors per qualità (con minFeatures configurabile)
            val validDbDescriptors = databaseDescriptors.mapIndexedNotNull { index, descriptors ->
                if (descriptors.rows() >= settings.minFeatures) {
                    index to descriptors
                } else {
                    Log.d(TAG, "Skipping image #$index: insufficient features (${descriptors.rows()} < ${settings.minFeatures})")
                    null
                }
            }

            Log.d(TAG, "Valid images for matching: ${validDbDescriptors.size}/${databaseDescriptors.size}")

            // Confronta con ogni set di descriptors valido
            validDbDescriptors.forEach { (originalIndex, dbDescriptors) ->
                Log.d(TAG, "Image #$originalIndex (${dbDescriptors.rows()} features)...")

                val similarity = matchFeatures(queryDescriptors, dbDescriptors)
                    .getOrElse {
                        Log.e(TAG, "Match failed: ${it.message}")
                        0.0
                    }

                Log.d(TAG, "Similarity: $similarity (threshold: $actualThreshold)")

                if (similarity >= actualThreshold) {
                    Log.d(TAG, "MATCH! Adding to results")
                    results.add(MatchResult(originalIndex, similarity))
                } else {
                    Log.d(TAG, "Below threshold")
                }
            }

            // Ordina per similarità decrescente
            results.sortByDescending { it.similarity }

            // Applica post-processing per evitare duplicati troppo simili
            val filteredResults = filterSimilarMatches(results)

            Log.d(TAG, "Final matches: ${filteredResults.size} (filtered from ${results.size})")

            Result.success(filteredResults)

        } catch (e: Exception) {
            Log.e(TAG, "Exception in findBestMatches: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Applica Lowe's ratio test con parametri configurabili
     */
    private fun applyRatioTest(
        matches: List<MatOfDMatch>,
        loweRatioThreshold: Float,
        absoluteDistanceThreshold: Float
    ): List<org.opencv.core.DMatch> {
        val goodMatches = mutableListOf<org.opencv.core.DMatch>()

        for (match in matches) {
            val matchArray = match.toArray()
            if (matchArray.size >= 2) {
                val bestMatch = matchArray[0]
                val secondBestMatch = matchArray[1]

                // Lowe's ratio test con soglia configurabile
                val ratio = bestMatch.distance / secondBestMatch.distance
                if (ratio < loweRatioThreshold) {
                    goodMatches.add(bestMatch)
                }
            } else if (matchArray.size == 1) {
                // Se c'è solo un match, accettalo se la distanza è buona
                val match = matchArray[0]
                if (match.distance < absoluteDistanceThreshold) {
                    goodMatches.add(match)
                }
            }
        }

        return goodMatches
    }

    /**
     * Calcola similarity con pesi configurabili
     */
    private fun calculateAdvancedSimilarity(
        goodMatches: List<org.opencv.core.DMatch>,
        features1Count: Int,
        features2Count: Int,
        settings: RecognitionSettings
    ): Double {
        if (goodMatches.isEmpty()) {
            return 0.0
        }

        // Filtro aggiuntivo: richiedi almeno un numero minimo di good matches
        val minGoodMatches = maxOf(15, settings.minFeatures / 2)
        if (goodMatches.size < minGoodMatches) {
            Log.d(TAG, "Too few good matches: ${goodMatches.size} < $minGoodMatches")
            return 0.0
        }

        // 1. Match ratio (numero di match rispetto alle features totali)
        val minFeatures = min(features1Count, features2Count)
        val maxFeatures = max(features1Count, features2Count)
        val matchRatio = goodMatches.size.toDouble() / minFeatures

        // 2. Feature density
        val densityFactor = minFeatures.toDouble() / maxFeatures

        // 3. Distance quality NORMALIZZATO
        val distances = goodMatches.map { it.distance.toDouble() }
        val avgDistance = distances.average()

        val normalizedDistance = avgDistance / settings.absoluteDistanceThreshold
        val distanceQuality = 1.0 / (1.0 + normalizedDistance)

        // 4. Consistency score (varianza delle distanze)
        val distanceVariance = distances.map { (it - avgDistance) * (it - avgDistance) }.average()
        val normalizedVariance = distanceVariance / (avgDistance * avgDistance).coerceAtLeast(1.0)
        val consistencyScore = 1.0 / (1.0 + normalizedVariance)

        Log.d(TAG, "Advanced metrics:")
        Log.d(TAG, "   Match ratio: $matchRatio (${goodMatches.size}/$minFeatures)")
        Log.d(TAG, "   Density factor: $densityFactor")
        Log.d(TAG, "   Distance quality: $distanceQuality")
        Log.d(TAG, "   Consistency: $consistencyScore")

        // Combina tutti i fattori con pesi configurabili
        val similarity = (matchRatio * settings.matchRatioWeight) +
                (densityFactor * settings.densityWeight) +
                (distanceQuality * settings.distanceQualityWeight) +
                (consistencyScore * settings.consistencyWeight)

        val finalSimilarity = similarity.coerceIn(0.0, 1.0)

        Log.d(TAG, "Final similarity: $finalSimilarity (weights: ${settings.matchRatioWeight}, ${settings.densityWeight}, ${settings.distanceQualityWeight}, ${settings.consistencyWeight})")

        return finalSimilarity
    }

    /**
     * Filtra match troppo simili per evitare duplicati
     */
    private fun filterSimilarMatches(results: List<MatchResult>): List<MatchResult> {
        if (results.size <= 1) return results

        val filtered = mutableListOf<MatchResult>()
        filtered.add(results.first()) // Aggiungi sempre il migliore

        for (i in 1 until results.size) {
            val current = results[i]
            val shouldAdd = filtered.none { existing ->
                kotlin.math.abs(existing.similarity - current.similarity) < SIMILARITY_DIFF_THRESHOLD
            }

            if (shouldAdd) {
                filtered.add(current)
            }
        }

        return filtered
    }

    /**
     * Risultato di un match
     */
    data class MatchResult(
        val index: Int,
        val similarity: Double,
        val confidence: Double = similarity
    )

    companion object {
        private const val TAG = "ConfigurableImageMatcher"
        private const val SIMILARITY_DIFF_THRESHOLD = 0.05
    }
}
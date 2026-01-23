package net.calvuz.qstore.data.opencv

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.DescriptorMatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Matcher avanzato per confrontare features OpenCV
 * Utilizza algoritmi migliorati e ratio test per matching più accurato
 */
@Singleton
class ImageMatcher @Inject constructor(
    private val openCVManager: OpenCVManager
) {
    private val matcher: BFMatcher by lazy {
        BFMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING, false) // crossCheck=false per knn
    }

    /**
     * Confronta due set di features con algoritmo migliorato
     */
    fun matchFeatures(descriptors1: Mat, descriptors2: Mat): Result<Double> {
        if (!openCVManager.isInitialized()) {
            return Result.failure(IllegalStateException("OpenCV not initialized"))
        }

        return try {
            // Verifica che ci siano descriptors
            if (descriptors1.empty() || descriptors2.empty()) {
                Log.d(TAG, "❌ Empty descriptors")
                return Result.success(0.0)
            }

            // Verifica numero minimo di features
            if (descriptors1.rows() < MIN_FEATURES || descriptors2.rows() < MIN_FEATURES) {
                Log.d(TAG, "❌ Insufficient features: ${descriptors1.rows()}, ${descriptors2.rows()}")
                return Result.success(0.0)
            }

            // Usa KNN matching con k=2 per ratio test
            val matches = mutableListOf<MatOfDMatch>()
            matcher.knnMatch(descriptors1, descriptors2, matches, 2)

            if (matches.isEmpty()) {
                Log.d(TAG, "❌ No KNN matches found")
                return Result.success(0.0)
            }

            // Applica Lowe's ratio test per filtrare good matches
            val goodMatches = applyRatioTest(matches)
            Log.d(TAG, "  📊 Good matches after ratio test: ${goodMatches.size}")

            if (goodMatches.isEmpty()) {
                return Result.success(0.0)
            }

            // Calcola similarity con algoritmo migliorato
            val similarity = calculateAdvancedSimilarity(
                goodMatches,
                descriptors1.rows(),
                descriptors2.rows()
            )

            Log.d(TAG, "  📈 Advanced similarity: $similarity")

            // Cleanup
            matches.forEach { it.release() }

            Result.success(similarity)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Match exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Trova i migliori match con algoritmo ottimizzato
     */
    fun findBestMatches(
        queryDescriptors: Mat,
        databaseDescriptors: List<Mat>,
        threshold: Double
    ): Result<List<MatchResult>> {
        Log.d(TAG, "⭐⭐⭐ findBestMatches IMPROVED ⭐⭐⭐")
        Log.d(TAG, "Query: ${queryDescriptors.rows()} features, DB: ${databaseDescriptors.size} images, Threshold: $threshold")

        if (!openCVManager.isInitialized()) {
            Log.e(TAG, "❌ OpenCV not initialized!")
            return Result.failure(IllegalStateException("OpenCV not initialized"))
        }

        return try {
            val results = mutableListOf<MatchResult>()

            // Pre-filtra database descriptors per qualità
            val validDbDescriptors = databaseDescriptors.mapIndexedNotNull { index, descriptors ->
                if (descriptors.rows() >= MIN_FEATURES) {
                    index to descriptors
                } else {
                    Log.d(TAG, "  ⚠️ Skipping image #$index: insufficient features (${descriptors.rows()})")
                    null
                }
            }

            Log.d(TAG, "🔍 Valid images for matching: ${validDbDescriptors.size}/${databaseDescriptors.size}")

            // Confronta con ogni set di descriptors valido
            validDbDescriptors.forEach { (originalIndex, dbDescriptors) ->
                Log.d(TAG, "  🔄 Image #$originalIndex (${dbDescriptors.rows()} features)...")

                val similarity = matchFeatures(queryDescriptors, dbDescriptors)
                    .getOrElse {
                        Log.e(TAG, "    ❌ Match failed: ${it.message}")
                        0.0
                    }

                Log.d(TAG, "    Similarity: $similarity (threshold: $threshold)")

                if (similarity >= threshold) {
                    Log.d(TAG, "    ✅ MATCH! Adding to results")
                    results.add(MatchResult(originalIndex, similarity))
                } else {
                    Log.d(TAG, "    ❌ Below threshold")
                }
            }

            // Ordina per similarità decrescente
            results.sortByDescending { it.similarity }

            // Applica post-processing per evitare duplicati troppo simili
            val filteredResults = filterSimilarMatches(results)

            Log.d(TAG, "✅ Final matches: ${filteredResults.size} (filtered from ${results.size})")

            Result.success(filteredResults)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception in findBestMatches: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Applica Lowe's ratio test per filtrare good matches
     * Confronta la distanza del miglior match con il secondo miglior match
     */
    private fun applyRatioTest(matches: List<MatOfDMatch>): List<org.opencv.core.DMatch> {
        val goodMatches = mutableListOf<org.opencv.core.DMatch>()

        for (match in matches) {
            val matchArray = match.toArray()
            if (matchArray.size >= 2) {
                val bestMatch = matchArray[0]
                val secondBestMatch = matchArray[1]

                // Lowe's ratio test: distanza_migliore / distanza_seconda < soglia
                val ratio = bestMatch.distance / secondBestMatch.distance
                if (ratio < LOWE_RATIO_THRESHOLD) {
                    goodMatches.add(bestMatch)
                }
            } else if (matchArray.size == 1) {
                // Se c'è solo un match, accettalo se la distanza è buona
                val match = matchArray[0]
                if (match.distance < ABSOLUTE_DISTANCE_THRESHOLD) {
                    goodMatches.add(match)
                }
            }
        }

        return goodMatches
    }

    /**
     * Calcola similarity con algoritmo semplificato e più permissivo
     */
    private fun calculateAdvancedSimilarity(
        goodMatches: List<org.opencv.core.DMatch>,
        features1Count: Int,
        features2Count: Int
    ): Double {
        if (goodMatches.isEmpty()) {
            return 0.0
        }

        // Filtro aggiuntivo: richiedi almeno un numero minimo di good matches
        val minGoodMatches = 15  // Aumentato per ridurre falsi positivi
        if (goodMatches.size < minGoodMatches) {
            Log.d(TAG, "      ❌ Too few good matches: ${goodMatches.size} < $minGoodMatches")
            return 0.0
        }

        // 1. Match ratio (numero di match rispetto alle features totali)
        val minFeatures = min(features1Count, features2Count)
        val maxFeatures = max(features1Count, features2Count)
        val matchRatio = goodMatches.size.toDouble() / minFeatures

        // 2. Feature density (evita bias verso immagini con molte features)
        val densityFactor = minFeatures.toDouble() / maxFeatures

        // 3. Distance quality NORMALIZZATO per gestire distanze alte
        val distances = goodMatches.map { it.distance.toDouble() }
        val avgDistance = distances.average()

        // Usa una scala logaritmica per gestire meglio distanze alte
        val normalizedDistance = avgDistance / ABSOLUTE_DISTANCE_THRESHOLD
        val distanceQuality = 1.0 / (1.0 + normalizedDistance) // Sempre positivo

        // 4. Consistency score (varianza delle distanze - minore è meglio)
        val distanceVariance = distances.map { (it - avgDistance) * (it - avgDistance) }.average()
        val normalizedVariance = distanceVariance / (avgDistance * avgDistance).coerceAtLeast(1.0)
        val consistencyScore = 1.0 / (1.0 + normalizedVariance)

        Log.d(TAG, "      📊 Advanced metrics:")
        Log.d(TAG, "         Match ratio: $matchRatio (${goodMatches.size}/$minFeatures)")
        Log.d(TAG, "         Density factor: $densityFactor")
        Log.d(TAG, "         Distance quality: $distanceQuality (avg: $avgDistance, normalized: $normalizedDistance)")
        Log.d(TAG, "         Consistency: $consistencyScore (var: $distanceVariance, norm: $normalizedVariance)")

        // Combina tutti i fattori con pesi ottimizzati
        val similarity = (matchRatio * MATCH_RATIO_WEIGHT) +
                (densityFactor * DENSITY_WEIGHT) +
                (distanceQuality * DISTANCE_QUALITY_WEIGHT) +
                (consistencyScore * CONSISTENCY_WEIGHT)

        val finalSimilarity = similarity.coerceIn(0.0, 1.0)

        Log.d(TAG, "      🎯 Final similarity calculation:")
        Log.d(TAG, "         Raw similarity: $similarity")
        Log.d(TAG, "         Clamped similarity: $finalSimilarity")

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
                // Se due match sono molto simili in score, prendi solo il migliore
                kotlin.math.abs(existing.similarity - current.similarity) < SIMILARITY_DIFF_THRESHOLD
            }

            if (shouldAdd) {
                filtered.add(current)
            }
        }

        return filtered
    }

    /**
     * Risultato di un match con metadati aggiuntivi
     */
    data class MatchResult(
        val index: Int,         // Indice nel database
        val similarity: Double, // Similarità (0.0 - 1.0)
        val confidence: Double = similarity  // Confidence level
    )

    companion object {
        private const val TAG = "ImageMatcher"

        // Parametri più restrittivi per ridurre falsi positivi
        private const val LOWE_RATIO_THRESHOLD = 0.7f    // Più restrittivo (era 0.8)
        private const val ABSOLUTE_DISTANCE_THRESHOLD = 280f  // Più restrittivo (era 350)

        // Aumentiamo il minimo di features per match più affidabili
        private const val MIN_FEATURES = 30  // Era 20, torniamo a 30

        // Riequilibriamo i pesi per favorire qualità
        private const val MATCH_RATIO_WEIGHT = 0.5        // 50% - ridotto da 60%
        private const val DENSITY_WEIGHT = 0.15           // 15% - aumentato da 10%
        private const val DISTANCE_QUALITY_WEIGHT = 0.25  // 25% - aumentato da 20%
        private const val CONSISTENCY_WEIGHT = 0.1        // 10% - uguale

        // Soglia per filtrare match simili
        private const val SIMILARITY_DIFF_THRESHOLD = 0.05
    }
}
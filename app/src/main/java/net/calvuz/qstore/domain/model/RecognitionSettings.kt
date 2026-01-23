package net.calvuz.qstore.domain.model

/**
 * Configurazione parametri per il riconoscimento immagini
 */
data class RecognitionSettings(
    // Parametri ImageMatcher
    val loweRatioThreshold: Float = 0.7f,
    val absoluteDistanceThreshold: Float = 280f,
    val minFeatures: Int = 30,

    // Pesi per calcolo similarity
    val matchRatioWeight: Double = 0.5,
    val densityWeight: Double = 0.15,
    val distanceQualityWeight: Double = 0.25,
    val consistencyWeight: Double = 0.1,

    // Soglia di matching globale
    val defaultMatchingThreshold: Double = 0.7,

    // Parametri di validazione
    val minFeaturesForValidation: Int = 30,
    val idealFeaturesForValidation: Int = 200
) {

    companion object {
        fun getDefault() = RecognitionSettings()

        fun getPresetPrecise() = RecognitionSettings(
            loweRatioThreshold = 0.6f,
            absoluteDistanceThreshold = 250f,
            minFeatures = 50,
            matchRatioWeight = 0.4,
            distanceQualityWeight = 0.35,
            defaultMatchingThreshold = 0.8
        )

        fun getPresetBalanced() = RecognitionSettings() // Default

        fun getPresetFast() = RecognitionSettings(
            loweRatioThreshold = 0.8f,
            absoluteDistanceThreshold = 320f,
            minFeatures = 20,
            matchRatioWeight = 0.6,
            distanceQualityWeight = 0.2,
            defaultMatchingThreshold = 0.6
        )
    }

    /**
     * Valida che i parametri siano in range accettabili
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (loweRatioThreshold !in 0.3f..0.9f) {
            errors.add("Lowe Ratio deve essere tra 0.3 e 0.9")
        }

        if (absoluteDistanceThreshold !in 100f..500f) {
            errors.add("Distance Threshold deve essere tra 100 e 500")
        }

        if (minFeatures !in 10..100) {
            errors.add("Min Features deve essere tra 10 e 100")
        }

        val totalWeight = matchRatioWeight + densityWeight + distanceQualityWeight + consistencyWeight
        if (totalWeight !in 0.9..1.1) {
            errors.add("Somma pesi deve essere ~1.0 (attuale: $totalWeight)")
        }

        if (defaultMatchingThreshold !in 0.1..1.0) {
            errors.add("Matching Threshold deve essere tra 0.1 e 1.0")
        }

        return errors
    }
}
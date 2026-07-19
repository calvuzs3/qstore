package net.calvuz.qstore.app.domain.model

/**
 * Statistiche di giacenza per un singolo magazzino/ubicazione — usata dalla dashboard Home.
 */
data class LocationStats(
    val locationUuid: String,
    val locationName: String,
    val articleCount: Int,
    val totalQuantity: Double
)

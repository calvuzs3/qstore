package net.calvuz.qstore.sync.domain.model

/** Esito di una pulizia definitiva dei dati soft-eliminati (vedi PurgeDeletedDataUseCase). */
data class PurgeSummary(
    val articlesPurged: Int,
    val imagesPurged: Int,
    val categoriesPurged: Int
) {
    val total: Int get() = articlesPurged + imagesPurged + categoriesPurged
}

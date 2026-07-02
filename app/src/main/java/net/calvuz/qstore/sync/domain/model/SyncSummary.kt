package net.calvuz.qstore.sync.domain.model

/** Esito di una sincronizzazione completa (push + pull). */
data class SyncSummary(
    val pushedCount: Int,
    val rejectedCount: Int,
    val pulledCount: Int
)

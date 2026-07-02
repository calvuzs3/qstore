package net.calvuz.qstore.app.domain.model

/**
 * Domain Model per Location (magazzino/ubicazione, es. "Sede", "Furgone Mario")
 * Indipendente dal database (Clean Architecture).
 */
data class Location(
    val uuid: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

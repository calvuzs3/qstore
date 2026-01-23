package net.calvuz.qstore.domain.model

import net.calvuz.qstore.domain.model.enum.MovementType

/**
 * Domain Model per Movimento Magazzino
 *
 * Rappresenta una movimentazione (carico/scarico) nel sistema.
 * Indipendente dal database (Clean Architecture).
 */
data class Movement(
    val id: Long,                 // ID auto-incrementato (0 = non ancora salvato)
    val articleUuid: String,      // Riferimento all'articolo
    val type: MovementType,       // Tipo movimento (IN/OUT)
    val quantity: Double,         // Quantità movimentata
    val notes: String,            // Note aggiuntive
    val createdAt: Long           // UTC timestamp milliseconds
)


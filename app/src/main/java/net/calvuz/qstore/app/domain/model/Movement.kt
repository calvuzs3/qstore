package net.calvuz.qstore.app.domain.model

import net.calvuz.qstore.app.domain.model.enum.MovementType

/**
 * Domain Model per Movimento Magazzino
 *
 * Rappresenta una movimentazione (carico/scarico/rettifica/trasferimento) nel sistema.
 * Indipendente dal database (Clean Architecture).
 *
 * Regola per tipo (from/to location):
 *   IN:         solo toLocationUuid
 *   OUT:        solo fromLocationUuid
 *   ADJUSTMENT: uno solo dei due (aumento->to, diminuzione->from)
 *   TRANSFER:   entrambi, diversi tra loro
 */
data class Movement(
    val id: String,                    // UUID (era Long autoincrementato)
    val articleUuid: String,           // Riferimento all'articolo
    val type: MovementType,
    val fromLocationUuid: String?,
    val toLocationUuid: String?,
    val quantity: Double,              // Quantità movimentata
    val notes: String,                 // Note aggiuntive
    val createdAt: Long,                // UTC timestamp milliseconds
    val createdBy: String? = null       // userId di chi era loggato — null se offline/senza account
)


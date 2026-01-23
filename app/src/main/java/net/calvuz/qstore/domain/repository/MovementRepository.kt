package net.calvuz.qstore.domain.repository

import net.calvuz.qstore.domain.model.Movement
import net.calvuz.qstore.domain.model.enum.MovementType
import kotlinx.coroutines.flow.Flow

/**
 * Repository Interface per gestione movimentazioni
 *
 * Definisce i contratti per la registrazione e consultazione delle movimentazioni.
 * L'implementazione sarà nel data layer.
 */
interface MovementRepository {

    /**
     * Registra una nuova movimentazione e aggiorna l'inventario
     * Operazione transazionale: movement + update inventory
     *
     * @param movement Movimentazione da registrare
     * @return Result con Unit in caso di successo, errore altrimenti
     */
    suspend fun addMovement(movement: Movement): Result<Unit>
    suspend fun addMovement(
        articleUuid: String,
        type: MovementType,
        quantity: Double,
        notes: String
    ): Result<Unit>

    /**
     * Recupera tutti i movimenti
     */
    suspend fun getAllMovements(): Result<List<Movement>>

    /**
     * Ottiene una movimentazione per UUID
     */
    suspend fun getMovementByUuid(uuid: String): Result<Movement?>

    /**
     * Ottiene tutte le movimentazioni di un articolo
     */
    suspend fun getMovementsByArticle(articleUuid: String): Result<List<Movement>>

    /**
     * Recupera ultimi N movimenti ordinati per data (desc)
     */
    suspend fun getRecentMovements(limit: Int): Result<List<Movement>>

    /**
     * Osserva tutte le movimentazioni (per storico generale)
     */
    fun observeAllMovements(): Flow<List<Movement>>

    /**
     * Osserva tutte le movimentazioni di un articolo ordinate per timestamp DESC
     */
    fun observeMovementsByArticle(articleUuid: String): Flow<List<Movement>>

    /**
     * Ottiene movimentazioni filtrate per tipo
     */
    fun observeMovementsByType(type: MovementType): Flow<List<Movement>>

    /**
     * Ottiene movimentazioni in un intervallo di tempo
     * @param startTimestamp Timestamp UTC inizio (inclusive)
     * @param endTimestamp Timestamp UTC fine (inclusive)
     */
    fun observeMovementsByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<Movement>>

    /**
     * Elimina una movimentazione
     * ATTENZIONE: Non aggiorna l'inventario automaticamente!
     * Usare con cautela solo per correzioni.
     */
    suspend fun deleteMovement(uuid: String): Result<Unit>
}
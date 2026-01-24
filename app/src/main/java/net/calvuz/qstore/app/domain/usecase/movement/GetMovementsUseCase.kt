package net.calvuz.qstore.app.domain.usecase.movement

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.repository.MovementRepository
import javax.inject.Inject

/**
 * Use Case per recuperare movimentazioni dal magazzino
 */
class GetMovementsUseCase @Inject constructor(
    private val movementRepository: MovementRepository
) {
    /**
     * Recupera tutti i movimenti di un articolo
     */
    suspend operator fun invoke(articleUuid: String): Result<List<Movement>> {
        return try {
            if (articleUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID articolo non valido"))
            }

            movementRepository.getMovementsByArticle(articleUuid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera tutti i movimenti
     */
    suspend fun getAll(): Result<List<Movement>> {
        return try {
            movementRepository.getAllMovements()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera ultimi N movimenti (ordinati per data decrescente)
     */
    suspend fun getRecent(limit: Int = 10): Result<List<Movement>> {
        return try {
            if (limit <= 0) {
                return Result.failure(IllegalArgumentException("Limit deve essere > 0"))
            }

            movementRepository.getRecentMovements(limit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene una movimentazione per UUID
     */
    suspend fun getByUuid(uuid: String): Result<Movement?> {
        if (uuid.isBlank()) {
            return Result.failure(IllegalArgumentException("UUID cannot be blank"))
        }
        return movementRepository.getMovementByUuid(uuid)
    }

    /**
     * Osserva tutte le movimentazioni di un articolo
     * (ordinate per timestamp DESC - più recenti prima)
     */
    fun observeByArticle(articleUuid: String): Flow<List<Movement>> {
        return movementRepository.observeMovementsByArticle(articleUuid)
    }

    /**
     * Ottiene tutte le movimentazioni di un articolo
     */
    suspend fun getByArticle(articleUuid: String): Result<List<Movement>> {
        return movementRepository.getMovementsByArticle(articleUuid)
    }

    /**
     * Osserva i movimenti di un articolo
     */
    fun observe(articleUuid: String): Flow<List<Movement>> {
        require(articleUuid.isNotBlank()) { "UUID articolo non valido" }
        return movementRepository.observeMovementsByArticle(articleUuid)
    }

    /**
     * Osserva tutte le movimentazioni (storico generale)
     */
    fun observeAll(): Flow<List<Movement>> {
        return movementRepository.observeAllMovements()
    }

    /**
     * Osserva movimentazioni filtrate per tipo
     */
    fun observeByType(type: MovementType): Flow<List<Movement>> {
        return movementRepository.observeMovementsByType(type)
    }

    /**
     * Osserva movimentazioni in un intervallo di tempo
     *
     * @param startTimestamp Timestamp UTC inizio (milliseconds)
     * @param endTimestamp Timestamp UTC fine (milliseconds)
     */
    fun observeByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<Movement>> {
        return movementRepository.observeMovementsByDateRange(startTimestamp, endTimestamp)
    }

    /**
     * Osserva solo le entrate
     */
    fun observeIncoming(): Flow<List<Movement>> {
        return observeByType(MovementType.IN)
    }

    /**
     * Osserva solo le uscite
     */
    fun observeOutgoing(): Flow<List<Movement>> {
        return observeByType(MovementType.OUT)
    }
}
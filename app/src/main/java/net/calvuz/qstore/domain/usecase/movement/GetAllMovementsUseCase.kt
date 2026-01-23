package net.calvuz.qstore.domain.usecase.movement

import net.calvuz.qstore.domain.model.Movement
import net.calvuz.qstore.domain.repository.MovementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case per recuperare tutti i movimenti del magazzino
 */
class GetAllMovementsUseCase @Inject constructor(
    private val movementRepository: MovementRepository
) {
    /**
     * Recupera tutti i movimenti ordinati dal più recente
     */
    suspend fun getAll(): Result<List<Movement>> {
        return try {
            movementRepository.getAllMovements()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutti i movimenti con aggiornamenti real-time
     */
    fun observeAll(): Flow<List<Movement>> {
        return movementRepository.observeAllMovements()
    }
}
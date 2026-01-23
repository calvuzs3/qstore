package net.calvuz.qstore.domain.usecase.movement

import net.calvuz.qstore.domain.model.Movement
import net.calvuz.qstore.domain.repository.MovementRepository
import javax.inject.Inject

/**
 * Use Case per recuperare tutti i movimenti di un articolo specifico
 * Ordinati dal più recente al più vecchio
 */
class GetMovementsByArticleUseCase @Inject constructor(
    private val movementRepository: MovementRepository
) {
    suspend operator fun invoke(articleId: String): Result<List<Movement>> {
        return try {
            movementRepository.getMovementsByArticle(articleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package net.calvuz.qstore.app.domain.usecase.movement

import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.repository.MovementRepository
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
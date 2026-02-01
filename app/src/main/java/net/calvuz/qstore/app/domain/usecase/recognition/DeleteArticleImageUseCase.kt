package net.calvuz.qstore.app.domain.usecase.recognition

import net.calvuz.qstore.app.domain.repository.ImageRecognitionRepository
import javax.inject.Inject

/**
 * Use Case: Elimina immagine articolo
 *
 * Business Rules:
 * - Elimina file dal file system
 * - Elimina record dal database
 * - Operazione idempotente (non fallisce se già eliminata)
 *
 * @param imageRecognitionRepository Per eliminare immagine
 */
class DeleteArticleImageUseCase @Inject constructor(
    private val imageRecognitionRepository: ImageRecognitionRepository
) {
    /**
     * Elimina un'immagine per ID
     *
     * @param imageUuid ID dell'immagine da eliminare
     * @return Result<Unit> Success se eliminata o Failure
     */
    suspend operator fun invoke(imageUuid: String): Result<Unit> {
        return try {
            // Validazione input
            if (imageUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID immagine vuoto: $imageUuid"))
            }

            // Elimina immagine
            imageRecognitionRepository.deleteImage(imageUuid)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina tutte le immagini di un articolo
     *
     * @param articleUuid UUID dell'articolo
     * @return Result<Int> Success con numero immagini eliminate o Failure
     */
    suspend fun deleteAllByArticle(articleUuid: String): Result<Int> {
        return try {
            // Validazione input
            if (articleUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID articolo non valido"))
            }

            // Elimina tutte le immagini dell'articolo
            imageRecognitionRepository.deleteImages(articleUuid)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
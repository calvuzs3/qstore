package net.calvuz.qstore.app.domain.usecase.recognition

import net.calvuz.qstore.app.domain.model.ArticleImage
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.repository.ImageRecognitionRepository
import javax.inject.Inject

/**
 * Use Case: Salva immagine articolo con features OpenCV
 *
 * Business Rules:
 * - Articolo deve esistere
 * - Immagine deve essere valida (non vuota)
 * - Estrae automaticamente features ORB
 * - Salva file + features nel DB
 *
 * @param articleRepository Per verificare esistenza articolo
 * @param imageRecognitionRepository Per salvare immagine
 */
class SaveArticleImageUseCase @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val imageRecognitionRepository: ImageRecognitionRepository
) {
    /**
     * Salva un'immagine per un articolo
     *
     * @param articleUuid UUID dell'articolo
     * @param imageData Bytes dell'immagine (JPEG/PNG)
     * @return Result<ArticleImage> Success con immagine salvata o Failure
     */
    suspend operator fun invoke(
        articleUuid: String,
        imageData: ByteArray
    ): Result<ArticleImage> {
        return try {
            // Validazione input
            if (articleUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID articolo non valido"))
            }

            if (imageData.isEmpty()) {
                return Result.failure(IllegalArgumentException("Immagine vuota"))
            }

            // Verifica esistenza articolo
            val articleExists = articleRepository.getByUuid(articleUuid)
                .map { it != null }
                .getOrElse { false }

            if (!articleExists) {
                return Result.failure(IllegalStateException("Articolo non trovato: $articleUuid"))
            }

            // Salva immagine + estrazione features
            imageRecognitionRepository.saveArticleImage(
                articleUuid = articleUuid,
                imageData = imageData
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
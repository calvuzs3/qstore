package net.calvuz.qstore.domain.usecase.recognition

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.domain.model.ArticleImage
import net.calvuz.qstore.domain.repository.ImageRecognitionRepository
import javax.inject.Inject

/**
 * Use Case: Recupera immagini articolo
 *
 * Business Rules:
 * - Recupera tutte le immagini associate a un articolo
 * - Supporta osservazione reattiva (Flow)
 * - Ritorna lista vuota se nessuna immagine
 *
 * @param imageRecognitionRepository Per recuperare immagini
 */
class GetArticleImagesUseCase @Inject constructor(
    private val imageRecognitionRepository: ImageRecognitionRepository
) {
    /**
     * Recupera tutte le immagini di un articolo (one-shot)
     *
     * @param articleUuid UUID dell'articolo
     * @return Result<List<ArticleImage>> Lista immagini o Failure
     */
    suspend operator fun invoke(
        articleUuid: String
    ): Result<List<ArticleImage>>
    {
        return try {
            // Validazione input
            if (articleUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID articolo non valido"))
            }

            // Recupera immagini
            imageRecognitionRepository.getArticleImages(articleUuid)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva reattivamente le immagini di un articolo
     *
     * @param articleUuid UUID dell'articolo
     * @return Flow<List<ArticleImage>> Stream che emette ad ogni cambio
     */
    fun observe(articleUuid: String): Flow<List<ArticleImage>> {
        require(articleUuid.isNotBlank()) { "UUID articolo non valido" }
        return imageRecognitionRepository.observeArticleImages(articleUuid)
    }

    /**
     * Recupera una singola immagine per ID
     *
     * @param imageId ID dell'immagine
     * @return Result<ArticleImage?> Immagine o null se non esiste
     */
    suspend fun getById(imageId: Long): Result<ArticleImage?> {
        return try {
            // Validazione input
            if (imageId <= 0) {
                return Result.failure(IllegalArgumentException("ID immagine non valido: $imageId"))
            }

            // Recupera immagine singola
            imageRecognitionRepository.getArticleImageById(imageId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se un articolo ha immagini
     *
     * @param articleUuid UUID dell'articolo
     * @return Result<Boolean> true se ha almeno un'immagine
     */
    suspend fun hasImages(articleUuid: String): Result<Boolean> {
        return try {
            // Validazione input
            if (articleUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID articolo non valido"))
            }

            // Conta immagini
            val images = imageRecognitionRepository.getArticleImages(articleUuid)
                .getOrElse { emptyList() }

            Result.success(images.isNotEmpty())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
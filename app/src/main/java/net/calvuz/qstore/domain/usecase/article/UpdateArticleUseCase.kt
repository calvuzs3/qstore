package net.calvuz.qstore.domain.usecase.article

import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Use Case per aggiornare un articolo esistente
 *
 * Nota: Questo use case aggiorna SOLO i dati anagrafici dell'articolo.
 * La giacenza (inventory) viene modificata solo tramite i movimenti.
 */
class UpdateArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    suspend operator fun invoke(article: Article): Result<Unit> {
        return try {
            // Validazioni
            if (article.uuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID non valido"))
            }

            if (article.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Il nome è obbligatorio"))
            }

            if (article.unitOfMeasure.isBlank()) {
                return Result.failure(IllegalArgumentException("L'unità di misura è obbligatoria"))
            }

            if (article.reorderLevel < 0) {
                return Result.failure(IllegalArgumentException("La soglia non può essere negativa"))
            }

            // Verifica che l'articolo esista
            articleRepository.getByUuid(article.uuid)
                .onSuccess { existingArticle ->
                    if (existingArticle == null) {
                        return Result.failure(IllegalArgumentException("Articolo non trovato"))
                    }
                }
                .onFailure {
                    return Result.failure(it)
                }

            // Aggiorna articolo (con updatedAt corrente)
            val updatedArticle = article.copy(
                updatedAt = System.currentTimeMillis()
            )

            articleRepository.updateArticle(updatedArticle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
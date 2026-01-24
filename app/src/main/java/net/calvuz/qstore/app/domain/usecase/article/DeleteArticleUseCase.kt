package net.calvuz.qstore.app.domain.usecase.article

import net.calvuz.qstore.app.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Use Case per eliminare un articolo
 *
 * ATTENZIONE: Questa operazione è irreversibile e cancellerà anche:
 * - Inventario associato
 * - Tutte le movimentazioni (CASCADE)
 * - Tutte le immagini (CASCADE)
 */
class DeleteArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    /**
     * Elimina un articolo e tutti i dati correlati
     *
     * @param uuid UUID dell'articolo da eliminare
     * @return Result con Unit in caso di successo, errore altrimenti
     */
    suspend operator fun invoke(uuid: String): Result<Unit> {
        if (uuid.isBlank()) {
            return Result.failure(IllegalArgumentException("UUID cannot be blank"))
        }

        // Verifica che l'articolo esista
        val article = articleRepository.getByUuid(uuid)
            .getOrElse {
                return Result.failure(it)
            }

        if (article == null) {
            return Result.failure(IllegalArgumentException("Article not found"))
        }

        return articleRepository.deleteArticle(uuid)
    }

    /**
     * Elimina più articoli in batch
     *
     * @param uuids Lista di UUID da eliminare
     * @return Result con numero di articoli eliminati con successo
     */
    suspend fun deleteMultiple(uuids: List<String>): Result<Int> {
        if (uuids.isEmpty()) {
            return Result.success(0)
        }

        var successCount = 0
        val errors = mutableListOf<Throwable>()

        uuids.forEach { uuid ->
            articleRepository.deleteArticle(uuid)
                .onSuccess { successCount++ }
                .onFailure { errors.add(it) }
        }

        return if (errors.isEmpty()) {
            Result.success(successCount)
        } else {
            // Se ci sono errori, ritorna il primo
            Result.failure(errors.first())
        }
    }
}
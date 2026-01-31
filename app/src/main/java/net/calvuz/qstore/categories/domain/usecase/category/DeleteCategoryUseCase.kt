package net.calvuz.qstore.categories.domain.usecase.category

import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: ArticleCategoryRepository
) {
    /**
     * Elimina una categoria.
     * Fallisce se la categoria contiene articoli.
     */
    suspend operator fun invoke(uuid: String): Result<Unit> {
        return repository.delete(uuid)
    }

    /**
     * Verifica se una categoria può essere eliminata (non ha articoli associati)
     */
    suspend fun canDelete(uuid: String): Boolean {
        return repository.countArticlesInCategory(uuid) == 0
    }

    /**
     * Conta gli articoli in una categoria
     */
    suspend fun countArticles(uuid: String): Int {
        return repository.countArticlesInCategory(uuid)
    }
}

package net.calvuz.qstore.categories.domain.usecase.category

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: ArticleCategoryRepository
) {
    fun observeAll(): Flow<List<ArticleCategory>> {
        return repository.observeAll()
    }

    suspend fun getAll(): List<ArticleCategory> {
        return repository.getAll()
    }

    suspend fun getByUuid(uuid: String): ArticleCategory? {
        return repository.getByUuid(uuid)
    }
}

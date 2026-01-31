package net.calvuz.qstore.categories.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.categories.domain.model.ArticleCategory

interface ArticleCategoryRepository {
    
    fun observeAll(): Flow<List<ArticleCategory>>
    
    suspend fun getAll(): List<ArticleCategory>
    
    suspend fun getByUuid(uuid: String): ArticleCategory?
    
    suspend fun getByName(name: String): ArticleCategory?
    
    suspend fun insert(category: ArticleCategory): Result<Unit>
    
    suspend fun update(category: ArticleCategory): Result<Unit>
    
    suspend fun delete(uuid: String): Result<Unit>
    
    suspend fun countArticlesInCategory(categoryId: String): Int
}

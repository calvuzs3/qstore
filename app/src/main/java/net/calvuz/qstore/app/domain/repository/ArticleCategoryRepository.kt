package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.ArticleCategory

interface ArticleCategoryRepository {
    fun observeAll(): Flow<List<ArticleCategory>>
    suspend fun getAll(): List<ArticleCategory>
    suspend fun getByUuid(uuid: String): ArticleCategory?
    suspend fun getByName(name: String): ArticleCategory?
}
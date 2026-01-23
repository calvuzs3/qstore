package net.calvuz.qstore.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.domain.model.ArticleCategory

interface ArticleCategoryRepository {
    fun getAllFlow(): Flow<List<ArticleCategory>>
    suspend fun getAll(): List<ArticleCategory>
    suspend fun getById(uuid: String): ArticleCategory?
    suspend fun getByName(name: String): ArticleCategory?
}
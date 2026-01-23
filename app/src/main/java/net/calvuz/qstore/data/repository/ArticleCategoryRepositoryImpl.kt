package net.calvuz.qstore.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.data.local.database.ArticleCategoryDao
import net.calvuz.qstore.data.mapper.toDomain
import net.calvuz.qstore.data.mapper.toDomainList
import net.calvuz.qstore.domain.model.ArticleCategory
import net.calvuz.qstore.domain.repository.ArticleCategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleCategoryRepositoryImpl @Inject constructor(
    private val categoryDao: ArticleCategoryDao
) : ArticleCategoryRepository {

    override fun getAllFlow(): Flow<List<ArticleCategory>> {
        return categoryDao.getAllFlow().map { it.toDomainList() }
    }

    override suspend fun getAll(): List<ArticleCategory> {
        return categoryDao.getAll().toDomainList()
    }

    override suspend fun getById(uuid: String): ArticleCategory? {
        return categoryDao.getById(uuid)?.toDomain()
    }

    override suspend fun getByName(name: String): ArticleCategory? {
        return categoryDao.getByName(name)?.toDomain()
    }
}
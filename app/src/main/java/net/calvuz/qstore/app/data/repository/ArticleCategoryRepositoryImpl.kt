package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.ArticleCategoryDao
import net.calvuz.qstore.app.data.mapper.toDomain
import net.calvuz.qstore.app.data.mapper.toDomainList
import net.calvuz.qstore.app.domain.model.ArticleCategory
import net.calvuz.qstore.app.domain.repository.ArticleCategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleCategoryRepositoryImpl @Inject constructor(
    private val categoryDao: ArticleCategoryDao,
) : ArticleCategoryRepository {

    override fun observeAll(): Flow<List<ArticleCategory>> {
        return categoryDao.getAllFlow().map { it.toDomainList() }
    }

    override suspend fun getAll(): List<ArticleCategory> {
        return categoryDao.getAll().toDomainList()
    }

    override suspend fun getByUuid(uuid: String): ArticleCategory? {
        return categoryDao.getByUuid(uuid)?.toDomain()
    }

    override suspend fun getByName(name: String): ArticleCategory? {
        return categoryDao.getByName(name)?.toDomain()
    }
}
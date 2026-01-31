package net.calvuz.qstore.categories.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.categories.data.local.ArticleCategoryDao
import net.calvuz.qstore.categories.data.mapper.toDomain
import net.calvuz.qstore.categories.data.mapper.toDomainList
import net.calvuz.qstore.categories.data.mapper.toEntity
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
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

    override suspend fun insert(category: ArticleCategory): Result<Unit> {
        return try {
            // Check if name already exists
            val existing = categoryDao.getByName(category.name)
            if (existing != null) {
                Result.failure(IllegalArgumentException("Una categoria con questo nome esiste già"))
            } else {
                categoryDao.insert(category.toEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(category: ArticleCategory): Result<Unit> {
        return try {
            // Check if name already exists for another category
            val existing = categoryDao.getByName(category.name)
            if (existing != null && existing.uuid != category.uuid) {
                Result.failure(IllegalArgumentException("Una categoria con questo nome esiste già"))
            } else {
                categoryDao.update(category.toEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(uuid: String): Result<Unit> {
        return try {
            val category = categoryDao.getByUuid(uuid)
            if (category != null) {
                // Check if category has articles
                val articleCount = categoryDao.countArticlesInCategory(uuid)
                if (articleCount > 0) {
                    Result.failure(
                        IllegalStateException(
                            "Impossibile eliminare: la categoria contiene $articleCount articoli"
                        )
                    )
                } else {
                    categoryDao.delete(category)
                    Result.success(Unit)
                }
            } else {
                Result.failure(IllegalArgumentException("Categoria non trovata"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun countArticlesInCategory(categoryId: String): Int {
        return categoryDao.countArticlesInCategory(categoryId)
    }
}

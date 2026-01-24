package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.ArticleDao
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.data.local.entity.InventoryEntity
import net.calvuz.qstore.app.data.mapper.ArticleMapper
import net.calvuz.qstore.app.data.mapper.InventoryMapper
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Implementazione del repository per articoli e inventario
 */
class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val inventoryDao: InventoryDao,
    private val articleMapper: ArticleMapper,
    private val inventoryMapper: InventoryMapper
) : ArticleRepository {

    override suspend fun getByUuid(uuid: String): Result<Article?> {
        return try {
            val entity = articleDao.getByUuid(uuid)
            val article = entity?.let { articleMapper.toDomain(it) }
            Result.success(article)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAll(): Result<List<Article>> {
        return try {
            val entities = articleDao.getAll()
            val articles = entities.map { articleMapper.toDomain(it) }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertArticle(article: Article, initialQuantity: Double): Result<Unit> {
        return try {
            // Inserisci articolo
            articleDao.insert(articleMapper.toEntity(article))

            // Crea inventario iniziale
            val inventory = InventoryEntity(
                articleUuid = article.uuid,
                currentQuantity = initialQuantity,
                lastMovementAt = article.createdAt
            )
            inventoryDao.insert(inventory)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateArticle(article: Article): Result<Unit> {
        return try {
            val entity = articleMapper.toEntity(article)
            articleDao.update(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteArticle(uuid: String): Result<Unit> {
        return try {
            val article = articleDao.getByUuid(uuid)
                ?: return Result.success(Unit) // Già eliminato

            articleDao.delete(article)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getByCategory(category: String): Result<List<Article>> {
        return try {
            val entities = articleDao.getByCategory(category)
            val articles = entities.map { articleMapper.toDomain(it) }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAll(): Flow<List<Article>> {
        return articleDao.observeAll()
            .map { entities -> entities.map { articleMapper.toDomain(it) } }
    }

    override fun observeByUuid(uuid: String): Flow<Article?> {
        return articleDao.observeByUuid(uuid).map { entity ->
            entity?.let { articleMapper.toDomain(it) }
        }
    }

    override suspend fun searchByName(query: String): Result<List<Article>> {
        return try {
            val entities = articleDao.searchByName("%$query%")
            val articles = entities.map { articleMapper.toDomain(it) }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInventory(articleUuid: String): Result<Inventory?> {
        return try {
            val entity = inventoryDao.getByArticleUuid(articleUuid)
            val inventory = entity?.let { inventoryMapper.toDomain(it) }
            Result.success(inventory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeInventory(articleUuid: String): Flow<Inventory?> {
        return inventoryDao.observeByArticleUuid(articleUuid).map { entity ->
            entity?.let { inventoryMapper.toDomain(it) }
        }
    }

    override suspend fun getArticlesCount(): Result<Int> {
        return try {
            val count = articleDao.getCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
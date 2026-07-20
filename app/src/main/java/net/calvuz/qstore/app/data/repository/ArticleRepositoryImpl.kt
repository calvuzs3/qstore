package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.ArticleDao
import net.calvuz.qstore.app.data.local.database.ArticleImageDao
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.data.local.storage.ImageStorageManager
import net.calvuz.qstore.app.data.mapper.ArticleMapper
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Implementazione del repository per articoli e inventario
 */
class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val articleImageDao: ArticleImageDao,
    private val imageStorageManager: ImageStorageManager,
    private val inventoryDao: InventoryDao,
    private val articleMapper: ArticleMapper
) : ArticleRepository {

    override suspend fun getByUuid(uuid: String): Result<Article?> {
        return try {
            // getByUuid del DAO non filtra is_deleted (serve anche al sync per il LWW su
            // pull) — va escluso qui: un articolo cancellato non deve comparire lato app.
            val entity = articleDao.getByUuid(uuid)?.takeIf { !it.isDeleted }
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

    override suspend fun insertArticle(article: Article): Result<Unit> {
        return try {
            articleDao.insert(articleMapper.toEntity(article))
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
            articleDao.getByUuid(uuid)
                ?: return Result.success(Unit) // Già eliminato

            val now = System.currentTimeMillis()

            // Soft-delete, non più un DELETE fisico: propaga la cancellazione al server al
            // prossimo push (vedi SyncRepositoryImpl). Inventario e movimenti NON vengono
            // toccati — i movimenti restano un log append-only anche per un articolo
            // cancellato, l'inventario resta come cache stantia innocua (l'articolo comunque
            // sparisce da ogni lista/ricerca). Il CASCADE del FK di Room su article_images
            // non scatta più (un UPDATE non innesca CASCADE), quindi le immagini vanno
            // marcate cancellate esplicitamente qui, incluso il file fisico sul device.
            articleImageDao.getByArticleUuid(uuid).forEach { image ->
                imageStorageManager.deleteImage(image.imagePath)
            }
            articleImageDao.markAllDeletedByArticleUuid(uuid, now)

            articleDao.markDeleted(uuid, now)
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

    // Giacenza TOTALE dell'articolo, sommata su tutte le ubicazioni — non esiste ancora
    // una UI per mostrare/gestire la giacenza per singola ubicazione.
    override suspend fun getInventory(articleUuid: String): Result<Inventory?> {
        return try {
            val total = inventoryDao.getTotalByArticle(articleUuid)
            val quantity = total.totalQuantity
            val inventory = quantity?.let {
                Inventory(articleUuid, it, total.lastMovementAt ?: 0L)
            }
            Result.success(inventory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeInventory(articleUuid: String): Flow<Inventory?> {
        return inventoryDao.observeTotalByArticle(articleUuid).map { total ->
            total.totalQuantity?.let { Inventory(articleUuid, it, total.lastMovementAt ?: 0L) }
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

    override suspend fun getRecentlyCreated(limit: Int): Result<List<Article>> {
        return try {
            val entities = articleDao.getRecentlyCreated(limit)
            Result.success(entities.map { articleMapper.toDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

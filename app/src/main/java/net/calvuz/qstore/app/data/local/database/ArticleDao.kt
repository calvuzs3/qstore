package net.calvuz.qstore.app.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.ArticleEntity

/**
 * DAO per operazioni sulla tabella articles
 */
@Dao
interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(article: ArticleEntity)

    @Update
    suspend fun update(article: ArticleEntity)

    @Delete
    suspend fun delete(article: ArticleEntity)

    // Non filtrata su is_deleted: usata anche dal sync (LWW su pull) e da delete/update, che
    // devono trovare la riga indipendentemente dal suo stato di cancellazione.
    @Query("SELECT * FROM articles WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE uuid = :uuid AND is_deleted = 0")
    fun observeByUuid(uuid: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE is_deleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAll(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE is_deleted = 0 AND name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchByName(searchQuery: String): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE is_deleted = 0 AND category_id = :category ORDER BY name ASC")
    suspend fun getByCategory(category: String): List<ArticleEntity>

    /** Ultimi articoli creati (dashboard Home) — ordinati per data di creazione decrescente. */
    @Query("SELECT * FROM articles WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentlyCreated(limit: Int): List<ArticleEntity>

    @Query("SELECT COUNT(*) FROM articles WHERE is_deleted = 0")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE is_deleted = 0")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE is_deleted = 0 AND category_id = :categoryId")
    suspend fun countByCategory(categoryId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE uuid = :uuid AND is_deleted = 0)")
    suspend fun exists(uuid: String): Boolean

    /**
     * Righe modificate dopo il cursore di sync — usata per costruire il payload di push.
     * Non filtrata su is_deleted: una cancellazione (is_deleted=1, updated_at=now) DEVE
     * essere raccolta qui per essere propagata al server.
     */
    @Query("SELECT * FROM articles WHERE updated_at > :since")
    suspend fun getUpdatedSince(since: Long): List<ArticleEntity>

    /** Soft-delete: usata da ArticleRepositoryImpl.deleteArticle() al posto di un DELETE fisico. */
    @Query("UPDATE articles SET is_deleted = 1, updated_at = :updatedAt WHERE uuid = :uuid")
    suspend fun markDeleted(uuid: String, updatedAt: Long)

}
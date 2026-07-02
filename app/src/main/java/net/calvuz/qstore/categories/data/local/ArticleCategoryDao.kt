package net.calvuz.qstore.categories.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.ArticleCategoryEntity

@Dao
interface ArticleCategoryDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(category: ArticleCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(categories: List<ArticleCategoryEntity>)

    @Update
    suspend fun update(category: ArticleCategoryEntity)

    @Delete
    suspend fun delete(category: ArticleCategoryEntity)

    @Query("SELECT * FROM article_categories WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ArticleCategoryEntity?

    @Query("SELECT * FROM article_categories WHERE uuid = :uuid AND is_deleted = 0")
    fun getByIdFlow(uuid: String): Flow<ArticleCategoryEntity?>

    // Filtrato is_deleted=0: una categoria cancellata non deve bloccare il riuso del nome.
    @Query("SELECT * FROM article_categories WHERE name = :name AND is_deleted = 0")
    suspend fun getByName(name: String): ArticleCategoryEntity?

    @Query("SELECT * FROM article_categories WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ArticleCategoryEntity>>

    @Query("SELECT * FROM article_categories WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAll(): List<ArticleCategoryEntity>

    @Query("SELECT COUNT(*) FROM article_categories WHERE is_deleted = 0")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun countArticlesInCategory(categoryId: String): Int

    @Query("SELECT DISTINCT name FROM article_categories WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAllCategories(): List<String>

    /**
     * Righe modificate dopo il cursore di sync — usata per costruire il payload di push.
     * Non filtrata su is_deleted: una cancellazione (is_deleted=1, updated_at=now) DEVE
     * essere raccolta qui per essere propagata al server.
     */
    @Query("SELECT * FROM article_categories WHERE updated_at > :since")
    suspend fun getUpdatedSince(since: Long): List<ArticleCategoryEntity>

    /** Soft-delete: usata da ArticleCategoryRepositoryImpl.delete() al posto di un DELETE fisico. */
    @Query("UPDATE article_categories SET is_deleted = 1, updated_at = :updatedAt WHERE uuid = :uuid")
    suspend fun markDeleted(uuid: String, updatedAt: Long)
}
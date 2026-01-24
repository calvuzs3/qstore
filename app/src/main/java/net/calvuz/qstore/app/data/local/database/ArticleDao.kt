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

    @Query("SELECT * FROM articles WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE uuid = :uuid")
    fun observeByUuid(uuid: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles ORDER BY name ASC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY name ASC")
    suspend fun getAll(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchByName(searchQuery: String): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE category_id = :category ORDER BY name ASC")
    suspend fun getByCategory(category: String): List<ArticleEntity>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE uuid = :uuid)")
    suspend fun exists(uuid: String): Boolean

}
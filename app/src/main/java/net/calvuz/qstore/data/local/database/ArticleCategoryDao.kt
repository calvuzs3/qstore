package net.calvuz.qstore.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.data.local.entity.ArticleCategoryEntity

@Dao
interface ArticleCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: ArticleCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<ArticleCategoryEntity>)

    @Update
    suspend fun update(category: ArticleCategoryEntity)

    @Delete
    suspend fun delete(category: ArticleCategoryEntity)

    @Query("SELECT * FROM article_categories WHERE uuid = :uuid")
    suspend fun getById(uuid: String): ArticleCategoryEntity?

    @Query("SELECT * FROM article_categories WHERE uuid = :uuid")
    fun getByIdFlow(uuid: String): Flow<ArticleCategoryEntity?>

    @Query("SELECT * FROM article_categories WHERE name = :name")
    suspend fun getByName(name: String): ArticleCategoryEntity?

    @Query("SELECT * FROM article_categories ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ArticleCategoryEntity>>

    @Query("SELECT * FROM article_categories ORDER BY name ASC")
    suspend fun getAll(): List<ArticleCategoryEntity>

    @Query("SELECT COUNT(*) FROM article_categories")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE category_id = :categoryId")
    suspend fun countArticlesInCategory(categoryId: String): Int

    @Query("SELECT DISTINCT name FROM article_categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<String>
}
package net.calvuz.qstore.app.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.ArticleLocationThresholdEntity

/**
 * DAO per operazioni sulla tabella article_location_thresholds
 */
@Dao
interface ArticleLocationThresholdDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threshold: ArticleLocationThresholdEntity)

    @Update
    suspend fun update(threshold: ArticleLocationThresholdEntity)

    @Delete
    suspend fun delete(threshold: ArticleLocationThresholdEntity)

    @Query("SELECT * FROM article_location_thresholds WHERE article_uuid = :articleUuid AND location_uuid = :locationUuid")
    suspend fun getByArticleAndLocation(articleUuid: String, locationUuid: String): ArticleLocationThresholdEntity?

    @Query("SELECT * FROM article_location_thresholds WHERE article_uuid = :articleUuid")
    fun observeByArticle(articleUuid: String): Flow<List<ArticleLocationThresholdEntity>>
}

package net.calvuz.qstore.app.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.MovementEntity
import net.calvuz.qstore.app.domain.model.enum.MovementType

/**
 * DAO per operazioni sulla tabella movements
 */
@Dao
interface MovementDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(movement: MovementEntity)

    @Delete
    suspend fun delete(movement: MovementEntity)

    @Query("DELETE FROM movements WHERE article_uuid = :articleUuid")
    suspend fun deleteByArticleUuid(articleUuid: String): Int

    @Query("SELECT * FROM movements ORDER BY created_at DESC")
    suspend fun getAllMovements(): List<MovementEntity>

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun getById(id: Long): MovementEntity?

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun getById(id: String): MovementEntity?

    @Query("SELECT * FROM movements WHERE article_uuid = :articleUuid ORDER BY created_at DESC")
    suspend fun getByArticleUuid(articleUuid: String): List<MovementEntity>

    @Query("SELECT * FROM movements WHERE type = :type ORDER BY created_at DESC")
    suspend fun getByType(type: String): List<MovementEntity>

    @Query("""
        SELECT * FROM movements 
        WHERE created_at BETWEEN :startTimestamp AND :endTimestamp 
        ORDER BY created_at DESC
    """)
    suspend fun getByDateRange(startTimestamp: Long, endTimestamp: Long): List<MovementEntity>

    @Query("SELECT * FROM movements ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentMovements(limit: Int): List<MovementEntity>

    @Query("SELECT * FROM movements ORDER BY created_at DESC")
    fun observeAll(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE article_uuid = :articleUuid ORDER BY created_at DESC")
    fun observeByArticleUuid(articleUuid: String): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE type = :type ORDER BY created_at DESC")
    fun observeByType(type: MovementType): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE created_at BETWEEN :start AND :end ORDER BY created_at DESC")
    fun observeByDateRange(start: Long, end: Long): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<MovementEntity>>

    @Query("""
        SELECT * FROM movements 
        WHERE created_at >= :startTimestamp AND created_at <= :endTimestamp 
        ORDER BY created_at DESC
    """)
    fun observeByPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<MovementEntity>>

    @Query("""
        SELECT * FROM movements 
        WHERE article_uuid = :articleUuid AND type = :type 
        ORDER BY created_at DESC
    """)
    fun observeByArticleAndType(articleUuid: String, type: MovementType): Flow<List<MovementEntity>>

    @Query("SELECT COUNT(*) FROM movements WHERE created_at >= :todayStart")
    suspend fun getTodayCount(todayStart: Long): Int

    @Query("""
        SELECT SUM(CASE WHEN type = 'IN' THEN quantity ELSE -quantity END) 
        FROM movements 
        WHERE article_uuid = :articleUuid
    """)
    suspend fun calculateTotalQuantity(articleUuid: String): Double?

    @Query("SELECT COUNT(*) FROM movements WHERE article_uuid = :articleUuid")
    suspend fun countByArticle(articleUuid: String): Int

}
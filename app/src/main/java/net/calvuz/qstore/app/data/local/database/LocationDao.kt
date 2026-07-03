package net.calvuz.qstore.app.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.LocationEntity

/**
 * DAO per operazioni sulla tabella locations
 */
@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(location: LocationEntity)

    @Update
    suspend fun update(location: LocationEntity)

    @Delete
    suspend fun delete(location: LocationEntity)

    @Query("SELECT * FROM locations WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): LocationEntity?

    // Filtrato is_deleted=0: un magazzino cancellato non deve bloccare il riuso del nome.
    @Query("SELECT * FROM locations WHERE name = :name AND is_deleted = 0")
    suspend fun getByName(name: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE is_deleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM locations WHERE is_deleted = 0")
    suspend fun count(): Int

    /** Righe modificate dopo il cursore di sync — usata per costruire il payload di push. */
    @Query("SELECT * FROM locations WHERE updated_at > :since")
    suspend fun getUpdatedSince(since: Long): List<LocationEntity>

    /** Soft-delete: usata da LocationRepositoryImpl.delete() al posto di un DELETE fisico. */
    @Query("UPDATE locations SET is_deleted = 1, updated_at = :updatedAt WHERE uuid = :uuid")
    suspend fun markDeleted(uuid: String, updatedAt: Long)
}

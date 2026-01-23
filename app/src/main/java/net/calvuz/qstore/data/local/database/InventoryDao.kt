package net.calvuz.qstore.data.local.database

import androidx.room.*
import net.calvuz.qstore.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO per operazioni sulla tabella inventory
 */
@Dao
interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inventory: InventoryEntity): Long

    @Update
    suspend fun update(inventory: InventoryEntity)

    @Delete
    suspend fun delete(inventory: InventoryEntity)

    @Query("SELECT * FROM inventory")
    suspend fun getAll(): List<InventoryEntity>

    @Query("SELECT * FROM inventory")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE article_uuid = :id")
    suspend fun getById(id: Long): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE article_uuid = :articleUuid")
    suspend fun getByArticleUuid(articleUuid: String): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE article_uuid = :articleUuid")
    fun observeByArticleUuid(articleUuid: String): Flow<InventoryEntity?>

    @Query("SELECT current_quantity FROM inventory WHERE article_uuid = :articleUuid")
    suspend fun getQuantity(articleUuid: String): Double?

    @Query(
        """
        UPDATE inventory 
        SET current_quantity = :quantity, last_movement_at = :lastMovementAt 
        WHERE article_uuid = :articleUuid
    """
    )
    suspend fun updateQuantity(articleUuid: String, quantity: Double, lastMovementAt: Long)

    /**
     * Incrementa/decrementa quantità inventario
     */
    @Query("""
        UPDATE inventory 
        SET current_quantity = current_quantity + :delta, 
            last_movement_at = :timestamp 
        WHERE article_uuid = :articleUuid
    """)
    suspend fun adjustQuantity(
        articleUuid: String,
        delta: Double,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        SELECT * FROM inventory 
        WHERE current_quantity <= :threshold 
        ORDER BY current_quantity ASC
    """
    )
    fun observeLowStock(threshold: Double): Flow<List<InventoryEntity>>
}
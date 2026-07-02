package net.calvuz.qstore.app.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.InventoryEntity

/**
 * Proiezione aggregata su tutte le ubicazioni di un articolo — usata per il confronto con
 * articles.reorderLevel (soglia globale, non per singola ubicazione).
 */
data class InventoryTotalProjection(
    val totalQuantity: Double?,
    val lastMovementAt: Long?
)

/**
 * DAO per operazioni sulla tabella inventory (chiave composta article_uuid + location_uuid)
 */
@Dao
interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inventory: InventoryEntity)

    @Update
    suspend fun update(inventory: InventoryEntity)

    @Delete
    suspend fun delete(inventory: InventoryEntity)

    @Query("SELECT * FROM inventory")
    suspend fun getAll(): List<InventoryEntity>

    @Query("SELECT * FROM inventory")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE article_uuid = :articleUuid AND location_uuid = :locationUuid")
    suspend fun getByArticleAndLocation(articleUuid: String, locationUuid: String): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE article_uuid = :articleUuid AND location_uuid = :locationUuid")
    fun observeByArticleAndLocation(articleUuid: String, locationUuid: String): Flow<InventoryEntity?>

    @Query("SELECT * FROM inventory WHERE article_uuid = :articleUuid")
    suspend fun getByArticleUuid(articleUuid: String): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE article_uuid = :articleUuid")
    fun observeByArticleUuid(articleUuid: String): Flow<List<InventoryEntity>>

    @Query("SELECT current_quantity FROM inventory WHERE article_uuid = :articleUuid AND location_uuid = :locationUuid")
    suspend fun getQuantity(articleUuid: String, locationUuid: String): Double?

    @Query("""
        SELECT SUM(current_quantity) as totalQuantity, MAX(last_movement_at) as lastMovementAt
        FROM inventory WHERE article_uuid = :articleUuid
    """)
    suspend fun getTotalByArticle(articleUuid: String): InventoryTotalProjection

    @Query("""
        SELECT SUM(current_quantity) as totalQuantity, MAX(last_movement_at) as lastMovementAt
        FROM inventory WHERE article_uuid = :articleUuid
    """)
    fun observeTotalByArticle(articleUuid: String): Flow<InventoryTotalProjection>

    @Query(
        """
        UPDATE inventory
        SET current_quantity = :quantity, last_movement_at = :lastMovementAt
        WHERE article_uuid = :articleUuid AND location_uuid = :locationUuid
    """
    )
    suspend fun updateQuantity(articleUuid: String, locationUuid: String, quantity: Double, lastMovementAt: Long)

    /**
     * Incrementa/decrementa quantità inventario per una coppia (articolo, ubicazione).
     */
    @Query("""
        UPDATE inventory
        SET current_quantity = current_quantity + :delta,
            last_movement_at = :timestamp
        WHERE article_uuid = :articleUuid AND location_uuid = :locationUuid
    """)
    suspend fun adjustQuantity(
        articleUuid: String,
        locationUuid: String,
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

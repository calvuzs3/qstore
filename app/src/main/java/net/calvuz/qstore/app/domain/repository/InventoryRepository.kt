package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Inventory

/**
 * Repository interface per la gestione delle giacenze.
 * Specchia i metodi disponibili in InventoryDao.
 */
interface InventoryRepository {

    // ===== OBSERVE (Flow) =====

    fun observeAll(): Flow<List<Inventory>>

    fun observeByArticleUuid(articleUuid: String): Flow<Inventory?>

    fun observeLowStock(threshold: Double): Flow<List<Inventory>>

    // ===== READ (Suspend) =====

    suspend fun getByArticleUuid(articleUuid: String): Inventory?

    suspend fun getAll(): List<Inventory>

    suspend fun getQuantity(articleUuid: String): Double?

    suspend fun exists(articleUuid: String): Boolean

    // ===== WRITE (Suspend) =====

    suspend fun insert(inventory: Inventory): Long

    suspend fun update(inventory: Inventory)

    suspend fun delete(inventory: Inventory)

    /**
     * Imposta direttamente la quantità.
     */
    suspend fun updateQuantity(articleUuid: String, quantity: Double, lastMovementAt: Long)

    /**
     * Incrementa/decrementa la quantità di un delta.
     */
    suspend fun adjustQuantity(
        articleUuid: String,
        delta: Double,
        timestamp: Long = System.currentTimeMillis()
    )
}

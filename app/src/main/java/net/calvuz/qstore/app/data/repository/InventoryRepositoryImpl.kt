package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.data.mapper.InventoryMapper
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val mapper: InventoryMapper
) : InventoryRepository {

    override fun observeAll(): Flow<List<Inventory>> {
        return inventoryDao.observeAll().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeByArticleUuid(articleUuid: String): Flow<Inventory?> {
        return inventoryDao.observeByArticleUuid(articleUuid).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }

    override suspend fun getByArticleUuid(articleUuid: String): Inventory? {
        return inventoryDao.getByArticleUuid(articleUuid)?.let { mapper.toDomain(it) }
    }

    override suspend fun getAll(): List<Inventory> {
        return inventoryDao.getAll().map { mapper.toDomain(it) }
    }

    override suspend fun insert(inventory: Inventory): Long {
        return inventoryDao.insert(mapper.toEntity(inventory))
    }

    override suspend fun update(inventory: Inventory) {
        inventoryDao.update(mapper.toEntity(inventory))
    }

    override suspend fun delete(inventory: Inventory) {
        inventoryDao.delete(mapper.toEntity(inventory))
    }

    override suspend fun getQuantity(articleUuid: String): Double? {
        return inventoryDao.getQuantity(articleUuid)
    }

    override suspend fun updateQuantity(articleUuid: String, quantity: Double, lastMovementAt: Long) {
        inventoryDao.updateQuantity(articleUuid, quantity, lastMovementAt)
    }

    override suspend fun adjustQuantity(articleUuid: String, delta: Double, timestamp: Long) {
        inventoryDao.adjustQuantity(articleUuid, delta, timestamp)
    }

    override fun observeLowStock(threshold: Double): Flow<List<Inventory>> {
        return inventoryDao.observeLowStock(threshold).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override suspend fun exists(articleUuid: String): Boolean {
        return inventoryDao.getByArticleUuid(articleUuid) != null
    }
}

package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.model.InventoryEntry
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao
) : InventoryRepository {

    override suspend fun getByArticleUuid(articleUuid: String): Inventory? {
        val total = inventoryDao.getTotalByArticle(articleUuid)
        val quantity = total.totalQuantity ?: return null
        return Inventory(
            articleUuid = articleUuid,
            currentQuantity = quantity,
            lastMovementAt = total.lastMovementAt ?: 0L
        )
    }

    override fun observeQuantitiesByLocation(locationUuid: String): Flow<Map<String, Double>> {
        return inventoryDao.observeByLocation(locationUuid).map { entities ->
            entities.associate { it.articleUuid to it.currentQuantity }
        }
    }

    override suspend fun getQuantityAt(articleUuid: String, locationUuid: String): Double {
        return inventoryDao.getQuantity(articleUuid, locationUuid) ?: 0.0
    }

    override suspend fun getAllEntries(): List<InventoryEntry> {
        return inventoryDao.getAll().map {
            InventoryEntry(
                articleUuid = it.articleUuid,
                locationUuid = it.locationUuid,
                currentQuantity = it.currentQuantity
            )
        }
    }
}

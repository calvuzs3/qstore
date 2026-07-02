package net.calvuz.qstore.app.data.repository

import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.domain.model.Inventory
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
}

package net.calvuz.qstore.app.data.mapper

import net.calvuz.qstore.app.data.local.entity.InventoryEntity
import net.calvuz.qstore.app.domain.model.Inventory
import javax.inject.Inject

/**
 * Mapper per convertire tra InventoryEntity (data layer) e Inventory (domain layer)
 */
class InventoryMapper @Inject constructor() {

    /**
     * Converte da Entity a Domain Model
     */
    fun toDomain(entity: InventoryEntity): Inventory {
        return Inventory(
            articleUuid = entity.articleUuid,
            currentQuantity = entity.currentQuantity,
            lastMovementAt = entity.lastMovementAt
        )
    }

    /**
     * Converte una lista di Entity in lista di Domain Models
     */
    fun toDomainList(entities: List<InventoryEntity>): List<Inventory> {
        return entities.map { toDomain(it) }
    }

    // Nessun toEntity/toEntityList: Inventory (domain) non ha un'ubicazione, non è più
    // rappresentabile 1:1 come InventoryEntity (chiave composta article_uuid+location_uuid).
    // La scrittura dell'inventario passa sempre da MovementRepositoryImpl, mai da un
    // mapping diretto Inventory -> InventoryEntity.
}
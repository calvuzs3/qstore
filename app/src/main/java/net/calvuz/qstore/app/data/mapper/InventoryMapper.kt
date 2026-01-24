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
     * Converte da Domain Model a Entity
     */
    fun toEntity(domain: Inventory): InventoryEntity {
        return InventoryEntity(
            articleUuid = domain.articleUuid,
            currentQuantity = domain.currentQuantity,
            lastMovementAt = domain.lastMovementAt
        )
    }

    /**
     * Converte una lista di Entity in lista di Domain Models
     */
    fun toDomainList(entities: List<InventoryEntity>): List<Inventory> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converte lista di Domain Model a lista di Entity
     */
    fun toEntityList(inventories: List<Inventory>): List<InventoryEntity> {
        return inventories.map { toEntity(it) }
    }
}
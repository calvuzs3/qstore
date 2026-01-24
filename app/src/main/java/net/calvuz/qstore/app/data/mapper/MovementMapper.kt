package net.calvuz.qstore.app.data.mapper

import net.calvuz.qstore.app.data.local.entity.MovementEntity
import net.calvuz.qstore.app.domain.model.Movement
import javax.inject.Inject

/**
 * Mapper per convertire tra MovementEntity (data layer) e Movement (domain layer)
 */
class MovementMapper @Inject constructor() {

    /**
     * Converte da Entity a Domain Model
     */
    fun toDomain(entity: MovementEntity): Movement {
        return Movement(
            id = entity.id,
            articleUuid = entity.articleUuid,
            type = entity.type,
            quantity = entity.quantity,
            notes = entity.notes,
            createdAt = entity.createdAt
        )
    }

    /**
     * Converte da Domain Model a Entity
     */
    fun toEntity(domain: Movement): MovementEntity {
        return MovementEntity(
            id = domain.id,
            articleUuid = domain.articleUuid,
            type = domain.type,
            quantity = domain.quantity,
            notes = domain.notes,
            createdAt = domain.createdAt
        )
    }

    /**
     * Converte una lista di Entity in lista di Domain Models
     */
    fun toDomainList(entities: List<MovementEntity>): List<Movement> {
        return entities.map { toDomain(it) }
    }

    /**
     * Lista Domain → Lista Entity
     */
    fun toEntityList(domains: List<Movement>): List<MovementEntity> {
        return domains.map { toEntity(it) }
    }
}
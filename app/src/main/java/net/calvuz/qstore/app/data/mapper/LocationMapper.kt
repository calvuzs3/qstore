package net.calvuz.qstore.app.data.mapper

import net.calvuz.qstore.app.data.local.entity.LocationEntity
import net.calvuz.qstore.app.domain.model.Location
import javax.inject.Inject

/**
 * Mapper per convertire tra LocationEntity (data layer) e Location (domain layer)
 */
class LocationMapper @Inject constructor() {

    fun toDomain(entity: LocationEntity): Location {
        return Location(
            uuid = entity.uuid,
            name = entity.name,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Location): LocationEntity {
        return LocationEntity(
            uuid = domain.uuid,
            name = domain.name,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomainList(entities: List<LocationEntity>): List<Location> {
        return entities.map { toDomain(it) }
    }
}

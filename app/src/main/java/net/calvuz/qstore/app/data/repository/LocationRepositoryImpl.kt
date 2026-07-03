package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.InventoryDao
import net.calvuz.qstore.app.data.local.database.LocationDao
import net.calvuz.qstore.app.data.mapper.LocationMapper
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.repository.LocationRepository
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val inventoryDao: InventoryDao,
    private val locationMapper: LocationMapper
) : LocationRepository {

    override suspend fun getAll(): Result<List<Location>> {
        return try {
            Result.success(locationMapper.toDomainList(locationDao.getAll()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAll(): Flow<List<Location>> {
        return locationDao.observeAll().map { locationMapper.toDomainList(it) }
    }

    override suspend fun getByUuid(uuid: String): Result<Location?> {
        return try {
            Result.success(locationDao.getByUuid(uuid)?.takeIf { !it.isDeleted }?.let { locationMapper.toDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getByName(name: String): Result<Location?> {
        return try {
            Result.success(locationDao.getByName(name)?.let { locationMapper.toDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insert(location: Location): Result<Unit> {
        return try {
            val existing = locationDao.getByName(location.name)
            if (existing != null) {
                Result.failure(IllegalArgumentException("Un magazzino con questo nome esiste già"))
            } else {
                locationDao.insert(locationMapper.toEntity(location))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(location: Location): Result<Unit> {
        return try {
            val existing = locationDao.getByName(location.name)
            if (existing != null && existing.uuid != location.uuid) {
                Result.failure(IllegalArgumentException("Un magazzino con questo nome esiste già"))
            } else {
                locationDao.update(locationMapper.toEntity(location))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(uuid: String): Result<Unit> {
        return try {
            val location = locationDao.getByUuid(uuid)
                ?: return Result.failure(IllegalArgumentException("Magazzino non trovato"))

            if (locationDao.count() <= 1) {
                return Result.failure(
                    IllegalStateException("Impossibile eliminare: deve rimanere almeno un magazzino")
                )
            }

            if (inventoryDao.hasStock(location.uuid)) {
                return Result.failure(
                    IllegalStateException("Impossibile eliminare: il magazzino ha ancora giacenza")
                )
            }

            locationDao.markDeleted(uuid, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun canDelete(uuid: String): Result<Boolean> {
        return try {
            Result.success(locationDao.count() > 1 && !inventoryDao.hasStock(uuid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasStock(uuid: String): Result<Boolean> {
        return try {
            Result.success(inventoryDao.hasStock(uuid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

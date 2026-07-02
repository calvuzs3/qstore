package net.calvuz.qstore.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.data.local.database.LocationDao
import net.calvuz.qstore.app.data.mapper.LocationMapper
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.repository.LocationRepository
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
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
            Result.success(locationDao.getByUuid(uuid)?.let { locationMapper.toDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

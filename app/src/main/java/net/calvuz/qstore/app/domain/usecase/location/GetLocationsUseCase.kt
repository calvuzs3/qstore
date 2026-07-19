package net.calvuz.qstore.app.domain.usecase.location

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.model.LocationStats
import net.calvuz.qstore.app.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * Use Case per recuperare i magazzini/ubicazioni
 */
class GetLocationsUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    fun observeAll(): Flow<List<Location>> {
        return locationRepository.observeAll()
    }

    suspend fun getAll(): Result<List<Location>> {
        return locationRepository.getAll()
    }

    suspend fun getByUuid(uuid: String): Result<Location?> {
        return locationRepository.getByUuid(uuid)
    }

    /**
     * Statistiche di giacenza (numero articoli, quantità totale) per ogni magazzino.
     */
    suspend fun getStats(): Result<List<LocationStats>> {
        return locationRepository.getLocationStats()
    }
}

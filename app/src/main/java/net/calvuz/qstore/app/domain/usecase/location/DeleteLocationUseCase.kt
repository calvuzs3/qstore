package net.calvuz.qstore.app.domain.usecase.location

import net.calvuz.qstore.app.domain.repository.LocationRepository
import javax.inject.Inject

class DeleteLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    /**
     * Elimina un magazzino.
     * Fallisce se è l'unico rimasto o se ha ancora giacenza.
     */
    suspend operator fun invoke(uuid: String): Result<Unit> {
        return locationRepository.delete(uuid)
    }

    /**
     * Verifica se un magazzino può essere eliminato (non è l'unico rimasto e non ha giacenza)
     */
    suspend fun canDelete(uuid: String): Result<Boolean> {
        return locationRepository.canDelete(uuid)
    }

    /**
     * Verifica se un magazzino ha ancora giacenza (motivo di blocco specifico)
     */
    suspend fun hasStock(uuid: String): Result<Boolean> {
        return locationRepository.hasStock(uuid)
    }
}

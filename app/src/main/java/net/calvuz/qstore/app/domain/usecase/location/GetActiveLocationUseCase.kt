package net.calvuz.qstore.app.domain.usecase.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.repository.ActiveLocationRepository
import net.calvuz.qstore.app.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * Use Case per osservare il magazzino attualmente selezionato.
 * Ritorna `null` se non ne è selezionato uno ("Tutti i magazzini") o se l'ubicazione
 * salvata non esiste più (es. cancellata nel frattempo) — fallback silenzioso, niente crash.
 */
class GetActiveLocationUseCase @Inject constructor(
    private val activeLocationRepository: ActiveLocationRepository,
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): Flow<Location?> {
        return combine(
            activeLocationRepository.observeActiveLocationUuid(),
            locationRepository.observeAll()
        ) { uuid, locations ->
            if (uuid == null) null else locations.find { it.uuid == uuid }
        }
    }
}

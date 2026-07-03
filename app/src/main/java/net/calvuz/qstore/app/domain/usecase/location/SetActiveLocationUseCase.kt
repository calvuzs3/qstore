package net.calvuz.qstore.app.domain.usecase.location

import net.calvuz.qstore.app.domain.repository.ActiveLocationRepository
import javax.inject.Inject

/**
 * Use Case per impostare il magazzino attualmente selezionato.
 * `uuid = null` significa "Tutti i magazzini" (vista aggregata).
 */
class SetActiveLocationUseCase @Inject constructor(
    private val activeLocationRepository: ActiveLocationRepository
) {
    suspend operator fun invoke(uuid: String?) {
        activeLocationRepository.setActiveLocationUuid(uuid)
    }
}

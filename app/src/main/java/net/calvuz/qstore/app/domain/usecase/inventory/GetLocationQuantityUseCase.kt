package net.calvuz.qstore.app.domain.usecase.inventory

import net.calvuz.qstore.app.domain.repository.InventoryRepository
import javax.inject.Inject

/**
 * Use Case per ottenere la giacenza di un singolo articolo in una specifica ubicazione —
 * usato dal form movimenti per validare/mostrare la disponibilità nel magazzino selezionato.
 */
class GetLocationQuantityUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(articleUuid: String, locationUuid: String): Double {
        return inventoryRepository.getQuantityAt(articleUuid, locationUuid)
    }
}

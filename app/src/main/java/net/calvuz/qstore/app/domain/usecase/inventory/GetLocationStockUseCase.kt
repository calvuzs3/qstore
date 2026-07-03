package net.calvuz.qstore.app.domain.usecase.inventory

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import javax.inject.Inject

/**
 * Use Case per ottenere la giacenza di ogni articolo in una specifica ubicazione,
 * in un'unica query — usato per filtrare/decorare la lista articoli per magazzino.
 */
class GetLocationStockUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke(locationUuid: String): Flow<Map<String, Double>> {
        return inventoryRepository.observeQuantitiesByLocation(locationUuid)
    }
}

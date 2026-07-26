package net.calvuz.qstore.app.domain.usecase.inventory

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import javax.inject.Inject

/**
 * Osserva la giacenza di un singolo articolo in una specifica ubicazione, in tempo reale —
 * usata dal dettaglio articolo per mostrare la quantità nel magazzino attivo invece del solo
 * totale su tutte le ubicazioni (vedi ArticleDetailViewModel).
 */
class ObserveLocationQuantityUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke(articleUuid: String, locationUuid: String): Flow<Double> {
        return inventoryRepository.observeQuantityAt(articleUuid, locationUuid)
    }
}

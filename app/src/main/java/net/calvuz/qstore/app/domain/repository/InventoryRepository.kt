package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Inventory

/**
 * Repository interface per la lettura delle giacenze aggregate.
 *
 * La scrittura non passa da qui: ogni movimento aggiorna l'inventario per ubicazione
 * transazionalmente dentro MovementRepositoryImpl.
 */
interface InventoryRepository {

    /** Giacenza totale dell'articolo, sommata su tutte le ubicazioni. Null se non ha mai avuto movimenti. */
    suspend fun getByArticleUuid(articleUuid: String): Inventory?

    /** Quantità di ogni articolo (per uuid) in una specifica ubicazione. */
    fun observeQuantitiesByLocation(locationUuid: String): Flow<Map<String, Double>>

    /** Quantità di un articolo in una specifica ubicazione. 0.0 se non ha mai avuto movimenti lì. */
    suspend fun getQuantityAt(articleUuid: String, locationUuid: String): Double
}

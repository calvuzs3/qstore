package net.calvuz.qstore.app.domain.repository

import net.calvuz.qstore.app.domain.model.Inventory

/**
 * Repository interface per la lettura delle giacenze aggregate.
 *
 * La scrittura non passa da qui: ogni movimento aggiorna l'inventario per ubicazione
 * transazionalmente dentro MovementRepositoryImpl. Questo repository espone solo la
 * giacenza TOTALE di un articolo, sommata su tutte le ubicazioni.
 */
interface InventoryRepository {

    /** Giacenza totale dell'articolo, sommata su tutte le ubicazioni. Null se non ha mai avuto movimenti. */
    suspend fun getByArticleUuid(articleUuid: String): Inventory?
}

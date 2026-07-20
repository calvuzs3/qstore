package net.calvuz.qstore.app.domain.model

/**
 * Riga di giacenza per una singola coppia (articolo, ubicazione) — a differenza di [Inventory]
 * (già aggregato su tutte le ubicazioni), espone il dettaglio per ubicazione. Usata dove serve
 * confrontare la giacenza grezza con lo storico movimenti, es. ReconcileInventoryMovementsUseCase.
 */
data class InventoryEntry(
    val articleUuid: String,
    val locationUuid: String,
    val currentQuantity: Double
)

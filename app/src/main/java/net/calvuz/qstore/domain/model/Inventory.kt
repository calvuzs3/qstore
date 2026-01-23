package net.calvuz.qstore.domain.model

/**
 * Domain Model per Inventory
 * Rappresenta la giacenza corrente di un articolo.
 *
 * Relazione 1:1 con Article.
 * Separato da Article per evitare di aggiornare il timestamp dell'articolo
 * ad ogni movimentazione di magazzino.
 */
data class Inventory(
    val articleUuid: String,
    val currentQuantity: Double,    // Double per supportare decimali (es: 2.5 kg)
    val lastMovementAt: Long        // Unix timestamp UTC dell'ultima movimentazione
)
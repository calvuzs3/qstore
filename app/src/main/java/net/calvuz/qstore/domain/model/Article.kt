package net.calvuz.qstore.domain.model

/**
 * Domain Model per Article
 * Rappresenta i dati anagrafici di un articolo di magazzino.
 *
 * Note: La quantità corrente NON è qui, ma in Inventory
 * per separare i dati anagrafici dalle giacenze.
 */
data class Article(
    val uuid: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val unitOfMeasure: String,    // pz, kg, lt, mt, etc.
    val reorderLevel: Double,     // Soglia sotto scorta (0 = disabilitato)
    val codeOEM: String = "",     // NUOVO
    val codeERP: String = "",     // NUOVO
    val codeBM: String = "",      // NUOVO
    val notes: String,            // Note aggiuntive
    val createdAt: Long,          // UTC timestamp milliseconds
    val updatedAt: Long           // UTC timestamp milliseconds
)
package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository per il magazzino/ubicazione attualmente selezionato dall'utente.
 * `null` = "Tutti i magazzini" (vista aggregata, comportamento di default).
 */
interface ActiveLocationRepository {

    fun observeActiveLocationUuid(): Flow<String?>

    suspend fun setActiveLocationUuid(uuid: String?)
}

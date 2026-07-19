package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.model.LocationStats

/**
 * Repository interface per la gestione dei magazzini/ubicazioni.
 */
interface LocationRepository {

    suspend fun getAll(): Result<List<Location>>

    fun observeAll(): Flow<List<Location>>

    suspend fun getByUuid(uuid: String): Result<Location?>

    suspend fun getByName(name: String): Result<Location?>

    suspend fun insert(location: Location): Result<Unit>

    suspend fun update(location: Location): Result<Unit>

    /**
     * Cancella (soft-delete) un'ubicazione. Fallisce se è l'unica rimasta o se ha ancora
     * giacenza reale (vedi [canDelete]).
     */
    suspend fun delete(uuid: String): Result<Unit>

    /**
     * Verifica se un'ubicazione può essere cancellata: non deve essere l'unica rimasta e non
     * deve avere giacenza (current_quantity > 0) in nessun articolo.
     */
    suspend fun canDelete(uuid: String): Result<Boolean>

    suspend fun hasStock(uuid: String): Result<Boolean>

    /**
     * Statistiche di giacenza (numero articoli, quantità totale) per ogni magazzino —
     * include anche le ubicazioni senza alcuna giacenza (0/0). Usata dalla dashboard Home.
     */
    suspend fun getLocationStats(): Result<List<LocationStats>>
}

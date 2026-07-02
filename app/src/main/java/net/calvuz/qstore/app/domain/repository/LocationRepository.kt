package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Location

/**
 * Repository interface per la gestione dei magazzini/ubicazioni.
 */
interface LocationRepository {

    suspend fun getAll(): Result<List<Location>>

    fun observeAll(): Flow<List<Location>>

    suspend fun getByUuid(uuid: String): Result<Location?>
}

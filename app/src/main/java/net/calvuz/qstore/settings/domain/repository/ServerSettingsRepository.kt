package net.calvuz.qstore.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.settings.domain.model.ServerSettings

/**
 * Repository per la configurazione del server di sincronizzazione.
 */
interface ServerSettingsRepository {

    /** Osserva la configurazione corrente. */
    fun getSettings(): Flow<ServerSettings>

    /** Aggiorna l'URL base del server. */
    suspend fun setBaseUrl(baseUrl: String)
}

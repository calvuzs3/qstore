package net.calvuz.qstore.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.settings.domain.model.ServerSettings
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "server_settings"
)

@Singleton
class ServerSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ServerSettingsRepository {

    private val dataStore = context.serverSettingsDataStore

    private object PreferenceKeys {
        val BASE_URL = stringPreferencesKey("base_url")
    }

    override fun getSettings(): Flow<ServerSettings> {
        return dataStore.data.map { preferences ->
            ServerSettings(baseUrl = preferences[PreferenceKeys.BASE_URL] ?: "")
        }
    }

    override suspend fun setBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            // Senza slash finale, per costruire gli URL delle richieste senza doppie "//"
            preferences[PreferenceKeys.BASE_URL] = baseUrl.trim().trimEnd('/')
        }
    }
}

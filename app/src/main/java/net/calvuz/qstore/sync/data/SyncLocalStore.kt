package net.calvuz.qstore.sync.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.sync.domain.repository.SyncSettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_state")

/**
 * Cursore `since` per la pull incrementale + un deviceId stabile generato una volta sola
 * per installazione (usato in SyncPushRequest.deviceId, e in futuro per il canale
 * WebSocket, per poter escludere il device mittente dal nudge). Implementa anche
 * SyncSettingsRepository per esporre la preferenza wifi-only alla presentation layer
 * senza farle toccare direttamente questa classe di data layer.
 */
@Singleton
class SyncLocalStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncSettingsRepository {
    private val dataStore = context.syncDataStore

    private object Keys {
        val SINCE = longPreferencesKey("since")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val ALLOW_METERED_IMAGES = booleanPreferencesKey("allow_metered_images")
    }

    suspend fun getSince(): Long = dataStore.data.map { it[Keys.SINCE] ?: 0L }.first()

    suspend fun setSince(since: Long) {
        dataStore.edit { it[Keys.SINCE] = since }
    }

    suspend fun getDeviceId(): String {
        val existing = dataStore.data.map { it[Keys.DEVICE_ID] }.first()
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        dataStore.edit { it[Keys.DEVICE_ID] = newId }
        return newId
    }

    override fun observeAllowMeteredNetworkForImages(): Flow<Boolean> =
        dataStore.data.map { it[Keys.ALLOW_METERED_IMAGES] ?: false }

    override suspend fun setAllowMeteredNetworkForImages(allow: Boolean) {
        dataStore.edit { it[Keys.ALLOW_METERED_IMAGES] = allow }
    }
}

package net.calvuz.qstore.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.app.domain.repository.ActiveLocationRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeLocationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "active_location"
)

/**
 * Implementazione di [ActiveLocationRepository] usando DataStore Preferences.
 */
@Singleton
class ActiveLocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ActiveLocationRepository {

    private val dataStore = context.activeLocationDataStore

    private object PreferenceKeys {
        val LOCATION_UUID = stringPreferencesKey("location_uuid")
    }

    override fun observeActiveLocationUuid(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.LOCATION_UUID]
        }
    }

    override suspend fun setActiveLocationUuid(uuid: String?) {
        dataStore.edit { preferences ->
            if (uuid == null) {
                preferences.remove(PreferenceKeys.LOCATION_UUID)
            } else {
                preferences[PreferenceKeys.LOCATION_UUID] = uuid
            }
        }
    }
}

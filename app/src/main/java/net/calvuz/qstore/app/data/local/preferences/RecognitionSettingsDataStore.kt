package net.calvuz.qstore.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.settings.domain.model.RecognitionSettings
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recognitionSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recognition_settings"
)

@Singleton
class RecognitionSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val LOWE_RATIO_THRESHOLD = floatPreferencesKey("lowe_ratio_threshold")
        val ABSOLUTE_DISTANCE_THRESHOLD = floatPreferencesKey("absolute_distance_threshold")
        val MIN_FEATURES = intPreferencesKey("min_features")
        val MATCH_RATIO_WEIGHT = doublePreferencesKey("match_ratio_weight")
        val DENSITY_WEIGHT = doublePreferencesKey("density_weight")
        val DISTANCE_QUALITY_WEIGHT = doublePreferencesKey("distance_quality_weight")
        val CONSISTENCY_WEIGHT = doublePreferencesKey("consistency_weight")
        val DEFAULT_MATCHING_THRESHOLD = doublePreferencesKey("default_matching_threshold")
        val MIN_FEATURES_VALIDATION = intPreferencesKey("min_features_validation")
        val IDEAL_FEATURES_VALIDATION = intPreferencesKey("ideal_features_validation")
        val PRESET_NAME = stringPreferencesKey("preset_name")
    }

    /**
     * Flow delle impostazioni correnti con valori di default
     */
    val recognitionSettings: Flow<RecognitionSettings> = context.recognitionSettingsDataStore.data
        .catch { exception ->
            // In caso di errore, emetti preferences vuote per usare i default
            emit(emptyPreferences())
        }
        .map { preferences ->
            mapPreferencesToSettings(preferences)
        }

    /**
     * Flow del preset corrente
     */
    val currentPreset: Flow<String?> = context.recognitionSettingsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[PreferencesKeys.PRESET_NAME] ?: "Bilanciato"
        }

    /**
     * Aggiorna le impostazioni
     */
    suspend fun updateSettings(settings: RecognitionSettings) {
        context.recognitionSettingsDataStore.edit { preferences ->
            mapSettingsToPreferences(settings, preferences)
        }
    }

    /**
     * Aggiorna preset e relative impostazioni
     */
    suspend fun updatePreset(presetName: String, settings: RecognitionSettings) {
        context.recognitionSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRESET_NAME] = presetName
            mapSettingsToPreferences(settings, preferences)
        }
    }

    /**
     * Reset alle impostazioni di default
     */
    suspend fun resetToDefault() {
        val defaultSettings = RecognitionSettings.getDefault()
        context.recognitionSettingsDataStore.edit { preferences ->
            preferences.clear() // Rimuovi tutto
            preferences[PreferencesKeys.PRESET_NAME] = "Bilanciato"
            mapSettingsToPreferences(defaultSettings, preferences)
        }
    }

    /**
     * Mappa Preferences a RecognitionSettings
     */
    private fun mapPreferencesToSettings(preferences: Preferences): RecognitionSettings {
        return RecognitionSettings(
            loweRatioThreshold = preferences[PreferencesKeys.LOWE_RATIO_THRESHOLD] ?: 0.7f,
            absoluteDistanceThreshold = preferences[PreferencesKeys.ABSOLUTE_DISTANCE_THRESHOLD] ?: 280f,
            minFeatures = preferences[PreferencesKeys.MIN_FEATURES] ?: 30,
            matchRatioWeight = preferences[PreferencesKeys.MATCH_RATIO_WEIGHT] ?: 0.5,
            densityWeight = preferences[PreferencesKeys.DENSITY_WEIGHT] ?: 0.15,
            distanceQualityWeight = preferences[PreferencesKeys.DISTANCE_QUALITY_WEIGHT] ?: 0.25,
            consistencyWeight = preferences[PreferencesKeys.CONSISTENCY_WEIGHT] ?: 0.1,
            defaultMatchingThreshold = preferences[PreferencesKeys.DEFAULT_MATCHING_THRESHOLD] ?: 0.7,
            minFeaturesForValidation = preferences[PreferencesKeys.MIN_FEATURES_VALIDATION] ?: 30,
            idealFeaturesForValidation = preferences[PreferencesKeys.IDEAL_FEATURES_VALIDATION] ?: 200
        )
    }

    /**
     * Mappa RecognitionSettings a Preferences
     */
    private fun mapSettingsToPreferences(
        settings: RecognitionSettings,
        preferences: MutablePreferences
    ) {
        preferences[PreferencesKeys.LOWE_RATIO_THRESHOLD] = settings.loweRatioThreshold
        preferences[PreferencesKeys.ABSOLUTE_DISTANCE_THRESHOLD] = settings.absoluteDistanceThreshold
        preferences[PreferencesKeys.MIN_FEATURES] = settings.minFeatures
        preferences[PreferencesKeys.MATCH_RATIO_WEIGHT] = settings.matchRatioWeight
        preferences[PreferencesKeys.DENSITY_WEIGHT] = settings.densityWeight
        preferences[PreferencesKeys.DISTANCE_QUALITY_WEIGHT] = settings.distanceQualityWeight
        preferences[PreferencesKeys.CONSISTENCY_WEIGHT] = settings.consistencyWeight
        preferences[PreferencesKeys.DEFAULT_MATCHING_THRESHOLD] = settings.defaultMatchingThreshold
        preferences[PreferencesKeys.MIN_FEATURES_VALIDATION] = settings.minFeaturesForValidation
        preferences[PreferencesKeys.IDEAL_FEATURES_VALIDATION] = settings.idealFeaturesForValidation
    }
}
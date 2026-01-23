package net.calvuz.qstore.data.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.data.local.preferences.RecognitionSettingsDataStore
import net.calvuz.qstore.domain.model.RecognitionSettings
import net.calvuz.qstore.domain.repository.RecognitionSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione concreta del repository per le impostazioni di riconoscimento
 */
@Singleton
class RecognitionSettingsRepositoryImpl @Inject constructor(
    private val dataStore: RecognitionSettingsDataStore
) : RecognitionSettingsRepository {

    override fun getSettings(): Flow<RecognitionSettings> {
        return dataStore.recognitionSettings
    }

    override suspend fun updateSettings(settings: RecognitionSettings) {
        // Validazione prima del salvataggio
        val validationErrors = settings.validate()
        if (validationErrors.isNotEmpty()) {
            throw IllegalArgumentException(
                "Impostazioni non valide: ${validationErrors.joinToString(", ")}"
            )
        }

        dataStore.updateSettings(settings)
    }

    override suspend fun applyPreset(presetName: String) {
        val settings = when (presetName) {
            "Preciso" -> RecognitionSettings.getPresetPrecise()
            "Bilanciato" -> RecognitionSettings.getPresetBalanced()
            "Veloce" -> RecognitionSettings.getPresetFast()
            else -> throw IllegalArgumentException("Preset non riconosciuto: $presetName")
        }

        dataStore.updatePreset(presetName, settings)
    }

    override fun getCurrentPreset(): Flow<String?> {
        return dataStore.currentPreset
    }

    override suspend fun resetToDefault() {
        dataStore.resetToDefault()
    }
}

package net.calvuz.qstore.backup.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.calvuz.qstore.settings.domain.model.RecognitionSettings
import net.calvuz.qstore.settings.domain.repository.RecognitionSettingsRepository

/**
 * Implementazione in-memory di [RecognitionSettingsRepository] per i test — evita di dipendere
 * da DataStore reale tra un test e l'altro.
 */
class FakeRecognitionSettingsRepository(
    initial: RecognitionSettings = RecognitionSettings.getDefault()
) : RecognitionSettingsRepository {

    private val settingsState = MutableStateFlow(initial)
    private val presetState = MutableStateFlow<String?>(null)

    override fun getSettings(): Flow<RecognitionSettings> = settingsState

    override suspend fun updateSettings(settings: RecognitionSettings) {
        settingsState.value = settings
    }

    override suspend fun applyPreset(presetName: String) {
        val settings = when (presetName) {
            "Preciso" -> RecognitionSettings.getPresetPrecise()
            "Bilanciato" -> RecognitionSettings.getPresetBalanced()
            "Veloce" -> RecognitionSettings.getPresetFast()
            else -> throw IllegalArgumentException("Preset non riconosciuto: $presetName")
        }
        settingsState.value = settings
        presetState.value = presetName
    }

    override fun getCurrentPreset(): Flow<String?> = presetState

    override suspend fun resetToDefault() {
        settingsState.value = RecognitionSettings.getDefault()
        presetState.value = null
    }

    fun current(): RecognitionSettings = settingsState.value
    fun currentPresetName(): String? = presetState.value
}

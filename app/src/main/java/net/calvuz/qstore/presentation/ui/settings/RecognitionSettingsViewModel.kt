package net.calvuz.qstore.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.domain.model.RecognitionSettings
import net.calvuz.qstore.domain.repository.RecognitionSettingsRepository
import net.calvuz.qstore.data.opencv.ImageRecognitionValidator
import javax.inject.Inject

@HiltViewModel
class RecognitionSettingsViewModel @Inject constructor(
    private val settingsRepository: RecognitionSettingsRepository,
    private val validator: ImageRecognitionValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecognitionSettingsUiState())
    val uiState: StateFlow<RecognitionSettingsUiState> = _uiState.asStateFlow()

    val currentSettings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecognitionSettings.getDefault()
        )

    val currentPreset = settingsRepository.getCurrentPreset()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Bilanciato"
        )

    init {
        // Osserva cambiamenti nelle impostazioni per validazione
        viewModelScope.launch {
            currentSettings.collect { settings ->
                val validationErrors = settings.validate()
                _uiState.value = _uiState.value.copy(
                    validationErrors = validationErrors,
                    isValid = validationErrors.isEmpty()
                )
            }
        }
    }

    /**
     * Applica un preset predefinito
     */
    fun applyPreset(presetName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                settingsRepository.applyPreset(presetName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Preset '$presetName' applicato"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore applicando preset: ${e.message}"
                )
            }
        }
    }

    /**
     * Aggiorna parametro singolo
     */
    fun updateParameter(parameterUpdate: (RecognitionSettings) -> RecognitionSettings) {
        viewModelScope.launch {
            try {
                val current = currentSettings.value
                val updated = parameterUpdate(current)
                settingsRepository.updateSettings(updated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore aggiornamento: ${e.message}"
                )
            }
        }
    }

    /**
     * Reset alle impostazioni di default
     */
    fun resetToDefault() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                settingsRepository.resetToDefault()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Impostazioni ripristinate"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore reset: ${e.message}"
                )
            }
        }
    }

    /**
     * Testa le impostazioni correnti con database
     */
    fun testCurrentSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isTestingSettings = true,
                    testResult = null
                )

                val analysis = validator.analyzeDatabaseQuality()

                _uiState.value = _uiState.value.copy(
                    isTestingSettings = false,
                    testResult = SettingsTestResult(
                        databaseStats = analysis,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingSettings = false,
                    error = "Errore test: ${e.message}"
                )
            }
        }
    }

    /**
     * Pulisce messaggi di stato
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            message = null
        )
    }
}

/**
 * Stato UI per le impostazioni di riconoscimento
 */
data class RecognitionSettingsUiState(
    val isLoading: Boolean = false,
    val isTestingSettings: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val isValid: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val testResult: SettingsTestResult? = null
)

/**
 * Risultato test impostazioni
 */
data class SettingsTestResult(
    val databaseStats: ImageRecognitionValidator.DatabaseAnalysisResult,
    val timestamp: Long
)
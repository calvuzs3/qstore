package net.calvuz.qstore.settings.presentation.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository
import javax.inject.Inject

@HiltViewModel
class DisplaySettingsViewModel @Inject constructor(
    private val settingsRepository: DisplaySettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DisplaySettingsUiState())
    val uiState: StateFlow<DisplaySettingsUiState> = _uiState.asStateFlow()

    /**
     * Flow delle impostazioni correnti, osservato dalla UI.
     */
    val currentSettings: StateFlow<DisplaySettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DisplaySettings.getDefault()
        )

    /**
     * Aggiorna lo stile delle card articoli.
     */
    fun setArticleCardStyle(style: ArticleCardStyle) {
        viewModelScope.launch {
            try {
                settingsRepository.setArticleCardStyle(style)
                showMessage("Stile card aggiornato")
            } catch (e: Exception) {
                showError("Errore: ${e.message}")
            }
        }
    }

    /**
     * Aggiorna la visibilità degli indicatori di stock.
     */
    fun setShowStockIndicators(show: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setShowStockIndicators(show)
            } catch (e: Exception) {
                showError("Errore: ${e.message}")
            }
        }
    }

    /**
     * Aggiorna la visibilità delle immagini articoli.
     */
    fun setShowArticleImages(show: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setShowArticleImages(show)
            } catch (e: Exception) {
                showError("Errore: ${e.message}")
            }
        }
    }

    /**
     * Ripristina tutte le impostazioni display ai valori di default.
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
     * Pulisce i messaggi di stato (errore e successo).
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            message = null
        )
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
}

/**
 * Stato UI per la schermata impostazioni display.
 */
data class DisplaySettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

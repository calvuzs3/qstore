package net.calvuz.qstore.settings.presentation.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.calvuz.qstore.settings.domain.model.ServerSettings
import net.calvuz.qstore.settings.domain.repository.ServerSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(
    private val serverSettingsRepository: ServerSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerSettingsUiState())
    val uiState: StateFlow<ServerSettingsUiState> = _uiState.asStateFlow()

    val currentSettings: StateFlow<ServerSettings> = serverSettingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ServerSettings.getDefault()
        )

    fun save(baseUrl: String) {
        viewModelScope.launch {
            try {
                serverSettingsRepository.setBaseUrl(baseUrl)
                _uiState.value = _uiState.value.copy(message = "Indirizzo server salvato")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Errore: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}

data class ServerSettingsUiState(
    val error: String? = null,
    val message: String? = null
)

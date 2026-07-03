package net.calvuz.qstore.app.presentation.ui.locations.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.usecase.location.GetLocationsUseCase
import net.calvuz.qstore.app.domain.usecase.location.SaveLocationUseCase
import javax.inject.Inject

@HiltViewModel
class LocationEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLocationsUseCase: GetLocationsUseCase,
    private val saveLocationUseCase: SaveLocationUseCase
) : ViewModel() {

    private val locationUuid: String? = savedStateHandle.get<String>("locationUuid")
    val isEditMode: Boolean = locationUuid != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _uiState = MutableStateFlow<LocationEditUiState>(LocationEditUiState.Idle)
    val uiState: StateFlow<LocationEditUiState> = _uiState.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private var originalLocation: Location? = null

    val isFormValid: StateFlow<Boolean> = _name.map { it.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasChanges: StateFlow<Boolean> = combine(
        _name,
        _notes
    ) { name, notes ->
        if (isEditMode && originalLocation != null) {
            name != originalLocation?.name || notes != originalLocation?.notes
        } else {
            name.isNotBlank() || notes.isNotBlank()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        if (isEditMode && locationUuid != null) {
            loadLocation(locationUuid)
        }
    }

    private fun loadLocation(uuid: String) {
        viewModelScope.launch {
            _uiState.value = LocationEditUiState.Loading

            getLocationsUseCase.getByUuid(uuid)
                .onSuccess { location ->
                    if (location != null) {
                        originalLocation = location
                        _name.value = location.name
                        _notes.value = location.notes
                        _uiState.value = LocationEditUiState.Idle
                    } else {
                        _uiState.value = LocationEditUiState.Error("Magazzino non trovato")
                    }
                }
                .onFailure { error ->
                    _uiState.value = LocationEditUiState.Error(
                        error.message ?: "Errore nel caricamento"
                    )
                }
        }
    }

    fun onNameChange(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun onNotesChange(value: String) {
        _notes.value = value
    }

    fun save() {
        if (_name.value.isBlank()) {
            _nameError.value = "Il nome è obbligatorio"
            return
        }

        viewModelScope.launch {
            _uiState.value = LocationEditUiState.Saving

            val result = if (isEditMode && locationUuid != null) {
                saveLocationUseCase.update(
                    uuid = locationUuid,
                    name = _name.value,
                    notes = _notes.value
                )
            } else {
                saveLocationUseCase.create(
                    name = _name.value,
                    notes = _notes.value
                )
            }

            result
                .onSuccess {
                    _uiState.value = LocationEditUiState.Saved
                }
                .onFailure { error ->
                    if (error.message?.contains("nome esiste") == true) {
                        _nameError.value = error.message
                        _uiState.value = LocationEditUiState.Idle
                    } else {
                        _uiState.value = LocationEditUiState.Error(
                            error.message ?: "Errore nel salvataggio"
                        )
                    }
                }
        }
    }

    fun resetError() {
        _uiState.value = LocationEditUiState.Idle
    }
}

sealed class LocationEditUiState {
    data object Idle : LocationEditUiState()
    data object Loading : LocationEditUiState()
    data object Saving : LocationEditUiState()
    data object Saved : LocationEditUiState()
    data class Error(val message: String) : LocationEditUiState()
}

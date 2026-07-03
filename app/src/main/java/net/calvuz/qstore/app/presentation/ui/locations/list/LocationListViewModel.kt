package net.calvuz.qstore.app.presentation.ui.locations.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.usecase.location.DeleteLocationUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetLocationsUseCase
import javax.inject.Inject

@HiltViewModel
class LocationListViewModel @Inject constructor(
    private val getLocationsUseCase: GetLocationsUseCase,
    private val deleteLocationUseCase: DeleteLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationListUiState>(LocationListUiState.Loading)
    val uiState: StateFlow<LocationListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val locations: StateFlow<List<Location>> = combine(
        getLocationsUseCase.observeAll(),
        _searchQuery
    ) { locations, query ->
        if (query.isBlank()) {
            locations
        } else {
            locations.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Motivo di blocco cancellazione per uuid (null = eliminabile)
    private val _deleteBlockReasons = MutableStateFlow<Map<String, String?>>(emptyMap())
    val deleteBlockReasons: StateFlow<Map<String, String?>> = _deleteBlockReasons.asStateFlow()

    init {
        loadLocations()
    }

    private fun loadLocations() {
        viewModelScope.launch {
            _uiState.value = LocationListUiState.Loading

            getLocationsUseCase.observeAll()
                .catch { e ->
                    _uiState.value = LocationListUiState.Error(
                        e.message ?: "Errore nel caricamento"
                    )
                }
                .collect { locations ->
                    _uiState.value = LocationListUiState.Success
                    loadDeleteBlockReasons(locations)
                }
        }
    }

    private fun loadDeleteBlockReasons(locations: List<Location>) {
        viewModelScope.launch {
            val reasons = mutableMapOf<String, String?>()
            locations.forEach { location ->
                reasons[location.uuid] = when {
                    locations.size <= 1 -> "È l'unico magazzino rimasto"
                    deleteLocationUseCase.hasStock(location.uuid).getOrDefault(false) ->
                        "Contiene ancora giacenza"
                    else -> null
                }
            }
            _deleteBlockReasons.value = reasons
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteLocation(uuid: String) {
        viewModelScope.launch {
            deleteLocationUseCase(uuid)
                .onSuccess {
                    _snackbarMessage.emit("Magazzino eliminato")
                }
                .onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "Errore nell'eliminazione")
                }
        }
    }

    fun refresh() {
        loadLocations()
    }
}

sealed class LocationListUiState {
    data object Loading : LocationListUiState()
    data object Success : LocationListUiState()
    data class Error(val message: String) : LocationListUiState()
}

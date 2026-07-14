package net.calvuz.qstore.app.presentation.ui.movements.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.app.domain.usecase.inventory.GetLocationQuantityUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetActiveLocationUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetLocationsUseCase
import net.calvuz.qstore.app.domain.usecase.movement.AddMovementUseCase
import javax.inject.Inject

data class AddMovementState(
    val article: Article? = null,
    val inventory: Inventory? = null,

    // Magazzini
    val locations: List<Location> = emptyList(),
    val fromLocationUuid: String? = null,
    val toLocationUuid: String? = null,
    val fromQuantity: Double = 0.0,
    val toQuantity: Double = 0.0,

    // Form fields
    val type: MovementType = MovementType.IN,
    val quantity: String = "1",
    val notes: String = "",

    // Validation errors
    val quantityError: String? = null,
    val locationError: String? = null,

    // UI state
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface AddMovementEvent {
    data object NavigateBack : AddMovementEvent
    data class ShowError(val message: String) : AddMovementEvent
    data class ShowSuccess(val message: String) : AddMovementEvent
}

@HiltViewModel
class AddMovementViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val addMovementUseCase: AddMovementUseCase,
    private val getLocationsUseCase: GetLocationsUseCase,
    private val getActiveLocationUseCase: GetActiveLocationUseCase,
    private val getLocationQuantityUseCase: GetLocationQuantityUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle.get<String>("articleId"))

    private val _state = MutableStateFlow(AddMovementState())
    val state: StateFlow<AddMovementState> = _state.asStateFlow()

    private val _events = MutableStateFlow<AddMovementEvent?>(null)
    val events: StateFlow<AddMovementEvent?> = _events.asStateFlow()

    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load article
            getArticleUseCase.getByUuid(articleId)
                .onSuccess { article ->
                    if (article != null) {
                        _state.update { it.copy(article = article) }
                        // Load inventory (aggregato) e magazzini
                        loadInventory(articleId)
                        loadLocations(articleId)
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Articolo non trovato"
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Errore nel caricamento"
                        )
                    }
                }
        }
    }

    private fun loadInventory(articleId: String) {
        viewModelScope.launch {
            getArticleUseCase.getInventory(articleId)
                .onSuccess { inventory ->
                    _state.update {
                        it.copy(
                            inventory = inventory,
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun loadLocations(articleId: String) {
        viewModelScope.launch {
            getLocationsUseCase.getAll()
                .onSuccess { locations ->
                    val activeLocationUuid = getActiveLocationUseCase().first()?.uuid
                    val defaultUuid = activeLocationUuid
                        ?.takeIf { uuid -> locations.any { it.uuid == uuid } }
                        ?: locations.firstOrNull()?.uuid

                    _state.update {
                        it.copy(
                            locations = locations,
                            fromLocationUuid = defaultUuid,
                            toLocationUuid = defaultUuid
                        )
                    }
                    refreshQuantities(articleId)
                }
        }
    }

    private fun refreshQuantities(articleId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val fromQuantity = currentState.fromLocationUuid
                ?.let { getLocationQuantityUseCase(articleId, it) } ?: 0.0
            val toQuantity = currentState.toLocationUuid
                ?.let { getLocationQuantityUseCase(articleId, it) } ?: 0.0

            _state.update { it.copy(fromQuantity = fromQuantity, toQuantity = toQuantity) }
        }
    }

    fun onTypeChange(type: MovementType) {
        _state.update { currentState ->
            var newState = currentState.copy(type = type, quantityError = null, locationError = null)

            // Trasferimento richiede due magazzini diversi: se coincidono, sceglie
            // per "A" il primo magazzino diverso da "Da" (se esiste).
            if (type == MovementType.TRANSFER && newState.fromLocationUuid == newState.toLocationUuid) {
                val alternative = newState.locations
                    .firstOrNull { it.uuid != newState.fromLocationUuid }
                    ?.uuid
                if (alternative != null) {
                    newState = newState.copy(toLocationUuid = alternative)
                }
            }

            newState
        }
        refreshQuantities(articleId)
    }

    fun onFromLocationChange(locationUuid: String) {
        _state.update { currentState ->
            var newState = currentState.copy(fromLocationUuid = locationUuid, locationError = null)

            if (newState.type == MovementType.TRANSFER && locationUuid == newState.toLocationUuid) {
                val alternative = newState.locations
                    .firstOrNull { it.uuid != locationUuid }
                    ?.uuid
                newState = newState.copy(toLocationUuid = alternative)
            }

            newState
        }
        refreshQuantities(articleId)
    }

    fun onToLocationChange(locationUuid: String) {
        _state.update { it.copy(toLocationUuid = locationUuid, locationError = null) }
        refreshQuantities(articleId)
    }

    fun onQuantityChange(value: String) {
        // Filtra solo numeri e punto decimale
        val filtered = value.filter { it.isDigit() || it == '.' }

        // Previeni multipli punti decimali
        val dotCount = filtered.count { it == '.' }
        val finalValue = if (dotCount > 1) {
            filtered.substring(0, filtered.lastIndexOf('.'))
        } else {
            filtered
        }

        _state.update { it.copy(quantity = finalValue, quantityError = null) }
    }

    fun onQuantityStep(delta: Double) {
        val current = _state.value.quantity.toDoubleOrNull() ?: 0.0
        val next = (current + delta).coerceAtLeast(0.0)
        val formatted = if (next == next.toLong().toDouble()) {
            next.toLong().toString()
        } else {
            next.toString()
        }
        _state.update { it.copy(quantity = formatted, quantityError = null) }
    }

    fun onNotesChange(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun onSaveClick() {
        if (validateForm()) {
            registerMovement()
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _state.value
        var isValid = true

        // Validate quantity
        val quantity = currentState.quantity.toDoubleOrNull()
        when {
            currentState.quantity.isBlank() -> {
                _state.update { it.copy(quantityError = "La quantità è obbligatoria") }
                isValid = false
            }
            quantity == null || quantity <= 0 -> {
                _state.update { it.copy(quantityError = "La quantità deve essere maggiore di 0") }
                isValid = false
            }
        }

        // Validate magazzini (solo se la quantità è già valida, per non sovrascrivere l'errore sopra)
        if (isValid && quantity != null) {
            when (currentState.type) {
                MovementType.IN -> {
                    if (currentState.toLocationUuid == null) {
                        _state.update { it.copy(locationError = "Seleziona il magazzino di destinazione") }
                        isValid = false
                    }
                }
                MovementType.OUT -> {
                    if (currentState.fromLocationUuid == null) {
                        _state.update { it.copy(locationError = "Seleziona il magazzino di partenza") }
                        isValid = false
                    } else if (quantity > currentState.fromQuantity) {
                        _state.update {
                            it.copy(quantityError = "Quantità insufficiente (disponibile: ${currentState.fromQuantity})")
                        }
                        isValid = false
                    }
                }
                MovementType.TRANSFER -> {
                    if (currentState.fromLocationUuid == null || currentState.toLocationUuid == null) {
                        _state.update { it.copy(locationError = "Seleziona entrambi i magazzini") }
                        isValid = false
                    } else if (currentState.fromLocationUuid == currentState.toLocationUuid) {
                        _state.update { it.copy(locationError = "I magazzini di partenza e arrivo devono essere diversi") }
                        isValid = false
                    } else if (quantity > currentState.fromQuantity) {
                        _state.update {
                            it.copy(quantityError = "Quantità insufficiente (disponibile: ${currentState.fromQuantity})")
                        }
                        isValid = false
                    }
                }
                MovementType.ADJUSTMENT -> Unit // non selezionabile da questa schermata
            }
        }

        return isValid
    }

    private fun registerMovement() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val currentState = _state.value
            val article = currentState.article ?: return@launch

            val (fromLocationUuid, toLocationUuid) = when (currentState.type) {
                MovementType.IN -> null to currentState.toLocationUuid
                MovementType.OUT -> currentState.fromLocationUuid to null
                MovementType.TRANSFER -> currentState.fromLocationUuid to currentState.toLocationUuid
                MovementType.ADJUSTMENT -> currentState.fromLocationUuid to currentState.toLocationUuid
            }

            addMovementUseCase(
                articleUuid = article.uuid,
                type = currentState.type,
                fromLocationUuid = fromLocationUuid,
                toLocationUuid = toLocationUuid,
                quantity = currentState.quantity.toDouble(),
                notes = currentState.notes.trim()
            ).onSuccess {
                val movementType = when (currentState.type) {
                    MovementType.IN -> "Carico"
                    MovementType.OUT -> "Scarico"
                    MovementType.ADJUSTMENT -> "Rettifica"
                    MovementType.TRANSFER -> "Trasferimento"
                }
                _events.value = AddMovementEvent.ShowSuccess(
                    "$movementType registrato con successo"
                )
                _events.value = AddMovementEvent.NavigateBack
            }.onFailure { throwable ->
                _state.update { it.copy(isSaving = false) }
                _events.value = AddMovementEvent.ShowError(
                    throwable.message ?: "Errore nella registrazione"
                )
            }
        }
    }

    fun onEventConsumed() {
        _events.value = null
    }
}

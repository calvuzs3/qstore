package net.calvuz.qstore.app.presentation.ui.movements.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.app.domain.usecase.movement.AddMovementUseCase
import javax.inject.Inject

data class AddMovementState(
    val article: Article? = null,
    val inventory: Inventory? = null,

    // Form fields
    val type: MovementType = MovementType.IN,
    val quantity: String = "",
    val notes: String = "",

    // Validation errors
    val quantityError: String? = null,

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
                        // Load inventory
                        loadInventory(articleId)
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

    fun onTypeChange(type: MovementType) {
        _state.update { it.copy(type = type) }
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
            currentState.type == MovementType.OUT -> {
                val available = currentState.inventory?.currentQuantity ?: 0.0
                if (quantity > available) {
                    _state.update {
                        it.copy(quantityError = "Quantità insufficiente (disponibile: $available)")
                    }
                    isValid = false
                }
            }
        }

        return isValid
    }

    private fun registerMovement() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val currentState = _state.value
            val article = currentState.article ?: return@launch

            addMovementUseCase(
                articleUuid = article.uuid,
                type = currentState.type,
                quantity = currentState.quantity.toDouble(),
                notes = currentState.notes.trim()
            ).onSuccess {
                val movementType = when (currentState.type) {
                    MovementType.IN -> "Carico"
                    MovementType.OUT -> "Scarico"
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
package net.calvuz.qstore.categories.presentation.ui.categories.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.categories.domain.usecase.category.GetCategoriesUseCase
import net.calvuz.qstore.categories.domain.usecase.category.SaveCategoryUseCase
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase
) : ViewModel() {

    private val categoryUuid: String? = savedStateHandle.get<String>("categoryUuid")
    val isEditMode: Boolean = categoryUuid != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _uiState = MutableStateFlow<CategoryEditUiState>(CategoryEditUiState.Idle)
    val uiState: StateFlow<CategoryEditUiState> = _uiState.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    // Original category (for edit mode)
    private var originalCategory: ArticleCategory? = null

    val isFormValid: StateFlow<Boolean> = _name.map { it.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasChanges: StateFlow<Boolean> = combine(
        _name,
        _description,
        _notes
    ) { name, description, notes ->
        if (isEditMode && originalCategory != null) {
            name != originalCategory?.name ||
            description != originalCategory?.description ||
            notes != originalCategory?.notes
        } else {
            name.isNotBlank() || description.isNotBlank() || notes.isNotBlank()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        if (isEditMode && categoryUuid != null) {
            loadCategory(categoryUuid)
        }
    }

    private fun loadCategory(uuid: String) {
        viewModelScope.launch {
            _uiState.value = CategoryEditUiState.Loading

            val category = getCategoriesUseCase.getByUuid(uuid)
            if (category != null) {
                originalCategory = category
                _name.value = category.name
                _description.value = category.description
                _notes.value = category.notes
                _uiState.value = CategoryEditUiState.Idle
            } else {
                _uiState.value = CategoryEditUiState.Error("Categoria non trovata")
            }
        }
    }

    fun onNameChange(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun onNotesChange(value: String) {
        _notes.value = value
    }

    fun save() {
        // Validate
        if (_name.value.isBlank()) {
            _nameError.value = "Il nome è obbligatorio"
            return
        }

        viewModelScope.launch {
            _uiState.value = CategoryEditUiState.Saving

            val result = if (isEditMode && categoryUuid != null) {
                saveCategoryUseCase.update(
                    uuid = categoryUuid,
                    name = _name.value,
                    description = _description.value,
                    notes = _notes.value
                )
            } else {
                saveCategoryUseCase.create(
                    name = _name.value,
                    description = _description.value,
                    notes = _notes.value
                )
            }

            result
                .onSuccess {
                    _uiState.value = CategoryEditUiState.Saved
                }
                .onFailure { error ->
                    if (error.message?.contains("nome esiste") == true) {
                        _nameError.value = error.message
                        _uiState.value = CategoryEditUiState.Idle
                    } else {
                        _uiState.value = CategoryEditUiState.Error(
                            error.message ?: "Errore nel salvataggio"
                        )
                    }
                }
        }
    }

    fun resetError() {
        _uiState.value = CategoryEditUiState.Idle
    }
}

sealed class CategoryEditUiState {
    data object Idle : CategoryEditUiState()
    data object Loading : CategoryEditUiState()
    data object Saving : CategoryEditUiState()
    data object Saved : CategoryEditUiState()
    data class Error(val message: String) : CategoryEditUiState()
}

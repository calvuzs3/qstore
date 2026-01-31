package net.calvuz.qstore.categories.presentation.ui.categories.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.categories.domain.usecase.category.DeleteCategoryUseCase
import net.calvuz.qstore.categories.domain.usecase.category.GetCategoriesUseCase
import javax.inject.Inject

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryListUiState>(CategoryListUiState.Loading)
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val categories: StateFlow<List<CategoryWithCount>> = combine(
        getCategoriesUseCase.observeAll(),
        _searchQuery
    ) { categories, query ->
        val filtered = if (query.isBlank()) {
            categories
        } else {
            categories.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        // Map to CategoryWithCount (count will be loaded separately)
        filtered.map { CategoryWithCount(it, null) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Separate flow for article counts
    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoryListUiState.Loading

            getCategoriesUseCase.observeAll()
                .catch { e ->
                    _uiState.value = CategoryListUiState.Error(
                        e.message ?: "Errore nel caricamento"
                    )
                }
                .collect { categories ->
                    _uiState.value = CategoryListUiState.Success
                    // Load article counts for each category
                    loadArticleCounts(categories)
                }
        }
    }

    private fun loadArticleCounts(categories: List<ArticleCategory>) {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            categories.forEach { category ->
                counts[category.uuid] = deleteCategoryUseCase.countArticles(category.uuid)
            }
            _categoryCounts.value = counts
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteCategory(uuid: String) {
        viewModelScope.launch {
            deleteCategoryUseCase(uuid)
                .onSuccess {
                    _snackbarMessage.emit("Categoria eliminata")
                }
                .onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "Errore nell'eliminazione")
                }
        }
    }

    fun refresh() {
        loadCategories()
    }

    fun getArticleCount(categoryId: String): Int {
        return _categoryCounts.value[categoryId] ?: 0
    }
}

data class CategoryWithCount(
    val category: ArticleCategory,
    val articleCount: Int?
)

sealed class CategoryListUiState {
    data object Loading : CategoryListUiState()
    data object Success : CategoryListUiState()
    data class Error(val message: String) : CategoryListUiState()
}

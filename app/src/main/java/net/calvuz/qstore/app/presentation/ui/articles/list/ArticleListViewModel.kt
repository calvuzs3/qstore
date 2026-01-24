package net.calvuz.qstore.app.presentation.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.ArticleCategory
import net.calvuz.qstore.app.domain.repository.ArticleCategoryRepository
import net.calvuz.qstore.app.domain.usecase.article.DeleteArticleUseCase
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import javax.inject.Inject

/**
 * ViewModel per Article List Screen
 *
 * Gestisce:
 * - Lista articoli
 * - Search
 * - Filtri per categoria (da database)
 * - Eliminazione articoli
 */
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val categoryRepository: ArticleCategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _uiState = MutableStateFlow<ArticleListUiState>(ArticleListUiState.Loading)
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    // Categorie dal database
    val categories: StateFlow<List<ArticleCategory>> = categoryRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Osserva articoli e applica filtri
    val articles: StateFlow<List<Article>> = combine(
        getArticleUseCase.observeAll(),
        _searchQuery,
        _selectedCategoryId
    ) { articles, query, categoryId ->
        var filtered = articles

        // Filtra per categoria (usando ID)
        if (categoryId != null) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        // Filtra per search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { article ->
                article.name.contains(query, ignoreCase = true) ||
                        article.description.contains(query, ignoreCase = true) ||
                        article.codeOEM.contains(query, ignoreCase = true) ||
                        article.codeERP.contains(query, ignoreCase = true) ||
                        article.codeBM.contains(query, ignoreCase = true)
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadArticles()
    }

    /**
     * Carica articoli
     */
    private fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = ArticleListUiState.Loading

            getArticleUseCase.observeAll()
                .catch { e ->
                    _uiState.value = ArticleListUiState.Error(
                        e.message ?: "Errore nel caricamento"
                    )
                }
                .collect {
                    _uiState.value = ArticleListUiState.Success
                }
        }
    }

    /**
     * Aggiorna search query
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Seleziona categoria per filtrare (usa ID categoria)
     */
    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * Reset filtri
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
    }

    /**
     * Elimina articolo
     */
    fun deleteArticle(articleUuid: String) {
        viewModelScope.launch {
            deleteArticleUseCase(articleUuid)
                .onFailure { error ->
                    _uiState.value = ArticleListUiState.Error(
                        error.message ?: "Errore nell'eliminazione"
                    )
                }
        }
    }

    /**
     * Refresh lista
     */
    fun refresh() {
        loadArticles()
    }

    /**
     * Trova il nome della categoria dato l'ID
     */
    fun getCategoryName(categoryId: String): String? {
        return categories.value.find { it.uuid == categoryId }?.name
    }
}

/**
 * Stati UI per Article List
 */
sealed class ArticleListUiState {
    data object Loading : ArticleListUiState()
    data object Success : ArticleListUiState()
    data class Error(val message: String) : ArticleListUiState()
}
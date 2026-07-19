package net.calvuz.qstore.app.presentation.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import net.calvuz.qstore.app.domain.usecase.article.DeleteArticleUseCase
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.app.domain.usecase.inventory.GetLocationStockUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetActiveLocationUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetLocationsUseCase
import net.calvuz.qstore.app.domain.usecase.location.SetActiveLocationUseCase
import net.calvuz.qstore.app.presentation.ui.articles.model.ArticleSortOrder
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.usecase.display.GetDisplaySettingsUseCase
import javax.inject.Inject

/**
 * ViewModel per Article List Screen
 *
 * Gestisce:
 * - Lista articoli
 * - Search
 * - Filtri per categoria (da database)
 * - Filtro/quantità per magazzino attivo
 * - Eliminazione articoli
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val getDisplaySettingsUseCase: GetDisplaySettingsUseCase,
    private val categoryRepository: ArticleCategoryRepository,
    private val getActiveLocationUseCase: GetActiveLocationUseCase,
    private val setActiveLocationUseCase: SetActiveLocationUseCase,
    private val getLocationsUseCase: GetLocationsUseCase,
    private val getLocationStockUseCase: GetLocationStockUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _sortOrder = MutableStateFlow(ArticleSortOrder.RECENT_UPDATED_FIRST)
    val sortOrder: StateFlow<ArticleSortOrder> = _sortOrder.asStateFlow()

    // Incrementato da retry() per far ripartire l'observe dopo un errore — la lista si
    // aggiorna già da sola ad ogni emissione del DAO, questo serve solo a recuperare da un
    // errore che ha terminato il Flow (es. eccezione dal DB), non per un refresh manuale.
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ArticleListUiState> = retryTrigger
        .flatMapLatest { getArticleUseCase.observeAll() }
        .map<List<Article>, ArticleListUiState> { ArticleListUiState.Success }
        .catch { e -> emit(ArticleListUiState.Error(e.message ?: "Errore nel caricamento")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArticleListUiState.Loading
        )

    val displaySettings: StateFlow<DisplaySettings> = getDisplaySettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DisplaySettings.getDefault()
        )

    // Categorie dal database
    val categories: StateFlow<List<ArticleCategory>> = categoryRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Magazzino attivo (null = "Tutti i magazzini") e lista magazzini per il selettore
    val activeLocation: StateFlow<Location?> = getActiveLocationUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val locations: StateFlow<List<Location>> = getLocationsUseCase.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Giacenza per articolo nel magazzino attivo; null = nessun magazzino selezionato (nessun filtro)
    private val stockByArticle: Flow<Map<String, Double>?> = activeLocation.flatMapLatest { location ->
        if (location == null) flowOf(null) else getLocationStockUseCase(location.uuid)
    }

    // Osserva articoli e applica filtri
    val articles: StateFlow<List<ArticleWithStock>> = combine(
        getArticleUseCase.observeAll(),
        _searchQuery,
        _selectedCategoryId,
        _sortOrder,
        stockByArticle
    ) { articles, query, categoryId, sortOrder, stockMap ->
        val filtered = applyFiltersAndSort(articles, query, categoryId, sortOrder)
        if (stockMap == null) {
            filtered.map { ArticleWithStock(it, quantity = null) }
        } else {
            filtered.mapNotNull { article ->
                val quantity = stockMap[article.uuid] ?: 0.0
                if (quantity > 0) ArticleWithStock(article, quantity) else null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
     * Aggiorna ordinamento
     */
    fun updateSortOrder(sortOrder: ArticleSortOrder) {
        _sortOrder.value = sortOrder
    }

    /**
     * Reset filtri
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
    }

    /**
     * Seleziona il magazzino attivo (null = "Tutti i magazzini")
     */
    fun selectLocation(locationUuid: String?) {
        viewModelScope.launch {
            setActiveLocationUseCase(locationUuid)
        }
    }

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    /**
     * Elimina articolo
     */
    fun deleteArticle(articleUuid: String) {
        viewModelScope.launch {
            deleteArticleUseCase(articleUuid)
                .onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "Errore nell'eliminazione")
                }
        }
    }

    /**
     * Ritenta l'observe dopo un errore (es. eccezione dal DB che ha terminato il Flow).
     * Non serve per un refresh manuale: la lista è già reattiva e si aggiorna da sola.
     */
    fun retry() {
        retryTrigger.value += 1
    }

    /**
     * Trova il nome della categoria dato l'ID
     */
    fun getCategoryName(categoryId: String): String? {
        return categories.value.find { it.uuid == categoryId }?.name
    }

    /**
     * Applica filtri e ordinamento alla lista articoli
     */
    private fun applyFiltersAndSort(
        articles: List<Article>,
        searchQuery: String,
        categoryId: String?,
        sortOrder: ArticleSortOrder
    ): List<Article> {
        var filtered = articles

        // Filtra per categoria
        if (categoryId != null) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        // Filtra per search query
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { article ->
                article.name.contains(searchQuery, ignoreCase = true) ||
                        article.description.contains(searchQuery, ignoreCase = true) ||
                        article.codeOEM.contains(searchQuery, ignoreCase = true) ||
                        article.codeERP.contains(searchQuery, ignoreCase = true) ||
                        article.codeBM.contains(searchQuery, ignoreCase = true)
            }
        }

        // Applica ordinamento
        filtered = when (sortOrder) {
            ArticleSortOrder.RECENT_UPDATED_FIRST -> filtered.sortedByDescending { it.updatedAt }
            ArticleSortOrder.OLDEST_UPDATED_FIRST -> filtered.sortedBy { it.updatedAt }
            ArticleSortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
            ArticleSortOrder.DESCRIPTION -> filtered.sortedBy { it.description.lowercase() }
            ArticleSortOrder.NOTES -> filtered.sortedBy { it.notes.lowercase() }
        }

        return filtered
    }
}


/**
 * Articolo decorato con la sua giacenza nel magazzino attivo.
 * `quantity == null` significa "Tutti i magazzini" (nessuna quantità mostrata, filtro non applicato).
 */
data class ArticleWithStock(
    val article: Article,
    val quantity: Double?
)

/**
 * Stati UI per Article List
 */
sealed class ArticleListUiState {
    data object Loading : ArticleListUiState()
    data object Success : ArticleListUiState()
    data class Error(val message: String) : ArticleListUiState()
}
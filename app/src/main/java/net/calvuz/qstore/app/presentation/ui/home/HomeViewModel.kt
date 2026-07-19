package net.calvuz.qstore.app.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.LocationStats
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetLocationsUseCase
import net.calvuz.qstore.app.domain.usecase.movement.GetMovementsUseCase
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository
import javax.inject.Inject

/**
 * ViewModel per Home Screen (Dashboard)
 *
 * Gestisce:
 * - Statistiche magazzino (totali + per ubicazione)
 * - Articoli sotto scorta
 * - Ultimi movimenti
 * - Ultimi articoli creati
 * - Visibilità opzionale delle sezioni (da DisplaySettings)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val getMovementsUseCase: GetMovementsUseCase,
    private val getLocationsUseCase: GetLocationsUseCase,
    private val displaySettingsRepository: DisplaySettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Impostazioni di visualizzazione della dashboard, osservate a parte dal caricamento
     * dati — non richiedono un refresh dei dati per aggiornarsi in UI.
     */
    val displaySettings: StateFlow<DisplaySettings> = displaySettingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DisplaySettings.getDefault()
        )

    init {
        loadDashboardData()
    }

    /**
     * Carica tutti i dati della dashboard
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                // Carica articoli
                val articles = getArticleUseCase.getAll()
                    .getOrElse { emptyList() }

                // Carica ultimi movimenti
                val recentMovements = getMovementsUseCase.getRecent(limit = 5)
                    .getOrElse { emptyList() }

                // Carica ultimi articoli creati
                val recentArticles = getArticleUseCase.getRecentlyCreated(limit = 5)
                    .getOrElse { emptyList() }

                // Statistiche per magazzino
                val locationStats = getLocationsUseCase.getStats()
                    .getOrElse { emptyList() }

                // Calcola statistiche
                val stats = calculateStats(articles)

                // Identifica articoli sotto scorta
                val lowStockArticles = articles.filter { article ->
                    val inventory = getArticleUseCase.getInventory(article.uuid)
                        .getOrNull()
                    inventory != null &&
                            article.reorderLevel > 0.0 &&
                            inventory.currentQuantity <= article.reorderLevel
                }

                _uiState.value = HomeUiState.Success(
                    stats = stats,
                    locationStats = locationStats,
                    lowStockArticles = lowStockArticles,
                    recentMovements = recentMovements,
                    recentArticles = recentArticles
                )

            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.message ?: "Errore nel caricamento dei dati"
                )
            }
        }
    }

    /**
     * Calcola statistiche magazzino
     */
    private suspend fun calculateStats(articles: List<Article>): DashboardStats {
        var totalArticles = articles.size
        var totalValue = 0.0
        var articlesWithStock = 0
        var articlesOutOfStock = 0

        articles.forEach { article ->
            val inventory = getArticleUseCase.getInventory(article.uuid)
                .getOrNull()

            if (inventory != null) {
                if (inventory.currentQuantity > 0) {
                    articlesWithStock++
                } else {
                    articlesOutOfStock++
                }
                // TODO: Calcola valore se aggiungi campo prezzo ad Article
                // totalValue += inventory.quantity * article.price
            }
        }

        return DashboardStats(
            totalArticles = totalArticles,
            articlesWithStock = articlesWithStock,
            articlesOutOfStock = articlesOutOfStock,
            totalValue = totalValue
        )
    }

    /**
     * Refresh dati dashboard
     */
    fun refresh() {
        loadDashboardData()
    }
}

/**
 * Stati UI della Home Screen
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()

    data class Success(
        val stats: DashboardStats,
        val locationStats: List<LocationStats>,
        val lowStockArticles: List<Article>,
        val recentMovements: List<Movement>,
        val recentArticles: List<Article>
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

/**
 * Statistiche dashboard
 */
data class DashboardStats(
    val totalArticles: Int,
    val articlesWithStock: Int,
    val articlesOutOfStock: Int,
    val totalValue: Double
)

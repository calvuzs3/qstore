package net.calvuz.qstore.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.model.Movement
import net.calvuz.qstore.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.domain.usecase.movement.GetMovementsUseCase
import javax.inject.Inject

/**
 * ViewModel per Home Screen (Dashboard)
 *
 * Gestisce:
 * - Statistiche magazzino
 * - Articoli sotto scorta
 * - Ultimi movimenti
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val getMovementsUseCase: GetMovementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
                    lowStockArticles = lowStockArticles,
                    recentMovements = recentMovements
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
        val lowStockArticles: List<Article>,
        val recentMovements: List<Movement>
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
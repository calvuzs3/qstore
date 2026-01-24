package net.calvuz.qstore.app.presentation.ui.movements.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.app.domain.usecase.movement.GetAllMovementsUseCase
import javax.inject.Inject

data class MovementWithArticle(
    val movement: Movement,
    val article: Article?
)

sealed interface MovementFilterType {
    data object All : MovementFilterType
    data object In : MovementFilterType
    data object Out : MovementFilterType
}

data class MovementListState(
    val movements: List<MovementWithArticle> = emptyList(),
    val filteredMovements: List<MovementWithArticle> = emptyList(),
    val searchQuery: String = "",
    val filterType: MovementFilterType = MovementFilterType.All,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MovementListViewModel @Inject constructor(
    private val getAllMovementsUseCase: GetAllMovementsUseCase,
    private val getArticleUseCase: GetArticleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MovementListState())
    val state: StateFlow<MovementListState> = _state.asStateFlow()

    init {
        loadMovements()
        observeMovements()
    }

    private fun observeMovements() {
        viewModelScope.launch {
            getAllMovementsUseCase.observeAll().collect { movements ->
                loadMovementsWithArticles(movements)
            }
        }
    }

    private fun loadMovements() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getAllMovementsUseCase.getAll()
                .onSuccess { movements ->
                    loadMovementsWithArticles(movements)
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

    private suspend fun loadMovementsWithArticles(movements: List<Movement>) {
        val movementsWithArticles = movements.map { movement ->
            val article = getArticleUseCase.getByUuid(movement.articleUuid).getOrNull()
            MovementWithArticle(movement, article)
        }

        _state.update {
            it.copy(
                movements = movementsWithArticles,
                isLoading = false
            )
        }

        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onFilterTypeChange(filterType: MovementFilterType) {
        _state.update { it.copy(filterType = filterType) }
        applyFilters()
    }

    private fun applyFilters() {
        val currentState = _state.value
        var filtered = currentState.movements

        // Filtro per tipo movimento
        filtered = when (currentState.filterType) {
            MovementFilterType.All -> filtered
            MovementFilterType.In -> filtered.filter { it.movement.type == MovementType.IN }
            MovementFilterType.Out -> filtered.filter { it.movement.type == MovementType.OUT }
        }

        // Filtro per ricerca (nome articolo o note)
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            filtered = filtered.filter { item ->
                item.article?.name?.lowercase()?.contains(query) == true ||
                        item.movement.notes.lowercase().contains(query)
            }
        }

        _state.update { it.copy(filteredMovements = filtered) }
    }

    fun refresh() {
        loadMovements()
    }
}
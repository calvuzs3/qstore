package net.calvuz.qstore.app.presentation.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.app.domain.model.ArticleImage
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import net.calvuz.qstore.app.domain.usecase.article.DeleteArticleUseCase
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.app.domain.usecase.inventory.ObserveLocationQuantityUseCase
import net.calvuz.qstore.app.domain.usecase.location.GetActiveLocationUseCase
import net.calvuz.qstore.app.domain.usecase.movement.GetMovementsByArticleUseCase
import net.calvuz.qstore.app.domain.usecase.recognition.DeleteArticleImageUseCase
import net.calvuz.qstore.app.domain.usecase.recognition.GetArticleImagesUseCase
import javax.inject.Inject

data class ArticleDetailState(
    val article: Article? = null,
    val category: ArticleCategory? = null,  // Categoria completa per mostrare il nome
    val inventory: Inventory? = null,
    // Magazzino attivo (stesso filtro persistente della lista articoli, vedi
    // ActiveLocationRepository) e giacenza SOLO in quel magazzino — null se nessun magazzino
    // è selezionato ("Tutti i magazzini"). Risolve un difetto reale: il dettaglio mostrava
    // sempre il totale su tutte le ubicazioni, confondendo l'utente che apriva un articolo
    // da un magazzino specifico aspettandosi la quantità lì, non la somma.
    val activeLocation: Location? = null,
    val activeLocationQuantity: Double? = null,
    val movements: List<Movement> = emptyList(),
    val images: List<ArticleImage> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false
)

sealed interface ArticleDetailEvent {
    data object NavigateBack : ArticleDetailEvent
    data object NavigateToEdit : ArticleDetailEvent
    data object NavigateToAddMovement : ArticleDetailEvent
    data class ShowError(val message: String) : ArticleDetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val getMovementsByArticleUseCase: GetMovementsByArticleUseCase,
    private val getArticleImagesUseCase: GetArticleImagesUseCase,
    private val deleteArticleImageUseCase: DeleteArticleImageUseCase,
    private val categoryRepository: ArticleCategoryRepository,
    private val getActiveLocationUseCase: GetActiveLocationUseCase,
    private val observeLocationQuantityUseCase: ObserveLocationQuantityUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle.get<String>("articleId"))

    private val _state = MutableStateFlow(ArticleDetailState())
    val state: StateFlow<ArticleDetailState> = _state.asStateFlow()

    private val _events = MutableStateFlow<ArticleDetailEvent?>(null)
    val events: StateFlow<ArticleDetailEvent?> = _events.asStateFlow()

    init {
        loadArticle()
        loadMovements()
        loadImages()
        observeArticleChanges()
        observeActiveLocationStock()
    }

    /**
     * Riusa lo stesso magazzino "attivo" persistente della lista articoli (ActiveLocationRepository)
     * — se l'utente ci ha filtrato per magazzino prima di aprire questo articolo, il dettaglio
     * mostra la giacenza SOLO lì, non il totale. Nessun parametro di navigazione necessario:
     * è lo stesso stato globale già usato altrove nell'app.
     */
    private fun observeActiveLocationStock() {
        viewModelScope.launch {
            getActiveLocationUseCase().flatMapLatest { location ->
                if (location == null) {
                    flowOf(null to null)
                } else {
                    observeLocationQuantityUseCase(articleId, location.uuid).map { quantity ->
                        location to quantity
                    }
                }
            }.collect { (location, quantity) ->
                _state.update { it.copy(activeLocation = location, activeLocationQuantity = quantity) }
            }
        }
    }

    private fun observeArticleChanges() {
        viewModelScope.launch {
            getArticleUseCase.observeByUuid(articleId).collect { article ->
                _state.update { it.copy(article = article) }
                // Ricarica categoria quando l'articolo cambia
                article?.let { loadCategory(it.categoryId) }
            }
        }

        viewModelScope.launch {
            getArticleUseCase.observeInventory(articleId).collect { inventory ->
                _state.update { it.copy(inventory = inventory) }
            }
        }
    }

    private fun loadArticle() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val articleResult = getArticleUseCase.getByUuid(articleId)
            val inventoryResult = getArticleUseCase.getInventory(articleId)

            if (articleResult.isSuccess && inventoryResult.isSuccess) {
                val article = articleResult.getOrNull()
                _state.update {
                    it.copy(
                        article = article,
                        inventory = inventoryResult.getOrNull(),
                        isLoading = false
                    )
                }
                // Carica la categoria
                article?.let { loadCategory(it.categoryId) }
            } else {
                val error = articleResult.exceptionOrNull() ?: inventoryResult.exceptionOrNull()
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error?.message ?: "Errore nel caricamento"
                    )
                }
                _events.value = ArticleDetailEvent.ShowError(
                    error?.message ?: "Errore nel caricamento dell'articolo"
                )
            }
        }
    }

    private fun loadCategory(categoryId: String) {
        if (categoryId.isBlank()) return

        viewModelScope.launch {
            val category = categoryRepository.getByUuid(categoryId)
            _state.update { it.copy(category = category) }
        }
    }

    private fun loadMovements() {
        viewModelScope.launch {
            getMovementsByArticleUseCase(articleId)
                .onSuccess { movements ->
                    _state.update { it.copy(movements = movements) }
                }
                .onFailure { throwable ->
                    // Non blocchiamo l'UI per errori sui movimenti
                    _events.value = ArticleDetailEvent.ShowError(
                        "Errore nel caricamento storico: ${throwable.message}"
                    )
                }
        }
    }

    private fun loadImages() {
        viewModelScope.launch {
            getArticleImagesUseCase(articleId)
                .onSuccess { images ->
                    _state.update { it.copy(images = images) }
                }
                .onFailure { throwable ->
                    // Non blocchiamo l'UI per errori sulle immagini
                    _events.value = ArticleDetailEvent.ShowError(
                        "Errore nel caricamento foto: ${throwable.message}"
                    )
                }
        }
    }

    fun onDeleteImage(imageUuid: String) {
        viewModelScope.launch {
            deleteArticleImageUseCase(imageUuid)
                .onSuccess {
                    // Rimuovi immagine dallo stato
                    _state.update {
                        it.copy(
                            images = it.images.filter { img -> img.uuid != imageUuid }
                        )
                    }
                }
                .onFailure { throwable ->
                    _events.value = ArticleDetailEvent.ShowError(
                        "Errore nell'eliminazione foto: ${throwable.message}"
                    )
                }
        }
    }

    fun onEditClick() {
        _events.value = ArticleDetailEvent.NavigateToEdit
    }

    fun onDeleteClick() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun onDeleteConfirm() {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, showDeleteDialog = false) }

            deleteArticleUseCase(articleId)
                .onSuccess {
                    _events.value = ArticleDetailEvent.NavigateBack
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isDeleting = false) }
                    _events.value = ArticleDetailEvent.ShowError(
                        throwable.message ?: "Errore nell'eliminazione"
                    )
                }
        }
    }

    fun onDeleteDismiss() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun onAddMovementClick() {
        _events.value = ArticleDetailEvent.NavigateToAddMovement
    }

    fun onRefresh() {
        loadArticle()
        loadMovements()
        loadImages()
    }

    fun onEventConsumed() {
        _events.value = null
    }
}
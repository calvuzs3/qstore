package net.calvuz.qstore.presentation.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.usecase.article.GetArticleUseCase
import javax.inject.Inject

/**
 * ViewModel per Search Results Screen
 *
 * Carica i dettagli degli articoli trovati dalla ricerca per immagine
 */
@HiltViewModel
class SearchResultsViewModel @Inject constructor(
    private val getArticleUseCase: GetArticleUseCase
) : ViewModel() {

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Carica articoli per UUID
     * Mantiene l'ordine di similarità dalla ricerca
     */
    fun loadArticles(articleUuids: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true

            val loadedArticles = mutableListOf<Article>()

            // Carica articoli mantenendo l'ordine
            articleUuids.forEach { uuid ->
                getArticleUseCase.getByUuid(uuid)
                    .onSuccess { article ->
                        article?.let { loadedArticles.add(it) }
                    }
            }

            _articles.value = loadedArticles
            _isLoading.value = false
        }
    }
}
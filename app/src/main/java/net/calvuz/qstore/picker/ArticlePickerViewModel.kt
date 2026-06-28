package net.calvuz.qstore.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.usecase.article.GetArticleUseCase
import net.calvuz.qstore.provider.ArticleContract
import javax.inject.Inject

@HiltViewModel
class ArticlePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArticleUseCase: GetArticleUseCase,
) : ViewModel() {

    // SavedStateHandle è popolato automaticamente da Hilt con gli extras dell'Intent.
    private val preselected: Set<String> =
        savedStateHandle.get<ArrayList<String>>(ArticleContract.PickerExtras.PRESELECTED_UUIDS)
            ?.toSet() ?: emptySet()

    private val _selectedUuids = MutableStateFlow(preselected)
    private val _searchQuery   = MutableStateFlow("")

    val selectedUuids: StateFlow<Set<String>> = _selectedUuids.asStateFlow()
    val searchQuery: StateFlow<String>         = _searchQuery.asStateFlow()

    val articles: StateFlow<List<Article>> = combine(
        getArticleUseCase.observeAll(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { matches(it, query) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(uuid: String) {
        _selectedUuids.update { current ->
            if (uuid in current) current - uuid else current + uuid
        }
    }

    private fun matches(article: Article, query: String): Boolean =
        listOf(article.name, article.codeOEM, article.codeERP, article.codeBM, article.description)
            .any { it.contains(query, ignoreCase = true) }
}

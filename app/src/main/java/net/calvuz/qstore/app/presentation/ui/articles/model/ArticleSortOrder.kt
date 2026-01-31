package net.calvuz.qstore.app.presentation.ui.articles.model

import androidx.compose.runtime.Composable

enum class ArticleSortOrder {
    RECENT_UPDATED_FIRST,
    OLDEST_UPDATED_FIRST,
    NAME,
    DESCRIPTION,
    NOTES,
}

@Composable
fun ArticleSortOrder.getDisplayName(): String {
    return when (this) {
        ArticleSortOrder.RECENT_UPDATED_FIRST -> "Più Recente"
        ArticleSortOrder.OLDEST_UPDATED_FIRST -> "Meno Recente"
        ArticleSortOrder.NAME  -> "Nome"
        ArticleSortOrder.DESCRIPTION -> "Descrizione"
        ArticleSortOrder.NOTES -> "Note"
    }
}
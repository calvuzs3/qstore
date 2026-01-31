package net.calvuz.qstore.app.presentation.ui.articles.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.app.presentation.ui.articles.components.ArticleCard
import net.calvuz.qstore.app.presentation.ui.articles.model.ArticleSortOrder
import net.calvuz.qstore.app.presentation.ui.articles.model.getDisplayName

/**
 * Article List Screen
 *
 * Features:
 * - Lista tutti gli articoli
 * - Search bar (cerca anche nei codici esterni)
 * - Filtro per categoria (dropdown in TopAppBar)
 * - Ordinamento configurabile (dropdown in TopAppBar)
 * - Swipe to delete
 * - Thumbnail foto articolo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDisplaySettings: () -> Unit,
    onArticleClick: (String) -> Unit,
    onAddArticleClick: () -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()

    // Menu states
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Get selected category name for display
    val selectedCategoryName = remember(selectedCategoryId, categories) {
        if (selectedCategoryId == null) {
            "Tutte"
        } else {
            categories.find { it.uuid == selectedCategoryId }?.name ?: "Tutte"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Articoli") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
                    }

                    // Category filter button
                    Box {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(Icons.Default.FilterList, "Filtra per categoria")
                        }

                        CategoryFilterMenu(
                            expanded = showCategoryMenu,
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = viewModel::selectCategory,
                            onDismiss = { showCategoryMenu = false }
                        )
                    }

                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Default.Sort, "Ordina")
                        }

                        SortMenu(
                            expanded = showSortMenu,
                            selectedSort = sortOrder,
                            onSortSelected = viewModel::updateSortOrder,
                            onDismiss = { showSortMenu = false }
                        )
                    }

                    // Options menu
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu")
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Impostazioni") },
                                onClick = {
                                    onNavigateToDisplaySettings()
                                    showOptionsMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddArticleClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, "Aggiungi articolo")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onClearClick = { viewModel.onSearchQueryChange("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Active filters indicator
            if (selectedCategoryId != null || searchQuery.isNotBlank()) {
                ActiveFiltersBar(
                    categoryName = if (selectedCategoryId != null) selectedCategoryName else null,
                    hasSearchQuery = searchQuery.isNotBlank(),
                    onClearFilters = { viewModel.clearFilters() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Sort order indicator
            SortOrderBar(
                sortOrder = sortOrder,
                onResetSort = { viewModel.updateSortOrder(ArticleSortOrder.RECENT_UPDATED_FIRST) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Article list
            when (uiState) {
                is ArticleListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ArticleListUiState.Error -> {
                    ErrorMessage(
                        message = (uiState as ArticleListUiState.Error).message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is ArticleListUiState.Success -> {
                    if (articles.isEmpty()) {
                        EmptyState(
                            message = if (searchQuery.isNotBlank() || selectedCategoryId != null) {
                                "Nessun articolo trovato"
                            } else {
                                "Nessun articolo in magazzino"
                            },
                            onAction = if (searchQuery.isNotBlank() || selectedCategoryId != null) {
                                { viewModel.clearFilters() }
                            } else {
                                onAddArticleClick
                            },
                            actionLabel = if (searchQuery.isNotBlank() || selectedCategoryId != null) {
                                "Cancella filtri"
                            } else {
                                "Aggiungi primo articolo"
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(articles, key = { it.uuid }) { article ->
                                ArticleCard(
                                    article = article,
                                    categoryName = viewModel.getCategoryName(article.categoryId),
                                    onClick = { onArticleClick(article.uuid) },
                                    onDeleteClick = { viewModel.deleteArticle(article.uuid) },
                                    cardStyle = displaySettings.articleCardStyle,
                                    showImage = displaySettings.showArticleImages,
                                    showStockIndicator = displaySettings.showStockIndicators,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Cerca articoli, codici...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Clear, "Cancella")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun ActiveFiltersBar(
    categoryName: String?,
    hasSearchQuery: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = buildString {
                append("Filtri: ")
                val filters = mutableListOf<String>()
                if (categoryName != null) filters.add(categoryName)
                if (hasSearchQuery) filters.add("ricerca")
                append(filters.joinToString(", "))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        TextButton(
            onClick = onClearFilters,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Cancella", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SortOrderBar(
    sortOrder: ArticleSortOrder,
    onResetSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDefault = sortOrder == ArticleSortOrder.RECENT_UPDATED_FIRST

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Default.Sort,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Ordine: ${sortOrder.getDisplayName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        if (!isDefault) {
            TextButton(
                onClick = onResetSort,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Reset", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    onAction: () -> Unit,
    actionLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            message,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Errore",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Riprova")
        }
    }
}

@Composable
private fun CategoryFilterMenu(
    expanded: Boolean,
    categories: List<ArticleCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // "Tutte" option (null = no filter)
        DropdownMenuItem(
            text = { Text("Tutte") },
            onClick = {
                onCategorySelected(null)
                onDismiss()
            },
            leadingIcon = if (selectedCategoryId == null) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        // Divider between "Tutte" and categories
        if (categories.isNotEmpty()) {
            HorizontalDivider()
        }

        // Category options
        categories.forEach { category ->
            DropdownMenuItem(
                text = { Text(category.name) },
                onClick = {
                    onCategorySelected(category.uuid)
                    onDismiss()
                },
                leadingIcon = if (selectedCategoryId == category.uuid) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    selectedSort: ArticleSortOrder,
    onSortSelected: (ArticleSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ArticleSortOrder.entries.forEach { sortOption ->
            DropdownMenuItem(
                text = { Text(sortOption.getDisplayName()) },
                onClick = {
                    onSortSelected(sortOption)
                    onDismiss()
                },
                leadingIcon = if (selectedSort == sortOption) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
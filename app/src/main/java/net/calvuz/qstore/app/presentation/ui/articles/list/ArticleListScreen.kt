package net.calvuz.qstore.app.presentation.ui.articles.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.app.domain.model.ArticleCategory
import net.calvuz.qstore.app.presentation.navigation.Screen.DisplaySettings
import net.calvuz.qstore.app.presentation.ui.articles.components.ArticleCard
import kotlin.collections.isNotEmpty

/**
 * Article List Screen
 *
 * Features:
 * - Lista tutti gli articoli
 * - Search bar (cerca anche nei codici esterni)
 * - Filtri per categoria (da database)
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()

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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
                    }

                    // Status menu
                    var showStatusMenu by remember { mutableStateOf(false) }

                    // Menu Button
                    IconButton(onClick = { showStatusMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }

                    // Menu
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Impostazioni") },
                            onClick = {
                                onNavigateToDisplaySettings()
                                showStatusMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                            }
                        )
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

            // Category filters (da database)
            if (categories.isNotEmpty()) {
                CategoryFilters(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onClearFilters = { viewModel.clearFilters() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
private fun CategoryFilters(
    categories: List<ArticleCategory>,
    selectedCategoryId: String?,
    onCategorySelect: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Categorie",
                style = MaterialTheme.typography.labelLarge
            )

            if (selectedCategoryId != null) {
                TextButton(onClick = onClearFilters) {
                    Text("Cancella filtri")
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.uuid }) { category ->
                FilterChip(
                    selected = category.uuid == selectedCategoryId,
                    onClick = {
                        onCategorySelect(
                            if (category.uuid == selectedCategoryId) null else category.uuid
                        )
                    },
                    label = { Text(category.name) },
                    leadingIcon = if (category.uuid == selectedCategoryId) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
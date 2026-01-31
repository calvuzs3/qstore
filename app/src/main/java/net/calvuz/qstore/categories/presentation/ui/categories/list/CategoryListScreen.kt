package net.calvuz.qstore.categories.presentation.ui.categories.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.calvuz.qstore.categories.domain.model.ArticleCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onNavigateBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    viewModel: CategoryListViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Delete confirmation dialog
    var categoryToDelete by remember { mutableStateOf<ArticleCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorie") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCategoryClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, "Aggiungi categoria")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Cerca categorie...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Cancella")
                        }
                    }
                },
                singleLine = true
            )

            when (uiState) {
                is CategoryListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CategoryListUiState.Error -> {
                    ErrorMessage(
                        message = (uiState as CategoryListUiState.Error).message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is CategoryListUiState.Success -> {
                    if (categories.isEmpty()) {
                        EmptyState(
                            message = if (searchQuery.isNotBlank()) {
                                "Nessuna categoria trovata"
                            } else {
                                "Nessuna categoria"
                            },
                            onAction = if (searchQuery.isNotBlank()) {
                                { viewModel.onSearchQueryChange("") }
                            } else {
                                onAddCategoryClick
                            },
                            actionLabel = if (searchQuery.isNotBlank()) {
                                "Cancella ricerca"
                            } else {
                                "Crea prima categoria"
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories, key = { it.category.uuid }) { categoryWithCount ->
                                CategoryCard(
                                    category = categoryWithCount.category,
                                    articleCount = categoryCounts[categoryWithCount.category.uuid] ?: 0,
                                    onClick = { onCategoryClick(categoryWithCount.category.uuid) },
                                    onDeleteClick = { categoryToDelete = categoryWithCount.category }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    categoryToDelete?.let { category ->
        val articleCount = categoryCounts[category.uuid] ?: 0
        
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (articleCount > 0) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Elimina categoria") },
            text = {
                if (articleCount > 0) {
                    Text(
                        "Impossibile eliminare \"${category.name}\": contiene $articleCount articoli.\n\n" +
                        "Sposta o elimina prima gli articoli dalla categoria."
                    )
                } else {
                    Text("Sei sicuro di voler eliminare la categoria \"${category.name}\"?")
                }
            },
            confirmButton = {
                if (articleCount == 0) {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory(category.uuid)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Elimina")
                    }
                } else {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (articleCount == 0) {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("Annulla")
                    }
                }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: ArticleCategory,
    articleCount: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (category.description.isNotBlank()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = when (articleCount) {
                        0 -> "Nessun articolo"
                        1 -> "1 articolo"
                        else -> "$articleCount articoli"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error
                )
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

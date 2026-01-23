package net.calvuz.qstore.presentation.ui.articles.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.model.ArticleCategory
import java.io.File

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
    onArticleClick: (String) -> Unit,
    onAddArticleClick: () -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                                    onDeleteClick = { viewModel.deleteArticle(article.uuid) }
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
private fun ArticleCard(
    article: Article,
    categoryName: String?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Articolo") },
            text = { Text("Sei sicuro di voler eliminare '${article.name}'? Questa azione non può essere annullata.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Foto thumbnail o icona placeholder
            ArticleThumbnail(
                articleId = article.uuid,
                modifier = Modifier.size(56.dp)
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = article.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Mostra il nome della categoria (non l'ID)
                if (categoryName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Mostra codici esterni se presenti
                val codes = listOfNotNull(
                    article.codeOEM.takeIf { it.isNotBlank() }?.let { "OEM: $it" },
                    article.codeERP.takeIf { it.isNotBlank() }?.let { "ERP: $it" }
                )
                if (codes.isNotEmpty()) {
                    Text(
                        text = codes.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Icon(
                    Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = "Dettagli",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArticleThumbnail(
    articleId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Cerca la prima immagine dell'articolo
    val firstImagePath = remember(articleId) {
        val articleDir = File(context.filesDir, "article_images/$articleId")
        if (articleDir.exists() && articleDir.isDirectory) {
            articleDir.listFiles()?.firstOrNull()?.let {
                File(context.filesDir, "article_images/$articleId/${it.name}").absolutePath
            }
        } else {
            null
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (firstImagePath != null) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        if (firstImagePath != null) {
            AsyncImage(
                model = File(firstImagePath),
                contentDescription = "Foto articolo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Warehouse,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
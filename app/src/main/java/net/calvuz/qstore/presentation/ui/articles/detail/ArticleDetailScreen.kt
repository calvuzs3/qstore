package net.calvuz.qstore.presentation.ui.articles.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.model.ArticleCategory
import net.calvuz.qstore.domain.model.ArticleImage
import net.calvuz.qstore.domain.model.Inventory
import net.calvuz.qstore.domain.model.Movement
import net.calvuz.qstore.domain.model.enum.MovementType
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToAddMovement: (String) -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(events) {
        when (val event = events) {
            is ArticleDetailEvent.NavigateBack -> onNavigateBack()
            is ArticleDetailEvent.NavigateToEdit -> {
                state.article?.let { onNavigateToEdit(it.uuid) }
            }
            is ArticleDetailEvent.NavigateToAddMovement -> {
                state.article?.let { onNavigateToAddMovement(it.uuid) }
            }
            is ArticleDetailEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message)
            }
            null -> { /* No event */ }
        }
        viewModel.onEventConsumed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Articolo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onEditClick) {
                        Icon(Icons.Default.Edit, "Modifica")
                    }
                    IconButton(onClick = viewModel::onDeleteClick) {
                        Icon(Icons.Default.Delete, "Elimina")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.article != null) {
                FloatingActionButton(
                    onClick = viewModel::onAddMovementClick
                ) {
                    Icon(Icons.Default.Add, "Registra Movimento")
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.article != null -> {
                ArticleDetailContent(
                    article = state.article!!,
                    category = state.category,
                    inventory = state.inventory,
                    movements = state.movements,
                    images = state.images,
                    onDeleteImage = viewModel::onDeleteImage,
                    modifier = Modifier.padding(padding),
                    onRefresh = viewModel::onRefresh
                )
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = viewModel::onRefresh) {
                            Text("Riprova")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Elimina Articolo") },
            text = {
                Text("Sei sicuro di voler eliminare questo articolo? Verranno eliminati anche tutti i movimenti associati.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) {
                    Text("Annulla")
                }
            }
        )
    }

    // Deleting Progress
    if (state.isDeleting) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ArticleDetailContent(
    article: Article,
    category: ArticleCategory?,
    inventory: Inventory?,
    movements: List<Movement>,
    images: List<ArticleImage>,
    onDeleteImage: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Article Info Card
        item {
            ArticleInfoCard(
                article = article,
                categoryName = category?.name
            )
        }

        // External Codes Card (se presenti)
        if (article.codeOEM.isNotBlank() || article.codeERP.isNotBlank() || article.codeBM.isNotBlank()) {
            item {
                ExternalCodesCard(article)
            }
        }

        // Photo Gallery (se ci sono foto)
        if (images.isNotEmpty()) {
            item {
                PhotoGallerySection(
                    images = images,
                    onDeleteImage = onDeleteImage
                )
            }
        }

        // Inventory Card
        item {
            InventoryCard(
                inventory = inventory,
                unit = article.unitOfMeasure,
                reorderLevel = article.reorderLevel
            )
        }

        // Movements Section
        item {
            Text(
                text = "Storico Movimentazioni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (movements.isEmpty()) {
            item {
                Card {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessuna movimentazione registrata",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(movements) { movement ->
                MovementCard(movement, article.unitOfMeasure)
            }
        }
    }
}

@Composable
private fun PhotoGallerySection(
    images: List<ArticleImage>,
    onDeleteImage: (Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<ArticleImage?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Foto (${images.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(
                    items = images,
                    key = { it.id }
                ) { image ->
                    PhotoGridItem(
                        image = image,
                        onDelete = { showDeleteDialog = image }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { image ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Elimina foto?") },
            text = { Text("Questa azione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteImage(image.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun PhotoGridItem(
    image: ArticleImage,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fullPath = remember(image.imagePath) {
        File(context.filesDir, "article_images/${image.imagePath}").absolutePath
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = File(fullPath),
            contentDescription = "Foto articolo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ArticleInfoCard(
    article: Article,
    categoryName: String?
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = article.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Mostra il nome della categoria (non l'ID)
            if (categoryName != null) {
                InfoRow(
                    icon = Icons.Default.Category,
                    label = "Categoria",
                    value = categoryName
                )
            }

            if (article.description.isNotBlank()) {
                InfoRow(
                    icon = Icons.Default.Description,
                    label = "Descrizione",
                    value = article.description
                )
            }

            InfoRow(
                icon = Icons.Default.Straighten,
                label = "Unità di misura",
                value = article.unitOfMeasure
            )

            if (article.reorderLevel > 0) {
                InfoRow(
                    icon = Icons.Default.Warning,
                    label = "Soglia sotto scorta",
                    value = "${article.reorderLevel} ${article.unitOfMeasure}"
                )
            }

            if (article.notes.isNotBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Default.Notes,
                    label = "Note",
                    value = article.notes
                )
            }
        }
    }
}

@Composable
private fun ExternalCodesCard(article: Article) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Codici Esterni",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (article.codeOEM.isNotBlank()) {
                CodeRow(
                    icon = Icons.Default.Factory,
                    label = "OEM",
                    value = article.codeOEM
                )
            }

            if (article.codeERP.isNotBlank()) {
                CodeRow(
                    icon = Icons.Default.Business,
                    label = "ERP",
                    value = article.codeERP
                )
            }

            if (article.codeBM.isNotBlank()) {
                CodeRow(
                    icon = Icons.Default.Badge,
                    label = "BM",
                    value = article.codeBM
                )
            }
        }
    }
}

@Composable
private fun CodeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InventoryCard(
    inventory: Inventory?,
    unit: String,
    reorderLevel: Double
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                inventory == null -> MaterialTheme.colorScheme.surfaceVariant
                inventory.currentQuantity <= reorderLevel ->
                    MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giacenza Attuale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (inventory != null && inventory.currentQuantity <= reorderLevel) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Sotto scorta",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = "${inventory?.currentQuantity ?: 0.0} $unit",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            if (inventory != null) {
                Text(
                    text = "Ultimo aggiornamento: ${formatTimestamp(inventory.lastMovementAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MovementCard(movement: Movement, unit: String) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (movement.type) {
                            MovementType.IN -> Icons.Default.Add
                            MovementType.OUT -> Icons.Default.Remove
                        },
                        contentDescription = null,
                        tint = when (movement.type) {
                            MovementType.IN -> MaterialTheme.colorScheme.primary
                            MovementType.OUT -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = when (movement.type) {
                            MovementType.IN -> "Carico"
                            MovementType.OUT -> "Scarico"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (movement.notes.isNotBlank()) {
                    Text(
                        text = movement.notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = formatTimestamp(movement.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (movement.type == MovementType.IN) "+" else "-"}${movement.quantity} $unit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (movement.type) {
                    MovementType.IN -> MaterialTheme.colorScheme.primary
                    MovementType.OUT -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return dateTime.format(formatter)
}
package net.calvuz.qstore.app.presentation.ui.movements.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.presentation.ui.common.EmptyState
import net.calvuz.qstore.app.presentation.ui.common.ErrorState
import net.calvuz.qstore.app.presentation.ui.theme.PlexMono
import net.calvuz.qstore.app.presentation.ui.theme.registrationTicks
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementListScreen(
    onNavigateBack: () -> Unit,
    onArticleClick: (String) -> Unit,
    viewModel: MovementListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storico Movimenti") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Filtri per tipo
            FilterChips(
                selectedFilter = state.filterType,
                onFilterChange = viewModel::onFilterTypeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista movimenti
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    ErrorState(
                        message = state.error!!,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                state.filteredMovements.isEmpty() -> {
                    EmptyState(
                        message = if (state.searchQuery.isNotBlank() || state.filterType != MovementFilterType.All) {
                            "Nessun movimento trovato"
                        } else {
                            "Nessun movimento registrato"
                        },
                        icon = Icons.Default.SwapVert,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredMovements) { item ->
                            MovementCard(
                                movementWithArticle = item,
                                onArticleClick = { onArticleClick(item.movement.articleUuid) }
                            )
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
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Cerca per articolo o note...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Cancella")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun FilterChips(
    selectedFilter: MovementFilterType,
    onFilterChange: (MovementFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == MovementFilterType.All,
            onClick = { onFilterChange(MovementFilterType.All) },
            label = { Text("Tutti") },
            leadingIcon = if (selectedFilter == MovementFilterType.All) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )

        FilterChip(
            selected = selectedFilter == MovementFilterType.In,
            onClick = { onFilterChange(MovementFilterType.In) },
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Carichi")
                }
            },
            leadingIcon = if (selectedFilter == MovementFilterType.In) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )

        FilterChip(
            selected = selectedFilter == MovementFilterType.Out,
            onClick = { onFilterChange(MovementFilterType.Out) },
            label = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Scarichi")
                }
            },
            leadingIcon = if (selectedFilter == MovementFilterType.Out) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
    }
}

@Composable
private fun MovementCard(
    movementWithArticle: MovementWithArticle,
    onArticleClick: () -> Unit
) {
    val movement = movementWithArticle.movement
    val article = movementWithArticle.article

    val typeAccent = when (movement.type) {
        MovementType.IN -> MaterialTheme.colorScheme.primary
        MovementType.OUT -> MaterialTheme.colorScheme.error
        MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.tertiary
        MovementType.TRANSFER -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = if (article != null) onArticleClick else { {} },
        modifier = Modifier
            .fillMaxWidth()
            .registrationTicks(color = typeAccent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icona tipo movimento
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = when (movement.type) {
                    MovementType.IN -> MaterialTheme.colorScheme.primaryContainer
                    MovementType.OUT -> MaterialTheme.colorScheme.errorContainer
                    MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.tertiaryContainer
                    MovementType.TRANSFER -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Icon(
                    imageVector = when (movement.type) {
                        MovementType.IN -> Icons.Default.Add
                        MovementType.OUT -> Icons.Default.Remove
                        MovementType.ADJUSTMENT -> Icons.Default.Edit
                        MovementType.TRANSFER -> Icons.AutoMirrored.Filled.CompareArrows
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = when (movement.type) {
                        MovementType.IN -> MaterialTheme.colorScheme.onPrimaryContainer
                        MovementType.OUT -> MaterialTheme.colorScheme.onErrorContainer
                        MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.onTertiaryContainer
                        MovementType.TRANSFER -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }

            // Contenuto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nome articolo
                Text(
                    text = article?.name ?: "Articolo eliminato",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (article == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Tipo movimento
                Text(
                    text = when (movement.type) {
                        MovementType.IN -> "Carico"
                        MovementType.OUT -> "Scarico"
                        MovementType.ADJUSTMENT -> "Rettifica"
                        MovementType.TRANSFER -> "Trasferimento"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = typeAccent
                )

                // Note (se presenti)
                if (movement.notes.isNotBlank()) {
                    Text(
                        text = movement.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Data e ora
                Text(
                    text = formatTimestamp(movement.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = PlexMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantità
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${if (movement.toLocationUuid != null) "+" else "-"}${movement.quantity}",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.Bold,
                    color = typeAccent
                )

                if (article != null) {
                    Text(
                        text = article.unitOfMeasure,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return dateTime.format(formatter)
}
package net.calvuz.qstore.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.model.Movement
import net.calvuz.qstore.domain.model.enum.MovementType

/**
 * Home Screen - Dashboard principale
 *
 * Mostra:
 * - Statistiche magazzino
 * - Articoli sotto scorta
 * - Ultimi movimenti
 * - Azioni rapide
 * - Impostazioni
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToArticles: () -> Unit,
    onNavigateToMovements: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAddArticle: () -> Unit,
    onNavigateToSettings: () -> Unit, // Nuovo parametro
    onArticleClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QuickStore") },
                actions = {
                    // Bottone settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Impostazioni")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddArticle,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, "Aggiungi articolo")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is HomeUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is HomeUiState.Success -> {
                    DashboardContent(
                        stats = state.stats,
                        lowStockArticles = state.lowStockArticles,
                        recentMovements = state.recentMovements,
                        onNavigateToArticles = onNavigateToArticles,
                        onNavigateToMovements = onNavigateToMovements,
                        onNavigateToCamera = onNavigateToCamera,
                        onArticleClick = onArticleClick
                    )
                }
            }
        }
    }
}


@Composable
private fun DashboardContent(
    stats: DashboardStats,
    lowStockArticles: List<Article>,
    recentMovements: List<Movement>,
    onNavigateToArticles: () -> Unit,
    onNavigateToMovements: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onArticleClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Azioni rapide
        item {
            QuickActionsCard(
                onNavigateToArticles = onNavigateToArticles,
                onNavigateToMovements = onNavigateToMovements,
                onNavigateToCamera = onNavigateToCamera
            )
        }

        // Statistiche
        item {
            StatsCard(stats = stats)
        }

        // Articoli sotto scorta
        if (lowStockArticles.isNotEmpty()) {
            item {
                Text(
                    "⚠️ Articoli Sotto Scorta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(lowStockArticles) { article ->
                LowStockArticleCard(
                    article = article,
                    onClick = { onArticleClick(article.uuid) }
                )
            }
        }

        // Ultimi movimenti
        if (recentMovements.isNotEmpty()) {
            item {
                Text(
                    "📋 Ultimi Movimenti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentMovements) { movement ->
                RecentMovementCard(movement = movement)
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onNavigateToArticles: () -> Unit,
    onNavigateToMovements: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Azioni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Warehouse,
                    label = "Articoli",
                    onClick = onNavigateToArticles,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = Icons.Default.SwapVert,
                    label = "Movimenti",
                    onClick = onNavigateToMovements,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Cerca Foto",
                    onClick = onNavigateToCamera,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun StatsCard(stats: DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📊 Statistiche Magazzino",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalArticles.toString(),
                    label = "Articoli Totali",
                    icon = Icons.Default.Category
                )

                StatItem(
                    value = stats.articlesWithStock.toString(),
                    label = "A Magazzino",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    value = stats.articlesOutOfStock.toString(),
                    label = "Esauriti",
                    icon = Icons.Default.Warning,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LowStockArticleCard(
    article: Article,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    article.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Dettagli"
            )
        }
    }
}

@Composable
private fun RecentMovementCard(movement: Movement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (movement.type) {
                    MovementType.IN -> Icons.Default.ArrowDownward
                    MovementType.OUT -> Icons.Default.ArrowUpward
                },
                contentDescription = null,
                tint = when (movement.type) {
                    MovementType.IN -> MaterialTheme.colorScheme.primary
                    MovementType.OUT -> MaterialTheme.colorScheme.error
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (movement.type) {
                        MovementType.IN -> "Carico"
                        MovementType.OUT -> "Scarico"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (movement.notes.isNotBlank()) {
                    Text(
                        movement.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "${if (movement.type == MovementType.IN) "+" else "-"}${movement.quantity}",
                style = MaterialTheme.typography.titleMedium,
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
private fun ErrorContent(
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
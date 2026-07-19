package net.calvuz.qstore.app.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import net.calvuz.qstore.app.presentation.ui.common.QsOutlinedButton as OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.LocationStats
import net.calvuz.qstore.app.domain.model.Movement
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.app.presentation.ui.common.ErrorState
import net.calvuz.qstore.app.presentation.ui.theme.PlexMono
import net.calvuz.qstore.app.presentation.ui.theme.registrationTicks
import net.calvuz.qstore.app.presentation.ui.theme.accentInk
import net.calvuz.qstore.app.presentation.ui.theme.accentInkAlt
import net.calvuz.qstore.settings.domain.model.DisplaySettings

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
    onNavigateToSettings: () -> Unit,
    onArticleClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()

    // HomeViewModel carica i dati con fetch one-shot, non Flow reattivi — senza questo,
    // tornando qui dopo una sync (Impostazioni > Account) o dopo aver registrato un
    // movimento altrove, la dashboard resta ferma allo snapshot di quando la ViewModel è
    // stata creata finché non si preme manualmente il refresh in alto.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QuickStore") },
                actions = {

                    // Refresh Button
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
                                onNavigateToSettings()
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
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is HomeUiState.Success -> {
                    DashboardContent(
                        stats = state.stats,
                        locationStats = state.locationStats,
                        lowStockArticles = state.lowStockArticles,
                        recentMovements = state.recentMovements,
                        recentArticles = state.recentArticles,
                        displaySettings = displaySettings,
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
    locationStats: List<LocationStats>,
    lowStockArticles: List<Article>,
    recentMovements: List<Movement>,
    recentArticles: List<Article>,
    displaySettings: DisplaySettings,
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

        // Statistiche (totali + per magazzino) — sezione opzionale
        if (displaySettings.showDashboardStats) {
            item {
                StatsCard(stats = stats)
            }

            if (locationStats.isNotEmpty()) {
                item {
                    LocationStatsCard(locationStats = locationStats)
                }
            }
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

        // Ultimi movimenti — sezione opzionale
        if (displaySettings.showRecentMovements && recentMovements.isNotEmpty()) {
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

        // Ultimi articoli creati — sezione opzionale
        if (displaySettings.showRecentArticles && recentArticles.isNotEmpty()) {
            item {
                Text(
                    "🆕 Ultimi Articoli Creati",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentArticles) { article ->
                RecentArticleCard(
                    article = article,
                    onClick = { onArticleClick(article.uuid) }
                )
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
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.accentInk)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatsCard(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Statistiche Magazzino",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Esauriti è l'unico che diventa arancio, e solo se > 0 — la severità si legge
            // per intensità dell'unico accento, non per un secondo colore semantico. La mira
            // (riempimento) resta arancio pieno; il testo (inchiostro) segue la regola
            // d'inchiostro — arancio solo su sfondo grafite, altrimenti grafite/onSurface.
            val outOfStock = stats.articlesOutOfStock > 0
            val outOfStockTickColor = if (outOfStock) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val outOfStockTextColor = if (outOfStock) {
                MaterialTheme.colorScheme.accentInk
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            StatItem(
                value = stats.totalArticles.toString(),
                label = "Articoli Totali",
                modifier = Modifier.weight(1f)
            )

            StatItem(
                value = stats.articlesWithStock.toString(),
                label = "A Magazzino",
                modifier = Modifier.weight(1f)
            )

            StatItem(
                value = stats.articlesOutOfStock.toString(),
                label = "Esauriti",
                color = outOfStockTextColor,
                tickColor = outOfStockTickColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    tickColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outline
) {
    Card(
        modifier = modifier.registrationTicks(color = tickColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = PlexMono,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationStatsCard(locationStats: List<LocationStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Articoli per Magazzino",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            locationStats.forEach { location ->
                LocationStatsRow(location = location)
            }
        }
    }
}

@Composable
private fun LocationStatsRow(location: LocationStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warehouse,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                location.locationName,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            "${location.articleCount} art. · ${location.totalQuantity.formatQuantity()}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = PlexMono,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.accentInk
        )
    }
}

private fun Double.formatQuantity(): String {
    return if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        "%.2f".format(this)
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
                    MovementType.ADJUSTMENT -> Icons.Default.Edit
                    MovementType.TRANSFER -> Icons.AutoMirrored.Filled.CompareArrows
                },
                contentDescription = null,
                tint = when (movement.type) {
                    MovementType.IN -> MaterialTheme.colorScheme.accentInk
                    MovementType.OUT -> MaterialTheme.colorScheme.error
                    MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.accentInkAlt
                    MovementType.TRANSFER -> MaterialTheme.colorScheme.secondary
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (movement.type) {
                        MovementType.IN -> "Carico"
                        MovementType.OUT -> "Scarico"
                        MovementType.ADJUSTMENT -> "Rettifica"
                        MovementType.TRANSFER -> "Trasferimento"
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
                "${if (movement.toLocationUuid != null) "+" else "-"}${movement.quantity}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (movement.type) {
                    MovementType.IN -> MaterialTheme.colorScheme.accentInk
                    MovementType.OUT -> MaterialTheme.colorScheme.error
                    MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.accentInkAlt
                    MovementType.TRANSFER -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}

@Composable
private fun RecentArticleCard(
    article: Article,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                Icons.Default.NewReleases,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.accentInk
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    article.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatCreatedAt(article.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Dettagli"
            )
        }
    }
}

private fun formatCreatedAt(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}


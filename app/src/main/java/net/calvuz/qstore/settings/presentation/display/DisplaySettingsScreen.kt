package net.calvuz.qstore.settings.presentation.display

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.getDescription
import net.calvuz.qstore.settings.domain.model.getDisplayName
import net.calvuz.qstore.settings.presentation.components.SettingsErrorCard
import net.calvuz.qstore.settings.presentation.components.SettingsSection
import net.calvuz.qstore.settings.presentation.components.SettingsSuccessCard
import net.calvuz.qstore.settings.presentation.components.SettingsSwitchItem

/**
 * Schermata impostazioni Aspetto & Layout.
 *
 * Permette di configurare:
 * - Stile delle card articoli (Full, Compact, Minimal)
 * - Visibilità indicatori stock
 * - Visibilità immagini articoli
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DisplaySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSettings by viewModel.currentSettings.collectAsStateWithLifecycle()

    // Auto-dismiss messaggi dopo 3 secondi
    LaunchedEffect(uiState.error, uiState.message) {
        if (uiState.error != null || uiState.message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aspetto & Layout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Ripristina")
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Messaggi di stato
            if (uiState.error != null) {
                item {
                    SettingsErrorCard(message = uiState.error!!)
                }
            }

            if (uiState.message != null) {
                item {
                    SettingsSuccessCard(message = uiState.message!!)
                }
            }

            // === Stile Card Articoli ===
            item {
                SettingsSection(
                    title = "Stile Card Articoli",
                    description = "Scegli come visualizzare gli articoli nella lista"
                ) {
                    ArticleCardStyleSelector(
                        selectedStyle = currentSettings.articleCardStyle,
                        onStyleSelected = viewModel::setArticleCardStyle
                    )
                }
            }

            // === Opzioni Visualizzazione ===
            item {
                SettingsSection(
                    title = "Opzioni Visualizzazione"
                ) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Inventory2,
                        title = "Indicatori Stock",
                        subtitle = "Mostra icone colorate per stato giacenza",
                        checked = currentSettings.showStockIndicators,
                        onCheckedChange = viewModel::setShowStockIndicators
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.Image,
                        title = "Immagini Articoli",
                        subtitle = "Mostra thumbnail nelle card",
                        checked = currentSettings.showArticleImages,
                        onCheckedChange = viewModel::setShowArticleImages
                    )
                }
            }

            // === Preview (Futuro) ===
            item {
                SettingsSection(
                    title = "Anteprima",
                    description = "Come apparirà la lista articoli"
                ) {
                    ArticleCardPreview(
                        style = currentSettings.articleCardStyle,
                        showImage = currentSettings.showArticleImages,
                        showStockIndicator = currentSettings.showStockIndicators
                    )
                }
            }
        }
    }
}

/**
 * Selettore stile card con 3 opzioni visuali.
 */
@Composable
private fun ArticleCardStyleSelector(
    selectedStyle: ArticleCardStyle,
    onStyleSelected: (ArticleCardStyle) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ArticleCardStyle.entries.forEach { style ->
            StyleOptionCard(
                style = style,
                isSelected = style == selectedStyle,
                onSelect = { onStyleSelected(style) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Card singola opzione stile.
 */
@Composable
private fun StyleOptionCard(
    style: ArticleCardStyle,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        onClick = onSelect,
        modifier = modifier.border(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icona rappresentativa dello stile
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.toIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Check badge se selezionato
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selezionato",
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = style.getDisplayName(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )

            Text(
                text = style.getDescription(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Preview mockup della card articolo.
 */
@Composable
private fun ArticleCardPreview(
    style: ArticleCardStyle,
    showImage: Boolean,
    showStockIndicator: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail placeholder
            if (showImage) {
                val imageSize = when (style) {
                    ArticleCardStyle.FULL -> 80.dp
                    ArticleCardStyle.COMPACT -> 56.dp
                    ArticleCardStyle.MINIMAL -> 40.dp
                }

                Surface(
                    modifier = Modifier.size(imageSize),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Contenuto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Articolo Esempio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Stock indicator
                    if (showStockIndicator) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }

                if (style != ArticleCardStyle.MINIMAL) {
                    Text(
                        text = "Categoria",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (style == ArticleCardStyle.FULL) {
                    Text(
                        text = "OEM: ABC123 • ERP: 456",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Descrizione articolo di esempio per mostrare come appare in modalità completa...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Extension per ottenere l'icona rappresentativa dello stile.
 */
private fun ArticleCardStyle.toIcon(): ImageVector = when (this) {
    ArticleCardStyle.FULL -> Icons.Default.ViewAgenda
    ArticleCardStyle.COMPACT -> Icons.Default.ViewCompact
    ArticleCardStyle.MINIMAL -> Icons.AutoMirrored.Default.ViewList
}

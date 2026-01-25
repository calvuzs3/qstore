package net.calvuz.qstore.app.presentation.ui.articles.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import java.io.File

/**
 * Card articolo con supporto per diversi stili di visualizzazione.
 *
 * @param article Articolo da visualizzare
 * @param categoryName Nome categoria (opzionale)
 * @param cardStyle Stile di visualizzazione (FULL, COMPACT, MINIMAL)
 * @param showImage Se mostrare l'immagine thumbnail
 * @param showStockIndicator Se mostrare l'indicatore di stock
 * @param stockLevel Livello di stock per l'indicatore (null = non disponibile)
 * @param onClick Callback click sulla card
 * @param onDeleteClick Callback eliminazione
 */
@Composable
fun ArticleCard(
    article: Article,
    categoryName: String?,
    cardStyle: ArticleCardStyle = ArticleCardStyle.COMPACT,
    showImage: Boolean = true,
    showStockIndicator: Boolean = true,
    stockLevel: StockLevel? = null,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Dialog conferma eliminazione
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            articleName = article.name,
            onConfirm = {
                onDeleteClick()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    when (cardStyle) {
        ArticleCardStyle.FULL -> ArticleCardFull(
            article = article,
            categoryName = categoryName,
            showImage = showImage,
            showStockIndicator = showStockIndicator,
            stockLevel = stockLevel,
            onClick = onClick,
            onDeleteClick = { showDeleteDialog = true }
        )

        ArticleCardStyle.COMPACT -> ArticleCardCompact(
            article = article,
            categoryName = categoryName,
            showImage = showImage,
            showStockIndicator = showStockIndicator,
            stockLevel = stockLevel,
            onClick = onClick,
            onDeleteClick = { showDeleteDialog = true }
        )

        ArticleCardStyle.MINIMAL -> ArticleCardMinimal(
            article = article,
            showImage = showImage,
            showStockIndicator = showStockIndicator,
            stockLevel = stockLevel,
            onClick = onClick,
            onDeleteClick = { showDeleteDialog = true }
        )
    }
}

/**
 * Versione FULL: immagine grande (80dp), tutti i dettagli inclusa descrizione.
 */
@Composable
private fun ArticleCardFull(
    article: Article,
    categoryName: String?,
    showImage: Boolean,
    showStockIndicator: Boolean,
    stockLevel: StockLevel?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            // Thumbnail grande
            if (showImage) {
                ArticleThumbnail(
                    articleId = article.uuid,
                    size = 80.dp
                )
            }

            // Contenuto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nome + Stock indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = article.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (showStockIndicator && stockLevel != null) {
                        StockIndicator(level = stockLevel)
                    }
                }

                // Categoria
                categoryName?.let { name ->
                    CategoryBadge(name = name)
                }

                // Codici OEM/ERP
                val codes = buildCodeString(article)
                if (codes.isNotEmpty()) {
                    Text(
                        text = codes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Descrizione (solo in FULL)
                article.description.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDeleteClick) {
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

/**
 * Versione COMPACT: immagine media (56dp), info essenziali.
 */
@Composable
private fun ArticleCardCompact(
    article: Article,
    categoryName: String?,
    showImage: Boolean,
    showStockIndicator: Boolean,
    stockLevel: StockLevel?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            // Thumbnail media
            if (showImage) {
                ArticleThumbnail(
                    articleId = article.uuid,
                    size = 56.dp
                )
            }

            // Contenuto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nome + Stock indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = article.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (showStockIndicator && stockLevel != null) {
                        StockIndicator(level = stockLevel)
                    }
                }

                // Categoria
                categoryName?.let { name ->
                    CategoryBadge(name = name)
                }

                // Codici OEM/ERP
                val codes = buildCodeString(article)
                if (codes.isNotEmpty()) {
                    Text(
                        text = codes,
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
                IconButton(onClick = onDeleteClick) {
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

/**
 * Versione MINIMAL: immagine piccola (40dp), solo nome, lista densa.
 */
@Composable
private fun ArticleCardMinimal(
    article: Article,
    showImage: Boolean,
    showStockIndicator: Boolean,
    stockLevel: StockLevel?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail piccola
            if (showImage) {
                ArticleThumbnail(
                    articleId = article.uuid,
                    size = 40.dp
                )
            }

            // Nome + Stock indicator
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = article.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (showStockIndicator && stockLevel != null) {
                    StockIndicator(level = stockLevel)
                }
            }

            // Solo freccia (delete via long-press o swipe)
            Icon(
                Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = "Dettagli",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// === Componenti condivisi ===

@Composable
private fun ArticleThumbnail(
    articleId: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
        modifier = modifier.size(size),
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warehouse,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.5f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(name: String) {
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
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StockIndicator(level: StockLevel) {
    Surface(
        modifier = Modifier.size(10.dp),
        shape = RoundedCornerShape(5.dp),
        color = level.color
    ) {}
}

@Composable
private fun DeleteConfirmationDialog(
    articleName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elimina Articolo") },
        text = {
            Text("Sei sicuro di voler eliminare '$articleName'? Questa azione non può essere annullata.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun buildCodeString(article: Article): String {
    return listOfNotNull(
        article.codeOEM.takeIf { it.isNotBlank() }?.let { "OEM: $it" },
        article.codeERP.takeIf { it.isNotBlank() }?.let { "ERP: $it" }
    ).joinToString(" • ")
}

/**
 * Livello di stock per l'indicatore colorato.
 */
enum class StockLevel {
    AVAILABLE,    // Verde - disponibile
    LOW,          // Giallo - sotto soglia minima
    OUT_OF_STOCK; // Rosso - esaurito

    val color: androidx.compose.ui.graphics.Color
        @Composable
        get() = when (this) {
            AVAILABLE -> MaterialTheme.colorScheme.primary
            LOW -> MaterialTheme.colorScheme.tertiary
            OUT_OF_STOCK -> MaterialTheme.colorScheme.error
        }
}

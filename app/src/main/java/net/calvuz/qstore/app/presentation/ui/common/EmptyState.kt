package net.calvuz.qstore.app.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.calvuz.qstore.app.presentation.ui.theme.Spacing

/**
 * Stato vuoto condiviso per liste (nessun elemento / nessun risultato). Il pulsante
 * d'azione compare solo se sia [onAction] che [actionLabel] sono forniti.
 */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            message,
            style = MaterialTheme.typography.titleLarge
        )

        if (onAction != null && actionLabel != null) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

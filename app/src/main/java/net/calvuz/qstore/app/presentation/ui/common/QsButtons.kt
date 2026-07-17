package net.calvuz.qstore.app.presentation.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * `TextButton`/`OutlinedButton` con inchiostro grafite di default invece di
 * `colorScheme.primary` (arancio) — quest'ultimo è il default M3 per questi due componenti
 * "senza riempimento pieno", ma violerebbe la regola d'inchiostro condivisa con QReport (vedi
 * Theme.kt e ../../QuickReport/design/design-system.md): mai arancio come testo su sfondo
 * chiaro. Stessa identica firma di M3, così un import alias
 * (`import ...QsTextButton as TextButton`) basta a correggere ogni chiamata esistente senza
 * toccarne il corpo.
 */
@Composable
fun QsTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = M3TextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun QsOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = M3OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content
)

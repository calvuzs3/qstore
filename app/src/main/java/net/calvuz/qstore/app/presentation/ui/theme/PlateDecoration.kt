package net.calvuz.qstore.app.presentation.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Motivo ricorrente "mire d'angolo" di Opzione 3 — Arancio e Technical Design
 * (vedi /design/mockup-orange-technical.html, classe `.plate`): due piccole "L" agli angoli
 * opposti, come i segni di registrazione di un disegno tecnico. Puramente decorativo, non
 * intercetta input — va applicato a card/riquadri che mostrano dati (giacenza, statistiche),
 * non a stati vuoti/di errore a schermo intero.
 */
fun Modifier.registrationTicks(
    color: Color,
    tickSize: Dp = 9.dp,
    strokeWidth: Dp = 2.dp,
    alpha: Float = 0.5f
): Modifier = this.drawWithContent {
    drawContent()

    val tickPx = tickSize.toPx()
    val strokePx = strokeWidth.toPx()
    val tintedColor = color.copy(alpha = alpha)

    // Mira in alto a sinistra
    drawLine(tintedColor, Offset(0f, strokePx / 2), Offset(tickPx, strokePx / 2), strokePx)
    drawLine(tintedColor, Offset(strokePx / 2, 0f), Offset(strokePx / 2, tickPx), strokePx)

    // Mira in basso a destra
    drawLine(
        tintedColor,
        Offset(size.width, size.height - strokePx / 2),
        Offset(size.width - tickPx, size.height - strokePx / 2),
        strokePx
    )
    drawLine(
        tintedColor,
        Offset(size.width - strokePx / 2, size.height),
        Offset(size.width - strokePx / 2, size.height - tickPx),
        strokePx
    )
}

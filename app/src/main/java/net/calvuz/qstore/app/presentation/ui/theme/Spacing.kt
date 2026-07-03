package net.calvuz.qstore.app.presentation.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Token di spacing centralizzati, ricavati dai valori già in uso nell'app
 * (4/8/12/16/24/32dp). Non copre ogni singolo caso d'uso esistente — pensato per i
 * componenti condivisi in `presentation/ui/common/` e per il nuovo codice.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

package net.calvuz.qstore.settings.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import net.calvuz.qstore.settings.presentation.display.DisplaySettingsScreen

/**
 * Routes per la navigazione delle impostazioni.
 */
object SettingsRoutes {
    const val ROOT = "settings"
    const val MAIN = "settings/main"
    const val DISPLAY = "settings/display"
    const val RECOGNITION = "settings/recognition"
    const val DATA = "settings/data"
    const val ABOUT = "settings/about"
}

/**
 * Estensione per navigare alle varie schermate settings.
 */
fun NavController.navigateToSettings() {
    navigate(SettingsRoutes.MAIN) {
        launchSingleTop = true
    }
}

fun NavController.navigateToDisplaySettings() {
    navigate(SettingsRoutes.DISPLAY) {
        launchSingleTop = true
    }
}

fun NavController.navigateToRecognitionSettings() {
    navigate(SettingsRoutes.RECOGNITION) {
        launchSingleTop = true
    }
}

/**
 * Definisce il NavGraph per le impostazioni.
 *
 * @param navController Controller per la navigazione
 * @param onNavigateBack Callback per tornare indietro dalla root settings
 * @param recognitionSettingsContent Composable per la schermata riconoscimento
 *        (passato dall'esterno per evitare dipendenze circolari)
 */
fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
    onNavigateBack: () -> Unit,
    recognitionSettingsContent: @Composable (onBack: () -> Unit) -> Unit
) {
    navigation(
        startDestination = SettingsRoutes.MAIN,
        route = SettingsRoutes.ROOT
    ) {
        // Menu principale
        composable(SettingsRoutes.MAIN) {
            SettingsScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToDisplay = {
                    navController.navigateToDisplaySettings()
                },
                onNavigateToRecognition = {
                    navController.navigateToRecognitionSettings()
                },
                onNavigateToData = {
                    // TODO: Implementare navigazione a Data settings
                },
                onNavigateToAbout = {
                    // TODO: Implementare navigazione a About
                }
            )
        }

        // Impostazioni Display
        composable(SettingsRoutes.DISPLAY) {
            DisplaySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Impostazioni Riconoscimento (content passato dall'esterno)
        composable(SettingsRoutes.RECOGNITION) {
            recognitionSettingsContent { navController.popBackStack() }
        }

        // TODO: Aggiungere altre routes quando implementate
        // composable(SettingsRoutes.DATA) { ... }
        // composable(SettingsRoutes.ABOUT) { ... }
    }
}

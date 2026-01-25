package net.calvuz.qstore.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qstore.settings.presentation.components.SettingsDivider
import net.calvuz.qstore.settings.presentation.components.SettingsNavigationItem
import net.calvuz.qstore.settings.presentation.components.SettingsSection

/**
 * Schermata principale delle impostazioni.
 *
 * Presenta un menu ad albero che naviga alle varie sotto-categorie:
 * - Aspetto & Layout
 * - Riconoscimento Immagini
 * - Dati & Backup (futuro)
 * - Informazioni App (futuro)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDisplay: () -> Unit,
    onNavigateToRecognition: () -> Unit,
    onNavigateToAbout: (() -> Unit),
    onNavigateToData: (() -> Unit)? = null  // Futuro
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
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
            // === Sezione Interfaccia ===
            item {
                SettingsSection(
                    title = "Interfaccia",
                    description = "Personalizza l'aspetto dell'applicazione"
                ) {
                    SettingsNavigationItem(
                        icon = Icons.Default.Palette,
                        title = "Aspetto & Layout",
                        subtitle = "Stile card, visualizzazione lista",
                        onClick = onNavigateToDisplay
                    )
                }
            }

            // === Sezione Funzionalità ===
            item {
                SettingsSection(
                    title = "Funzionalità",
                    description = "Configura il comportamento dell'app"
                ) {
                    SettingsNavigationItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Riconoscimento Immagini",
                        subtitle = "Parametri matching, soglie, preset",
                        onClick = onNavigateToRecognition
                    )
                }
            }

            // === Sezione Dati (Futuro) ===
            item {
                SettingsSection(
                    title = "Dati",
                    description = "Gestione dati e backup"
                ) {
                    SettingsNavigationItem(
                        icon = Icons.Default.Backup,
                        title = "Backup & Ripristino",
                        subtitle = "Esporta e importa i tuoi dati",
                        onClick = onNavigateToData ?: {}
                    )
                }
            }

            // === Sezione Informazioni (Futuro) ===
            item {
                SettingsSection(
                    title = "Altro"
                ) {
                    SettingsNavigationItem(
                        icon = Icons.Default.Info,
                        title = "Informazioni App",
                        subtitle = "Versione, licenze, crediti",
                        onClick = onNavigateToAbout ?: {}
                    )
                }
            }
        }
    }
}

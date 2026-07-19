package net.calvuz.qstore.settings.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qstore.settings.presentation.components.SettingsSection

/**
 * Elenco licenze delle librerie di terze parti usate in QuickStore — prima il pulsante
 * "Licenze Open Source" non faceva letteralmente nulla (`onNavigateToLicenses` non era mai
 * passato da AppNavigation, quindi `?.invoke()` era un no-op silenzioso).
 *
 * Elenco statico, non generato automaticamente: coerente con lo stack di dipendenze in
 * `gradle/libs.versions.toml` al momento della stesura. Va aggiornato a mano se cambiano le
 * librerie principali — accettabile per uno stack applicativo che non cambia spesso, evita di
 * introdurre il plugin Google `play-services-oss-licenses` (dipendenza Play Services in più)
 * solo per questa schermata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licenze Open Source") },
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
            item {
                Text(
                    "QuickStore è costruito su queste librerie open source.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsSection(title = "Apache License 2.0") {
                    LicenseEntry("AndroidX (Core, AppCompat, Activity, Lifecycle, Navigation, WorkManager, DataStore, Security Crypto, ExifInterface, ConstraintLayout)", "Google")
                    LicenseEntry("Jetpack Compose + Material 3", "Google")
                    LicenseEntry("Room", "Google")
                    LicenseEntry("CameraX", "Google")
                    LicenseEntry("Accompanist Permissions", "Google")
                    LicenseEntry("Hilt / Dagger", "Google")
                    LicenseEntry("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains")
                    LicenseEntry("Ktor Client", "JetBrains")
                    LicenseEntry("Timber", "Jake Wharton")
                    LicenseEntry("Coil", "Coil Contributors")
                    LicenseEntry("Jackson Core", "FasterXML")
                    LicenseEntry("uuid", "Benjamin Asher")
                    LicenseEntry("OpenCV (Android AAR)", "OpenCV.org / QuickBird Studios", isLast = true)
                }
            }

            item {
                SettingsSection(
                    title = "SIL Open Font License 1.1",
                    description = "Font bundlati come pesi statici"
                ) {
                    LicenseEntry("Inter", "The Inter Project Authors")
                    LicenseEntry("Archivo", "The Archivo Project Authors")
                    LicenseEntry("JetBrains Mono", "The JetBrains Mono Project Authors", isLast = true)
                }
            }
        }
    }
}

@Composable
private fun LicenseEntry(
    name: String,
    author: String,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

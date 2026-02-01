package net.calvuz.qstore.settings.presentation.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Web
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.calvuz.qstore.R
import net.calvuz.qstore.settings.presentation.components.SettingsNavigationItem
import net.calvuz.qstore.settings.presentation.components.SettingsSection
import androidx.core.net.toUri

/**
 * Schermata Informazioni App.
 *
 * Mostra:
 * - Logo e nome app
 * - Versione (code e name)
 * - Sviluppatore
 * - Link utili (privacy policy, licenze, contatti)
 * - Info tecniche
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: (() -> Unit)? = null,
    onNavigateToPrivacyPolicy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appInfo = remember { getAppInfo(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informazioni") },
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
            // === Header App ===
            item {
                AppHeaderCard(appInfo = appInfo)
            }

            // === Versione e Build ===
            item {
                SettingsSection(title = "Versione") {
                    InfoRow(
                        icon = Icons.Default.Info,
                        label = "Versione",
                        value = appInfo.versionName
                    )

                    InfoRow(
                        icon = Icons.Default.Code,
                        label = "Build",
                        value = appInfo.versionCode.toString()
                    )

                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Package",
                        value = appInfo.packageName
                    )
                }
            }

            // === Sviluppatore ===
            item {
                SettingsSection(title = "Sviluppatore") {
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Sviluppato da",
                        value = "calvuzs3"
                    )

                    SettingsNavigationItem(
                        icon = Icons.Default.Email,
                        title = "Contattaci",
                        subtitle = "calvuzs3@gmail.com",
                        onClick = {
                            openEmail(context, "calvuzs3@gmail.com", "QStore Feedback")
                        }
                    )

                    SettingsNavigationItem(
                        icon = Icons.Default.Web,
                        title = "Sito Web",
                        subtitle = "www.calvuz.net",
                        onClick = {
                            openUrl(context, "https://www.calvuz.net")
                        }
                    )
                }
            }

            // === Legale ===
            item {
                SettingsSection(title = "Legale") {
                    SettingsNavigationItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "Come gestiamo i tuoi dati",
                        onClick = {
                            onNavigateToPrivacyPolicy?.invoke()
                                ?: openUrl(context, "https://www.calvuz.net/qstore/privacy")
                        }
                    )

                    SettingsNavigationItem(
                        icon = Icons.Default.Gavel,
                        title = "Licenze Open Source",
                        subtitle = "Librerie di terze parti utilizzate",
                        onClick = {
                            onNavigateToLicenses?.invoke()
                        }
                    )
                }
            }

            // === Info Tecniche ===
            item {
                SettingsSection(title = "Informazioni Tecniche") {
                    InfoRow(
                        label = "Android SDK",
                        value = "API ${Build.VERSION.SDK_INT} (${getAndroidVersionName()})"
                    )

                    InfoRow(
                        label = "Dispositivo",
                        value = "${Build.MANUFACTURER} ${Build.MODEL}"
                    )

                    InfoRow(
                        label = "Architettura",
                        value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
                    )
                }
            }

            // === Footer ===
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "© 2025-2026 CalvuzS3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Realizzato con ❤️ in Italia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Card header con logo e nome app.
 */
@Composable
private fun AppHeaderCard(appInfo: AppInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Logo placeholder (sostituire con logo reale)
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primary
            ) {
                // Se hai un logo drawable:
                 Image(
                     painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                     contentDescription = "Logo QStore",
                     modifier = Modifier
                         .padding(4.dp)
                 )

                // Placeholder con icona
//                Icon(
//                    imageVector = Icons.Default.Code,
//                    contentDescription = "Logo QStore",
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp),
//                    tint = MaterialTheme.colorScheme.onPrimary
//                )
            }

            Text(
                text = "QStore",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Gestione Magazzino Intelligente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "v${appInfo.versionName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Riga singola con label e valore.
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// === Utility ===

/**
 * Info app estratte dal PackageManager.
 */
data class AppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long
)

/**
 * Ottiene le info dell'app dal PackageManager.
 */
private fun getAppInfo(context: Context): AppInfo {
    return try {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )

        val versionCode =
            packageInfo.longVersionCode

        AppInfo(
            packageName = context.packageName,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = versionCode
        )
    } catch (_: PackageManager.NameNotFoundException) {
        // Fallback con valori hardcoded
        AppInfo(
            packageName = "net.calvuz.qstore",
            versionName = "1.2.5",
            versionCode = 2
        )
    }
}

/**
 * Ottiene il nome commerciale della versione Android.
 */
private fun getAndroidVersionName(): String {
    return when (Build.VERSION.SDK_INT) {
        21, 22 -> "Lollipop"
        23 -> "Marshmallow"
        24, 25 -> "Nougat"
        26, 27 -> "Oreo"
        28 -> "Pie"
        29 -> "10"
        30 -> "11"
        31, 32 -> "12"
        33 -> "13"
        34 -> "14"
        35 -> "15"
        else -> "Android ${Build.VERSION.SDK_INT}"
    }
}

/**
 * Apre il client email.
 */
private fun openEmail(context: Context, email: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

/**
 * Apre un URL nel browser.
 */
private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

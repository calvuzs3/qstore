package net.calvuz.qstore.settings.presentation.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.settings.presentation.components.SettingsErrorCard
import net.calvuz.qstore.settings.presentation.components.SettingsSection
import net.calvuz.qstore.settings.presentation.components.SettingsSuccessCard

/**
 * Configurazione dell'indirizzo di quickstore-server, usato dal modulo auth/sync.
 * Il server oggi gira su un IP privato di una VM, non un dominio pubblico fisso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ServerSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSettings by viewModel.currentSettings.collectAsStateWithLifecycle()

    var baseUrl by remember(currentSettings.baseUrl) { mutableStateOf(currentSettings.baseUrl) }

    LaunchedEffect(uiState.error, uiState.message) {
        if (uiState.error != null || uiState.message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server di sincronizzazione") },
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
            if (uiState.error != null) {
                item { SettingsErrorCard(message = uiState.error!!) }
            }
            if (uiState.message != null) {
                item { SettingsSuccessCard(message = uiState.message!!) }
            }

            item {
                SettingsSection(
                    title = "Indirizzo server",
                    description = "Usato per login e sincronizzazione (es. https://machine.domain.tld)"
                ) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("URL server") },
                        placeholder = { Text("https://machine.domain.tld") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.save(baseUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salva")
                }
            }
        }
    }
}

package net.calvuz.qstore.auth.presentation.login

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.BuildConfig
import net.calvuz.qstore.auth.domain.model.OrganizationChoice

/**
 * Login opzionale per attivare la sincronizzazione multi-device — QuickStore resta
 * completamente utilizzabile offline senza mai passare da qui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Min SDK di questo progetto è già 33 (Android 13): il permesso runtime per le
    // notifiche serve sempre, nessun fallback per versioni precedenti. Senza questo, le
    // notifiche di avanzamento/esito del trasferimento foto in background (ImageTransferWorker)
    // non compaiono affatto, senza alcun errore visibile.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(uiState is LoginUiState.AlreadyLoggedIn) {
        if (uiState is LoginUiState.AlreadyLoggedIn) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.LoginForm -> {
                state.error?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                }
                state.info?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
                }
            }
            is LoginUiState.OrgSelection -> state.error?.let {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            }
            is LoginUiState.AlreadyLoggedIn -> {
                // Resta su questa schermata dopo il login (niente navigazione automatica):
                // i pulsanti Sincronizza/Disconnetti devono comparire subito, non solo
                // tornando indietro e rientrando in Account.
                if (state.justLoggedIn) {
                    snackbarHostState.showSnackbar("Login riuscito", duration = SnackbarDuration.Short)
                    viewModel.clearJustLoggedIn()
                }
                state.syncMessage?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                }
                state.reconcileMessage?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
                }
                state.purgeMessage?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
                }
                state.switchMessage?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Accedi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is LoginUiState.LoginForm -> LoginFormContent(
                state = state,
                paddingValues = paddingValues,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onSubmit = viewModel::submitLogin
            )
            is LoginUiState.OrgSelection -> OrgSelectionContent(
                state = state,
                paddingValues = paddingValues,
                onSelect = viewModel::selectOrganization
            )
            is LoginUiState.AlreadyLoggedIn -> AlreadyLoggedInContent(
                state = state,
                paddingValues = paddingValues,
                onLogout = viewModel::logout,
                onSyncNow = viewModel::syncNow,
                onAllowMeteredNetworkChange = viewModel::setAllowMeteredNetwork,
                onReconcileInventoryMovements = viewModel::reconcileInventoryMovements,
                onPurgeDeletedData = viewModel::purgeDeletedData,
                onSwitchOrganization = viewModel::switchOrganization
            )
        }
    }
}

@Composable
private fun LoginFormContent(
    state: LoginUiState.LoginForm,
    paddingValues: PaddingValues,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        Button(
            onClick = onSubmit,
            enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Accedi")
            }
        }
    }
}

@Composable
private fun AlreadyLoggedInContent(
    state: LoginUiState.AlreadyLoggedIn,
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    onSyncNow: () -> Unit,
    onAllowMeteredNetworkChange: (Boolean) -> Unit,
    onReconcileInventoryMovements: () -> Unit,
    onPurgeDeletedData: () -> Unit,
    onSwitchOrganization: () -> Unit
) {
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }
    var showSwitchOrgConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Connesso a", style = MaterialTheme.typography.labelMedium)
                Text(text = state.session.orgName, style = MaterialTheme.typography.titleMedium)
                Text(text = state.session.roleCode, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Questo device ha già dati sincronizzati con un'ALTRA organizzazione (vedi
        // GetBoundOrganizationUseCase) — sync bloccata finché l'utente non decide
        // esplicitamente di cambiare organizzazione, cancellando i dati locali.
        if (state.orgMismatch != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dati di un'altra organizzazione su questo device",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Questo device contiene dati sincronizzati con '${state.orgMismatch.orgName}', " +
                            "ma hai effettuato l'accesso come '${state.session.orgName}'. Per evitare di mescolare " +
                            "i dati delle due organizzazioni, la sincronizzazione resta disattivata finché non " +
                            "cambi organizzazione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { showSwitchOrgConfirmDialog = true },
                        enabled = !state.isSwitchingOrganization,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSwitchingOrganization) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Cambia organizzazione")
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Carica/scarica foto anche su dati mobili", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Se disattivo, il trasferimento foto parte solo su wifi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.allowMeteredNetwork, onCheckedChange = onAllowMeteredNetworkChange)
            }
        }

        Button(
            onClick = onSyncNow,
            enabled = !state.isSyncing && !state.isLoggingOut && state.orgMismatch == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Sincronizza ora")
            }
        }

        Button(
            onClick = { showPurgeConfirmDialog = true },
            enabled = !state.isSyncing && !state.isLoggingOut && !state.isPurging,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isPurging) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Pulizia dati cancellati")
            }
        }

        Button(
            onClick = onLogout,
            enabled = !state.isLoggingOut && !state.isSyncing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoggingOut) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Disconnetti")
            }
        }

        if (showPurgeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showPurgeConfirmDialog = false },
                title = { Text("Pulizia dati cancellati") },
                text = {
                    Text(
                        "Elimina definitivamente dal device articoli, foto e categorie " +
                            "cancellati da più di 90 giorni e già sincronizzati. Non riguarda " +
                            "gli elementi cancellati di recente o non ancora sincronizzati. " +
                            "Operazione irreversibile."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPurgeConfirmDialog = false
                            onPurgeDeletedData()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Elimina")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPurgeConfirmDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }

        if (showSwitchOrgConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showSwitchOrgConfirmDialog = false },
                title = { Text("Cambia organizzazione") },
                text = {
                    Text(
                        "Cancella per sempre TUTTI i dati locali di questo device (articoli, " +
                            "categorie, magazzini, movimenti, foto) per liberarlo dall'organizzazione " +
                            "'${state.orgMismatch?.orgName}' e permettergli di usare " +
                            "'${state.session.orgName}'. Verrà creato automaticamente un backup di " +
                            "sicurezza prima di procedere, ma l'operazione resta irreversibile: " +
                            "dopo, dovrai accedere di nuovo."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSwitchOrgConfirmDialog = false
                            onSwitchOrganization()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancella e cambia")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSwitchOrgConfirmDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }

        // Solo debug: tool di manutenzione una tantum per riparare gli articoli creati
        // prima del fix di AddArticleUseCase (giacenza iniziale mai propagata dal sync
        // perché scritta direttamente in inventory, senza movimento). Non serve in release.
        if (BuildConfig.DEBUG) {
            Button(
                onClick = onReconcileInventoryMovements,
                enabled = !state.isReconciling && !state.isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isReconciling) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("[DEBUG] Ripara movimenti mancanti")
                }
            }
        }
    }
}

@Composable
private fun OrgSelectionContent(
    state: LoginUiState.OrgSelection,
    paddingValues: PaddingValues,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Scegli l'organizzazione",
            style = MaterialTheme.typography.titleMedium
        )

        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.organizations) { org: OrganizationChoice ->
                Card(
                    onClick = { if (!state.isLoading) onSelect(org.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = org.name, style = MaterialTheme.typography.titleSmall)
                        Text(text = org.roleCode, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

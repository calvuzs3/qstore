package net.calvuz.qstore.app.presentation.ui.locations.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import net.calvuz.qstore.app.presentation.ui.common.QsTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.app.presentation.ui.common.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationEditViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()

    val isEditMode = viewModel.isEditMode

    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    LaunchedEffect(uiState) {
        if (uiState is LocationEditUiState.Saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Modifica magazzino" else "Nuovo magazzino")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, "Chiudi")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = isFormValid && uiState !is LocationEditUiState.Saving
                    ) {
                        if (uiState is LocationEditUiState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Salva")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is LocationEditUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is LocationEditUiState.Error -> {
                ErrorState(
                    message = (uiState as LocationEditUiState.Error).message,
                    onRetry = { viewModel.resetError() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome *") },
                        placeholder = { Text("Es: Sede, Furgone Mario...") },
                        leadingIcon = {
                            Icon(Icons.Default.Warehouse, contentDescription = null)
                        },
                        isError = nameError != null,
                        supportingText = nameError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.onNotesChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note") },
                        placeholder = { Text("Note aggiuntive") },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        minLines = 2,
                        maxLines = 4
                    )

                    Text(
                        text = "* Campo obbligatorio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null)
            },
            title = { Text("Modifiche non salvate") },
            text = { Text("Vuoi uscire senza salvare le modifiche?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Esci")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continua a modificare")
                }
            }
        )
    }
}


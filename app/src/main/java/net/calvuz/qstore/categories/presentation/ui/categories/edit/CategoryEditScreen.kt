package net.calvuz.qstore.categories.presentation.ui.categories.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()

    val isEditMode = viewModel.isEditMode

    // Discard changes dialog
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Handle back with unsaved changes
    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    // Navigate back on save success
    LaunchedEffect(uiState) {
        if (uiState is CategoryEditUiState.Saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isEditMode) "Modifica categoria" else "Nuova categoria") 
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
                    // Save button
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = isFormValid && uiState !is CategoryEditUiState.Saving
                    ) {
                        if (uiState is CategoryEditUiState.Saving) {
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
            is CategoryEditUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CategoryEditUiState.Error -> {
                ErrorContent(
                    message = (uiState as CategoryEditUiState.Error).message,
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
                    // Name field (required)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome *") },
                        placeholder = { Text("Es: Elettronica, Ferramenta...") },
                        leadingIcon = {
                            Icon(Icons.Default.Category, contentDescription = null)
                        },
                        isError = nameError != null,
                        supportingText = nameError?.let { 
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true
                    )

                    // Description field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.onDescriptionChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descrizione") },
                        placeholder = { Text("Descrizione opzionale") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        minLines = 2,
                        maxLines = 4
                    )

                    // Notes field
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

                    // Info text
                    Text(
                        text = "* Campo obbligatorio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Discard changes dialog
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

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Errore",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Riprova")
        }
    }
}

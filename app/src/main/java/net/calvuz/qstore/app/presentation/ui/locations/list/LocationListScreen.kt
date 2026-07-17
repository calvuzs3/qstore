package net.calvuz.qstore.app.presentation.ui.locations.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import net.calvuz.qstore.app.presentation.ui.theme.accentInk
import net.calvuz.qstore.app.presentation.ui.common.QsTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.presentation.ui.common.EmptyState
import net.calvuz.qstore.app.presentation.ui.common.ErrorState
import net.calvuz.qstore.app.presentation.ui.common.ListItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    onNavigateBack: () -> Unit,
    onLocationClick: (String) -> Unit,
    onAddLocationClick: () -> Unit,
    viewModel: LocationListViewModel = hiltViewModel()
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val deleteBlockReasons by viewModel.deleteBlockReasons.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var locationToDelete by remember { mutableStateOf<Location?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Magazzini") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLocationClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, "Aggiungi magazzino")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Cerca magazzini...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Cancella")
                        }
                    }
                },
                singleLine = true
            )

            when (uiState) {
                is LocationListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is LocationListUiState.Error -> {
                    ErrorState(
                        message = (uiState as LocationListUiState.Error).message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is LocationListUiState.Success -> {
                    if (locations.isEmpty()) {
                        EmptyState(
                            message = if (searchQuery.isNotBlank()) {
                                "Nessun magazzino trovato"
                            } else {
                                "Nessun magazzino"
                            },
                            icon = Icons.Default.Warehouse,
                            onAction = if (searchQuery.isNotBlank()) {
                                { viewModel.onSearchQueryChange("") }
                            } else {
                                onAddLocationClick
                            },
                            actionLabel = if (searchQuery.isNotBlank()) {
                                "Cancella ricerca"
                            } else {
                                "Crea primo magazzino"
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(locations, key = { it.uuid }) { location ->
                                ListItemCard(
                                    icon = Icons.Default.Warehouse,
                                    title = location.name,
                                    subtitle = location.notes.takeIf { it.isNotBlank() },
                                    onClick = { onLocationClick(location.uuid) },
                                    onDeleteClick = { locationToDelete = location }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    locationToDelete?.let { location ->
        val blockReason = deleteBlockReasons[location.uuid]

        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (blockReason != null)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.accentInk
                )
            },
            title = { Text("Elimina magazzino") },
            text = {
                if (blockReason != null) {
                    Text(
                        "Impossibile eliminare \"${location.name}\": $blockReason.\n\n" +
                            "Sposta prima la giacenza o crea un altro magazzino."
                    )
                } else {
                    Text("Sei sicuro di voler eliminare il magazzino \"${location.name}\"?")
                }
            },
            confirmButton = {
                if (blockReason == null) {
                    TextButton(
                        onClick = {
                            viewModel.deleteLocation(location.uuid)
                            locationToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Elimina")
                    }
                } else {
                    TextButton(onClick = { locationToDelete = null }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (blockReason == null) {
                    TextButton(onClick = { locationToDelete = null }) {
                        Text("Annulla")
                    }
                }
            }
        )
    }
}


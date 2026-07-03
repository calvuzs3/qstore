package net.calvuz.qstore.app.presentation.ui.movements.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.model.enum.MovementType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovementScreen(
    articleId: String,
    onNavigateBack: () -> Unit,
    viewModel: AddMovementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    LaunchedEffect(events) {
        when (val event = events) {
            is AddMovementEvent.NavigateBack -> onNavigateBack()
            is AddMovementEvent.ShowError -> {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
            is AddMovementEvent.ShowSuccess -> {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
            null -> { /* No event */ }
        }
        viewModel.onEventConsumed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Registra Movimento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.article != null -> {
                AddMovementContent(
                    state = state,
                    onTypeChange = viewModel::onTypeChange,
                    onQuantityChange = viewModel::onQuantityChange,
                    onNotesChange = viewModel::onNotesChange,
                    onFromLocationChange = viewModel::onFromLocationChange,
                    onToLocationChange = viewModel::onToLocationChange,
                    onSaveClick = viewModel::onSaveClick,
                    modifier = Modifier.padding(padding)
                )
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.error ?: "Errore sconosciuto",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMovementContent(
    state: AddMovementState,
    onTypeChange: (MovementType) -> Unit,
    onQuantityChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onFromLocationChange: (String) -> Unit,
    onToLocationChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val article = state.article ?: return
    val inventory = state.inventory

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Article Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = article.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (article.categoryId.isNotBlank()) {
                    Text(
                        text = article.categoryId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )

                val relevantLocationUuid = when (state.type) {
                    MovementType.OUT, MovementType.TRANSFER -> state.fromLocationUuid
                    MovementType.IN -> state.toLocationUuid
                    MovementType.ADJUSTMENT -> null
                }
                val relevantLocationName = state.locations.find { it.uuid == relevantLocationUuid }?.name
                val giacenzaLabel = if (state.locations.size >= 2 && relevantLocationName != null) {
                    "Giacenza in \"$relevantLocationName\""
                } else {
                    "Giacenza Attuale"
                }
                val giacenzaValue = when (state.type) {
                    MovementType.OUT, MovementType.TRANSFER -> state.fromQuantity
                    MovementType.IN -> state.toQuantity
                    MovementType.ADJUSTMENT -> inventory?.currentQuantity ?: 0.0
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = giacenzaLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$giacenzaValue ${article.unitOfMeasure}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Movement Type Selection
        Text(
            text = "Tipo Movimento",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Carico (IN)
            Card(
                onClick = { onTypeChange(MovementType.IN) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.type == MovementType.IN) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (state.type == MovementType.IN) {
                    CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    null
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (state.type == MovementType.IN) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Carico",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.type == MovementType.IN) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Entrata merce",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.type == MovementType.IN) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            // Scarico (OUT)
            Card(
                onClick = { onTypeChange(MovementType.OUT) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.type == MovementType.OUT) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (state.type == MovementType.OUT) {
                    CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                } else {
                    null
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (state.type == MovementType.OUT) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Scarico",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.type == MovementType.OUT) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Uscita merce",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.type == MovementType.OUT) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            // Trasferimento (TRANSFER)
            Card(
                onClick = { onTypeChange(MovementType.TRANSFER) },
                enabled = state.locations.size >= 2,
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.type == MovementType.TRANSFER) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (state.type == MovementType.TRANSFER) {
                    CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary)
                    )
                } else {
                    null
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (state.type == MovementType.TRANSFER) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Trasferimento",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.type == MovementType.TRANSFER) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (state.locations.size >= 2) "Tra magazzini" else "Serve un secondo magazzino",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.type == MovementType.TRANSFER) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }

        // Magazzini (solo se ne esiste più di uno — altrimenti si usa silenziosamente l'unico)
        if (state.locations.size >= 2) {
            HorizontalDivider()

            when (state.type) {
                MovementType.IN -> {
                    LocationDropdownField(
                        label = "Magazzino di destinazione",
                        locations = state.locations,
                        selectedUuid = state.toLocationUuid,
                        onSelected = onToLocationChange
                    )
                }
                MovementType.OUT -> {
                    LocationDropdownField(
                        label = "Magazzino di partenza",
                        locations = state.locations,
                        selectedUuid = state.fromLocationUuid,
                        onSelected = onFromLocationChange
                    )
                }
                MovementType.TRANSFER -> {
                    LocationDropdownField(
                        label = "Da",
                        locations = state.locations,
                        selectedUuid = state.fromLocationUuid,
                        onSelected = onFromLocationChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LocationDropdownField(
                        label = "A",
                        locations = state.locations.filter { it.uuid != state.fromLocationUuid },
                        selectedUuid = state.toLocationUuid,
                        onSelected = onToLocationChange
                    )
                }
                MovementType.ADJUSTMENT -> Unit
            }

            state.locationError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider()

        // Quantity Input
        Text(
            text = "Quantità",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = state.quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantità *") },
            placeholder = { Text("0") },
            suffix = { Text(article.unitOfMeasure) },
            isError = state.quantityError != null,
            supportingText = state.quantityError?.let { { Text(it) } } ?: {
                val newQuantity = state.quantity.toDoubleOrNull() ?: 0.0
                when (state.type) {
                    MovementType.IN -> Text(
                        "Giacenza dopo movimento: ${state.toQuantity + newQuantity} ${article.unitOfMeasure}"
                    )
                    MovementType.OUT -> Text(
                        "Giacenza dopo movimento: ${state.fromQuantity - newQuantity} ${article.unitOfMeasure}"
                    )
                    MovementType.TRANSFER -> Text(
                        "Partenza: ${state.fromQuantity - newQuantity} • Arrivo: ${state.toQuantity + newQuantity} ${article.unitOfMeasure}"
                    )
                    MovementType.ADJUSTMENT -> Text(
                        "Giacenza attuale: ${inventory?.currentQuantity ?: 0.0} ${article.unitOfMeasure}"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            leadingIcon = {
                Icon(
                    when (state.type) {
                        MovementType.IN -> Icons.Default.Add
                        MovementType.OUT -> Icons.Default.Remove
                        MovementType.ADJUSTMENT -> Icons.Default.Edit
                        MovementType.TRANSFER -> Icons.AutoMirrored.Filled.CompareArrows
                    },
                    contentDescription = null,
                    tint = when (state.type) {
                        MovementType.IN -> MaterialTheme.colorScheme.primary
                        MovementType.OUT -> MaterialTheme.colorScheme.error
                        MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.tertiary
                        MovementType.TRANSFER -> MaterialTheme.colorScheme.secondary
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Warning se quantità insufficiente nel magazzino di partenza
        if (state.type == MovementType.OUT || state.type == MovementType.TRANSFER) {
            val requestedQty = state.quantity.toDoubleOrNull() ?: 0.0
            val availableQty = state.fromQuantity

            if (requestedQty > availableQty) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Quantità insufficiente! Disponibile: $availableQty ${article.unitOfMeasure}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Notes
        Text(
            text = "Note",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Note Aggiuntive") },
            placeholder = { Text("Fornitori, riferimenti, motivo...") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button
        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Check, "Salva")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when (state.type) {
                        MovementType.IN -> "Registra Carico"
                        MovementType.OUT -> "Registra Scarico"
                        MovementType.ADJUSTMENT -> "Registra Rettifica"
                        MovementType.TRANSFER -> "Registra Trasferimento"
                    }
                )
            }
        }

        // Required fields note
        Text(
            text = "* Campo obbligatorio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropdownField(
    label: String,
    locations: List<Location>,
    selectedUuid: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = locations.find { it.uuid == selectedUuid }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = { /* Read only, selezione via dropdown */ },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            locations.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.name) },
                    onClick = {
                        onSelected(location.uuid)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Warehouse,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}
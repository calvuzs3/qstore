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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qstore.app.domain.model.Location
import net.calvuz.qstore.app.domain.model.enum.MovementType

/**
 * Finestra a comparsa dal basso per registrare rapidamente un movimento su un articolo.
 * Quantità preimpostata a 1 e layout compatto pensato per stare in un solo schermo,
 * senza dover scorrere per raggiungere il pulsante di salvataggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovementBottomSheet(
    articleId: String,
    onDismiss: () -> Unit,
    viewModel: AddMovementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    LaunchedEffect(events) {
        when (val event = events) {
            is AddMovementEvent.NavigateBack -> onDismiss()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.article != null -> {
                    AddMovementSheetContent(
                        state = state,
                        onTypeChange = viewModel::onTypeChange,
                        onQuantityChange = viewModel::onQuantityChange,
                        onQuantityStep = viewModel::onQuantityStep,
                        onNotesChange = viewModel::onNotesChange,
                        onFromLocationChange = viewModel::onFromLocationChange,
                        onToLocationChange = viewModel::onToLocationChange,
                        onSaveClick = viewModel::onSaveClick
                    )
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "Errore sconosciuto",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun AddMovementSheetContent(
    state: AddMovementState,
    onTypeChange: (MovementType) -> Unit,
    onQuantityChange: (String) -> Unit,
    onQuantityStep: (Double) -> Unit,
    onNotesChange: (String) -> Unit,
    onFromLocationChange: (String) -> Unit,
    onToLocationChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val article = state.article ?: return
    val inventory = state.inventory
    var notesExpanded by remember { mutableStateOf(state.notes.isNotBlank()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Titolo + giacenza rilevante, su una riga compatta
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = article.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
                "Giacenza attuale"
            }
            val giacenzaValue = when (state.type) {
                MovementType.OUT, MovementType.TRANSFER -> state.fromQuantity
                MovementType.IN -> state.toQuantity
                MovementType.ADJUSTMENT -> inventory?.currentQuantity ?: 0.0
            }
            Text(
                text = "$giacenzaLabel: $giacenzaValue ${article.unitOfMeasure}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tipo movimento — riga unica al posto delle 3 card
        val types = listOf(
            Triple(MovementType.IN, "Carico", Icons.Default.Add),
            Triple(MovementType.OUT, "Scarico", Icons.Default.Remove),
            Triple(MovementType.TRANSFER, "Trasf.", Icons.AutoMirrored.Filled.CompareArrows)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, (type, label, icon) ->
                SegmentedButton(
                    selected = state.type == type,
                    onClick = { onTypeChange(type) },
                    enabled = type != MovementType.TRANSFER || state.locations.size >= 2,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = state.type == type) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                ) {
                    Text(label)
                }
            }
        }
        if (state.locations.size < 2) {
            Text(
                text = "Il trasferimento richiede un secondo magazzino",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Magazzini (solo se ne esiste più di uno — altrimenti si usa silenziosamente l'unico)
        if (state.locations.size >= 2) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LocationDropdownField(
                                label = "Da",
                                locations = state.locations,
                                selectedUuid = state.fromLocationUuid,
                                onSelected = onFromLocationChange
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            LocationDropdownField(
                                label = "A",
                                locations = state.locations.filter { it.uuid != state.fromLocationUuid },
                                selectedUuid = state.toLocationUuid,
                                onSelected = onToLocationChange
                            )
                        }
                    }
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

        // Quantità — stepper +/- con valore preimpostato a 1
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(onClick = { onQuantityStep(-1.0) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Diminuisci")
                }

                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    suffix = { Text(article.unitOfMeasure) },
                    isError = state.quantityError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                FilledTonalIconButton(onClick = { onQuantityStep(1.0) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aumenta")
                }
            }

            val newQuantity = state.quantity.toDoubleOrNull() ?: 0.0
            val supportingText = state.quantityError ?: when (state.type) {
                MovementType.IN -> "Dopo il movimento: ${state.toQuantity + newQuantity} ${article.unitOfMeasure}"
                MovementType.OUT -> "Dopo il movimento: ${state.fromQuantity - newQuantity} ${article.unitOfMeasure}"
                MovementType.TRANSFER -> "Partenza: ${state.fromQuantity - newQuantity} • Arrivo: ${state.toQuantity + newQuantity} ${article.unitOfMeasure}"
                MovementType.ADJUSTMENT -> "Giacenza attuale: ${inventory?.currentQuantity ?: 0.0} ${article.unitOfMeasure}"
            }
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.quantityError != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 4.dp)
            )

            // Warning se quantità insufficiente nel magazzino di partenza
            if (state.type == MovementType.OUT || state.type == MovementType.TRANSFER) {
                if (newQuantity > state.fromQuantity) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Quantità insufficiente! Disponibile: ${state.fromQuantity} ${article.unitOfMeasure}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Note — collassate dietro un pulsante opzionale per non occupare spazio di default
        if (notesExpanded) {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text("Note") },
                placeholder = { Text("Fornitori, riferimenti, motivo...") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TextButton(onClick = { notesExpanded = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aggiungi nota")
            }
        }

        // Save Button
        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
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

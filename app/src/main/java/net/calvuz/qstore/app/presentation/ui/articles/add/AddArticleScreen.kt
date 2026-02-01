package net.calvuz.qstore.app.presentation.ui.articles.add

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import net.calvuz.qstore.categories.domain.model.ArticleCategory
import net.calvuz.qstore.app.domain.model.ArticleImage
import net.calvuz.qstore.app.presentation.ui.common.PhotoCaptureDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddArticleScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddArticleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val events by viewModel.events.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(events) {
        when (val event = events) {
            is AddArticleEvent.NavigateBack -> onNavigateBack()
            is AddArticleEvent.ShowError -> {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
            is AddArticleEvent.ShowSuccess -> {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
            null -> { /* No event */ }
        }
        viewModel.onEventConsumed()
    }

    // Photo capture dialog
    if (state.showPhotoDialog) {
        PhotoCaptureDialog(
            onDismiss = viewModel::onDismissPhotoDialog,
            onPhotoTaken = viewModel::onPhotoTaken
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditMode) "Modifica Articolo" else "Nuovo Articolo")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AddArticleContent(
                state = state,
                categories = categories,
                onNameChange = viewModel::onNameChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onCategorySelected = viewModel::onCategorySelected,
                onUnitOfMeasureChange = viewModel::onUnitOfMeasureChange,
                onReorderLevelChange = viewModel::onReorderLevelChange,
                onCodeOEMChange = viewModel::onCodeOEMChange,
                onCodeERPChange = viewModel::onCodeERPChange,
                onCodeBMChange = viewModel::onCodeBMChange,
                onNotesChange = viewModel::onNotesChange,
                onInitialQuantityChange = viewModel::onInitialQuantityChange,
                onAddPhotoClick = viewModel::onAddPhotoClick,
                onRemoveCapturedImage = viewModel::onRemoveCapturedImage,
                onRemoveSavedImage = viewModel::onRemoveSavedImage,
                onSaveClick = viewModel::onSaveClick,
                modifier = Modifier
                    .padding(padding)

            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddArticleContent(
    state: AddArticleState,
    categories: List<ArticleCategory>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategorySelected: (ArticleCategory) -> Unit,
    onUnitOfMeasureChange: (String) -> Unit,
    onReorderLevelChange: (String) -> Unit,
    onCodeOEMChange: (String) -> Unit,
    onCodeERPChange: (String) -> Unit,
    onCodeBMChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onInitialQuantityChange: (String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onRemoveCapturedImage: (String) -> Unit,
    onRemoveSavedImage: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnitDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val commonUnits = listOf("pz", "kg", "lt", "mt", "scatola", "pallet", "conf")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Informazioni Base
        Text(
            text = "Informazioni Base",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Nome (obbligatorio)
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Nome Articolo *") },
            placeholder = { Text("Es: Gomito zinc. FF d.3/4") },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Descrizione
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text("Descrizione") },
            placeholder = { Text("Descrizione dettagliata dell'articolo") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // Section: Foto Articolo
        Text(
            text = "Foto Articolo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Le foto permettono di riconoscere l'articolo tramite la fotocamera",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Grid immagini
        PhotosGrid(
            capturedImages = state.capturedImages,
            savedImages = state.savedImages,
            onAddPhotoClick = onAddPhotoClick,
            onRemoveCapturedImage = onRemoveCapturedImage,
            onRemoveSavedImage = onRemoveSavedImage
        )

        HorizontalDivider()

        // Section: Classificazione
        Text(
            text = "Classificazione",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Categoria con dropdown (da database)
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = it }
        ) {
            OutlinedTextField(
                value = state.categoryName,
                onValueChange = { /* Read only, selection via dropdown */ },
                readOnly = true,
                label = { Text("Categoria *") },
                placeholder = { Text("Seleziona una categoria") },
                isError = state.categoryError != null,
                supportingText = state.categoryError?.let { { Text(it) } },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                if (categories.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Nessuna categoria disponibile",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showCategoryDropdown = false },
                        enabled = false
                    )
                } else {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                onCategorySelected(category)
                                showCategoryDropdown = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Section: Codici Esterni
        Text(
            text = "Codici Esterni",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Codici per collegamento a sistemi esterni (opzionali)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Codice OEM
        OutlinedTextField(
            value = state.codeOEM,
            onValueChange = onCodeOEMChange,
            label = { Text("Codice OEM") },
            placeholder = { Text("Codice produttore originale") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Factory, "OEM")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Codice ERP
        OutlinedTextField(
            value = state.codeERP,
            onValueChange = onCodeERPChange,
            label = { Text("Codice ERP") },
            placeholder = { Text("Codice gestionale aziendale") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Business, "ERP")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Codice BM (Business Manager o altro)
        OutlinedTextField(
            value = state.codeBM,
            onValueChange = onCodeBMChange,
            label = { Text("Codice BM") },
            placeholder = { Text("Codice business manager") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Badge, "BM")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            ),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // Section: Unità e Gestione Scorte
        Text(
            text = "Unità e Gestione Scorte",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Unità di misura con dropdown
        ExposedDropdownMenuBox(
            expanded = showUnitDropdown,
            onExpandedChange = { showUnitDropdown = it }
        ) {
            OutlinedTextField(
                value = state.unitOfMeasure,
                onValueChange = onUnitOfMeasureChange,
                label = { Text("Unità di Misura *") },
                placeholder = { Text("Es: pz, kg, lt") },
                isError = state.unitError != null,
                supportingText = state.unitError?.let { { Text(it) } },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitDropdown)
                },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = showUnitDropdown,
                onDismissRequest = { showUnitDropdown = false }
            ) {
                commonUnits.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit) },
                        onClick = {
                            onUnitOfMeasureChange(unit)
                            showUnitDropdown = false
                        }
                    )
                }
            }
        }

        // Soglia sotto scorta
        OutlinedTextField(
            value = state.reorderLevel,
            onValueChange = onReorderLevelChange,
            label = { Text("Soglia Sotto Scorta") },
            placeholder = { Text("0") },
            isError = state.reorderLevelError != null,
            supportingText = state.reorderLevelError?.let { { Text(it) } } ?: {
                Text("Quantità minima prima dell'avviso (0 = disabilitato)")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            leadingIcon = {
                Icon(Icons.Default.Warning, "Soglia")
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Quantità iniziale (solo in creazione)
        if (!state.isEditMode) {
            OutlinedTextField(
                value = state.initialQuantity,
                onValueChange = onInitialQuantityChange,
                label = { Text("Quantità Iniziale") },
                placeholder = { Text("0") },
                suffix = { Text(state.unitOfMeasure) },
                isError = state.initialQuantityError != null,
                supportingText = state.initialQuantityError?.let { { Text(it) } } ?: {
                    Text("Giacenza iniziale in magazzino")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                leadingIcon = {
                    Icon(Icons.Default.Inventory, "Quantità")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()

        // Note
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Note Aggiuntive") },
            placeholder = { Text("Note, istruzioni o informazioni extra") },
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
                Text(if (state.isEditMode) "Aggiorna Articolo" else "Crea Articolo")
            }
        }
    }
}

@Composable
private fun PhotosGrid(
    capturedImages: List<CapturedImage>,
    savedImages: List<ArticleImage>,
    onAddPhotoClick: () -> Unit,
    onRemoveCapturedImage: (String) -> Unit,
    onRemoveSavedImage: (String) -> Unit
) {
    val totalImages = capturedImages.size + savedImages.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (totalImages > 0) "$totalImages foto" else "Nessuna foto",
                    style = MaterialTheme.typography.titleSmall
                )

                FilledTonalButton(
                    onClick = onAddPhotoClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Scatta")
                }
            }

            if (totalImages > 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    // Immagini catturate (non ancora salvate)
                    items(
                        items = capturedImages,
                        key = { it.id }
                    ) { capturedImage ->
                        CapturedImageItem(
                            capturedImage = capturedImage,
                            onRemove = { onRemoveCapturedImage(capturedImage.id) }
                        )
                    }

                    // Immagini già salvate nel database
                    items(
                        items = savedImages,
                        key = { it.uuid }
                    ) { savedImage ->
                        SavedImageItem(
                            savedImage = savedImage,
                            onRemove = { onRemoveSavedImage(savedImage.uuid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturedImageItem(
    capturedImage: CapturedImage,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Image(
            bitmap = capturedImage.bitmap.asImageBitmap(),
            contentDescription = "Foto catturata",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Badge "Nuova"
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 8.dp
        ) {
            Text(
                text = "NUOVA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Delete button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Rimuovi",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedImageItem(
    savedImage: ArticleImage,
    onRemove: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fullPath = remember(savedImage.imagePath) {
        // Converti path relativo in path assoluto
        java.io.File(context.filesDir, "article_images/${savedImage.imagePath}").absolutePath
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = java.io.File(fullPath),
            contentDescription = "Foto salvata",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Delete button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Rimuovi",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
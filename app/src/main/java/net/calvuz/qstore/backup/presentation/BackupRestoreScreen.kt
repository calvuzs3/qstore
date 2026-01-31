package net.calvuz.qstore.backup.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qstore.backup.domain.model.BackupMetadata
import net.calvuz.qstore.backup.domain.repository.BackupFileInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Dialog states
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var selectedUriForRestore by remember { mutableStateOf<Uri?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedBackupForDelete by remember { mutableStateOf<BackupFileInfo?>(null) }
    
    // File picker per importare backup
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Prima valida, poi chiedi conferma
            viewModel.validateBackupFromUri(it)
            selectedUriForRestore = it
        }
    }
    
    // Gestione eventi
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BackupEvent.BackupCreated -> {
                    val sizeStr = formatFileSize(event.sizeBytes)
                    snackbarHostState.showSnackbar(
                        message = "Backup creato: ${event.file.name} ($sizeStr)",
                        actionLabel = "Condividi",
                        duration = SnackbarDuration.Long
                    ).let { result ->
                        if (result == SnackbarResult.ActionPerformed) {
                            shareBackupFile(context, event.file)
                        }
                    }
                }
                is BackupEvent.RestoreCompleted -> {
                    snackbarHostState.showSnackbar("Ripristino completato con successo!")
                }
                is BackupEvent.BackupDeleted -> {
                    snackbarHostState.showSnackbar("Backup eliminato: ${event.fileName}")
                }
                is BackupEvent.ValidationSuccess -> {
                    // Mostra dialog di conferma
                    if (selectedUriForRestore != null) {
                        showRestoreConfirmDialog = true
                    }
                }
                is BackupEvent.ValidationFailed -> {
                    snackbarHostState.showSnackbar("Backup non valido: ${event.reason}")
                    selectedUriForRestore = null
                }
                is BackupEvent.Error -> {
                    snackbarHostState.showSnackbar("Errore: ${event.message}")
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup e Ripristino") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sezione Crea Backup
            item {
                CreateBackupSection(
                    estimatedSize = uiState.estimatedBackupSize,
                    isCreating = uiState.isCreatingBackup,
                    progress = uiState.progress?.progress,
                    progressPhase = uiState.progress?.phase,
                    onCreateBackup = { viewModel.createBackup() }
                )
            }
            
            // Sezione Importa Backup
            item {
                ImportBackupSection(
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/zip"))
                    },
                    isValidating = uiState.isValidating
                )
            }
            
            // Divider
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            // Sezione Backup Disponibili
            item {
                Text(
                    text = "Backup Disponibili",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (uiState.isLoadingBackups) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.availableBackups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Nessun backup disponibile nella cartella QStore",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.availableBackups) { backup ->
                    BackupItem(
                        backup = backup,
                        onRestore = {
                            selectedBackupForRestore = backup
                            showRestoreConfirmDialog = true
                        },
                        onDelete = {
                            selectedBackupForDelete = backup
                            showDeleteConfirmDialog = true
                        },
                        onShare = {
                            shareBackupFile(context, backup.file)
                        }
                    )
                }
            }
            
            // Spacer finale
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Progress overlay durante operazioni
        if (uiState.isCreatingBackup || uiState.isRestoring) {
            ProgressOverlay(
                message = if (uiState.isCreatingBackup) "Creazione backup..." else "Ripristino...",
                progress = uiState.progress?.progress ?: 0f,
                phase = uiState.progress?.phase ?: ""
            )
        }
    }
    
    // Dialog conferma ripristino
    if (showRestoreConfirmDialog) {
        RestoreConfirmDialog(
            metadata = uiState.selectedBackupMetadata,
            onConfirm = {
                showRestoreConfirmDialog = false
                selectedBackupForRestore?.let {
                    viewModel.restoreBackup(it.file)
                }
                selectedUriForRestore?.let {
                    viewModel.restoreBackupFromUri(it)
                }
                selectedBackupForRestore = null
                selectedUriForRestore = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                selectedBackupForRestore = null
                selectedUriForRestore = null
                viewModel.clearSelectedBackup()
            }
        )
    }
    
    // Dialog conferma eliminazione
    if (showDeleteConfirmDialog && selectedBackupForDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                selectedBackupForDelete = null
            },
            title = { Text("Elimina Backup") },
            text = {
                Text("Sei sicuro di voler eliminare questo backup?\n\n${selectedBackupForDelete?.file?.name}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBackupForDelete?.file?.let { viewModel.deleteBackup(it) }
                        showDeleteConfirmDialog = false
                        selectedBackupForDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    selectedBackupForDelete = null
                }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun CreateBackupSection(
    estimatedSize: Long,
    isCreating: Boolean,
    progress: Float?,
    progressPhase: String?,
    onCreateBackup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Crea Nuovo Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Esporta tutti i dati dell'app: articoli, categorie, movimenti, immagini e impostazioni.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Dimensione stimata: ${formatFileSize(estimatedSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isCreating && progress != null) {
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = progressPhase ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Button(
                onClick = onCreateBackup,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isCreating) "Creazione in corso..." else "Crea Backup")
            }
        }
    }
}

@Composable
private fun ImportBackupSection(
    onImportBackup: () -> Unit,
    isValidating: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.FileOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Importa da File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Seleziona un file di backup (.zip) da una posizione qualsiasi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedButton(
                onClick = onImportBackup,
                enabled = !isValidating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isValidating) "Validazione..." else "Seleziona File")
            }
        }
    }
}

@Composable
private fun BackupItem(
    backup: BackupFileInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backup.file.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = formatDate(backup.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatFileSize(backup.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                if (backup.isValid) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text("Valido", modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text("Non valido", modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            
            // Info dal metadata se disponibile
            backup.metadata?.let { metadata ->
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip("${metadata.counts.articles} articoli")
                    InfoChip("${metadata.counts.movements} movimenti")
                    InfoChip("${metadata.counts.imageFiles} immagini")
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Azioni
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestore,
                    enabled = backup.isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ripristina")
                }
                
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Condividi")
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RestoreConfirmDialog(
    metadata: BackupMetadata?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("Conferma Ripristino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Questa operazione sovrascriverà TUTTI i dati esistenti.",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Text("Un backup di sicurezza verrà creato automaticamente prima del ripristino.")
                
                metadata?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Contenuto del backup:", fontWeight = FontWeight.Medium)
                    Text("• ${it.counts.categories} categorie")
                    Text("• ${it.counts.articles} articoli")
                    Text("• ${it.counts.movements} movimenti")
                    Text("• ${it.counts.imageFiles} immagini")
                    Text("• Data backup: ${it.backupDate}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Ripristina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun ProgressOverlay(
    message: String,
    progress: Float,
    phase: String
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(200.dp)
                    .padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = phase,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun shareBackupFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Condividi backup"))
    } catch (_: Exception) {
        // Gestito silenziosamente
    }
}

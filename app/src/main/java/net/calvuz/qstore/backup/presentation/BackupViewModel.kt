package net.calvuz.qstore.backup.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qstore.backup.domain.model.*
import net.calvuz.qstore.backup.domain.repository.BackupFileInfo
import net.calvuz.qstore.backup.domain.usecase.CreateBackupUseCase
import net.calvuz.qstore.backup.domain.usecase.RestoreBackupUseCase
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import java.io.File
import javax.inject.Inject

/**
 * ViewModel per la gestione del backup/restore
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val backupRepository: BackupRepository
) : ViewModel() {
    
    // ============================================
    // STATE
    // ============================================
    
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()
    
    init {
        loadAvailableBackups()
        loadEstimatedSize()
    }
    
    // ============================================
    // BACKUP OPERATIONS
    // ============================================
    
    /**
     * Crea un nuovo backup
     */
    fun createBackup(options: BackupOptions = BackupOptions()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBackup = true, progress = null, error = null) }
            
            try {
                createBackupUseCase(options).collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
                
                // Backup completato con successo
                val result = createBackupUseCase.sync(options)
                when (result) {
                    is BackupResult.Success -> {
                        _events.emit(BackupEvent.BackupCreated(result.file, result.sizeBytes))
                        loadAvailableBackups()
                    }
                    is BackupResult.Error -> {
                        _uiState.update { it.copy(error = result.error.message) }
                        _events.emit(BackupEvent.Error(result.error.message ?: "Errore sconosciuto"))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                _events.emit(BackupEvent.Error(e.message ?: "Errore durante la creazione del backup"))
            } finally {
                _uiState.update { it.copy(isCreatingBackup = false, progress = null) }
            }
        }
    }
    
    /**
     * Crea un backup salvandolo nella posizione scelta dall'utente
     */
    fun createBackupToLocation(destinationDir: File) {
        val options = BackupOptions(destinationDir = destinationDir)
        createBackup(options)
    }
    
    // ============================================
    // RESTORE OPERATIONS
    // ============================================
    
    /**
     * Ripristina un backup da file
     */
    fun restoreBackup(backupFile: File, options: RestoreOptions = RestoreOptions()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, progress = null, error = null) }
            
            try {
                restoreBackupUseCase(backupFile, options).collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
                
                _events.emit(BackupEvent.RestoreCompleted)
                loadAvailableBackups()
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                _events.emit(BackupEvent.Error(e.message ?: "Errore durante il ripristino"))
            } finally {
                _uiState.update { it.copy(isRestoring = false, progress = null) }
            }
        }
    }
    
    /**
     * Ripristina un backup da URI (Document Picker)
     */
    fun restoreBackupFromUri(backupUri: Uri, options: RestoreOptions = RestoreOptions()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, progress = null, error = null) }
            
            try {
                restoreBackupUseCase(backupUri, options).collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
                
                _events.emit(BackupEvent.RestoreCompleted)
                loadAvailableBackups()
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                _events.emit(BackupEvent.Error(e.message ?: "Errore durante il ripristino"))
            } finally {
                _uiState.update { it.copy(isRestoring = false, progress = null) }
            }
        }
    }
    
    // ============================================
    // VALIDATION
    // ============================================
    
    /**
     * Valida un backup prima del ripristino
     */
    fun validateBackup(backupFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, selectedBackupMetadata = null) }
            
            val result = restoreBackupUseCase.validate(backupFile)
            result.fold(
                onSuccess = { metadata ->
                    _uiState.update { 
                        it.copy(
                            isValidating = false,
                            selectedBackupMetadata = metadata
                        )
                    }
                    _events.emit(BackupEvent.ValidationSuccess(metadata))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isValidating = false, error = error.message) }
                    _events.emit(BackupEvent.ValidationFailed(error.message ?: "Backup non valido"))
                }
            )
        }
    }
    
    /**
     * Valida un backup da URI
     */
    fun validateBackupFromUri(backupUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, selectedBackupMetadata = null) }
            
            val result = restoreBackupUseCase.validate(backupUri)
            result.fold(
                onSuccess = { metadata ->
                    _uiState.update { 
                        it.copy(
                            isValidating = false,
                            selectedBackupMetadata = metadata
                        )
                    }
                    _events.emit(BackupEvent.ValidationSuccess(metadata))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isValidating = false, error = error.message) }
                    _events.emit(BackupEvent.ValidationFailed(error.message ?: "Backup non valido"))
                }
            )
        }
    }
    
    // ============================================
    // UTILITY
    // ============================================
    
    /**
     * Carica la lista dei backup disponibili
     */
    fun loadAvailableBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBackups = true) }
            
            val backups = backupRepository.getAvailableBackups()
            _uiState.update { 
                it.copy(
                    isLoadingBackups = false,
                    availableBackups = backups
                )
            }
        }
    }
    
    /**
     * Carica la dimensione stimata del backup
     */
    fun loadEstimatedSize() {
        viewModelScope.launch {
            val size = backupRepository.estimateBackupSize()
            _uiState.update { it.copy(estimatedBackupSize = size) }
        }
    }
    
    /**
     * Elimina un backup
     */
    fun deleteBackup(backupFile: File) {
        viewModelScope.launch {
            val success = backupRepository.deleteBackup(backupFile)
            if (success) {
                _events.emit(BackupEvent.BackupDeleted(backupFile.name))
                loadAvailableBackups()
            } else {
                _events.emit(BackupEvent.Error("Impossibile eliminare il backup"))
            }
        }
    }
    
    /**
     * Resetta l'errore
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Resetta i metadata del backup selezionato
     */
    fun clearSelectedBackup() {
        _uiState.update { it.copy(selectedBackupMetadata = null) }
    }
}

/**
 * Stato della UI per il backup
 */
data class BackupUiState(
    val isCreatingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val isValidating: Boolean = false,
    val isLoadingBackups: Boolean = false,
    val progress: BackupProgress? = null,
    val error: String? = null,
    val availableBackups: List<BackupFileInfo> = emptyList(),
    val selectedBackupMetadata: BackupMetadata? = null,
    val estimatedBackupSize: Long = 0L
)

/**
 * Eventi one-shot per la UI
 */
sealed class BackupEvent {
    data class BackupCreated(val file: File, val sizeBytes: Long) : BackupEvent()
    data object RestoreCompleted : BackupEvent()
    data class BackupDeleted(val fileName: String) : BackupEvent()
    data class ValidationSuccess(val metadata: BackupMetadata) : BackupEvent()
    data class ValidationFailed(val reason: String) : BackupEvent()
    data class Error(val message: String) : BackupEvent()
}

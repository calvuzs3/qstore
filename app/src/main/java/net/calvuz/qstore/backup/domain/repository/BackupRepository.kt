package net.calvuz.qstore.backup.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.backup.domain.model.*
import java.io.File

/**
 * Repository per le operazioni di backup e ripristino
 */
interface BackupRepository {
    
    // ============================================
    // BACKUP OPERATIONS
    // ============================================
    
    /**
     * Crea un backup completo del sistema
     * @param options Opzioni per la creazione del backup
     * @return Flow con lo stato di avanzamento e il risultato finale
     */
    fun createBackup(options: BackupOptions = BackupOptions()): Flow<BackupProgress>
    
    /**
     * Crea un backup e ritorna il risultato
     * @param options Opzioni per la creazione del backup
     * @return Risultato dell'operazione
     */
    suspend fun createBackupSync(options: BackupOptions = BackupOptions()): BackupResult
    
    // ============================================
    // RESTORE OPERATIONS
    // ============================================
    
    /**
     * Ripristina un backup da file
     * @param backupFile File ZIP del backup
     * @param options Opzioni per il ripristino
     * @return Flow con lo stato di avanzamento
     */
    fun restoreBackup(backupFile: File, options: RestoreOptions = RestoreOptions()): Flow<BackupProgress>
    
    /**
     * Ripristina un backup da URI (per Document Picker)
     * @param backupUri URI del file di backup
     * @param options Opzioni per il ripristino
     * @return Flow con lo stato di avanzamento
     */
    fun restoreBackup(backupUri: Uri, options: RestoreOptions = RestoreOptions()): Flow<BackupProgress>
    
    /**
     * Ripristina un backup in modo sincrono
     * @param backupFile File ZIP del backup
     * @param options Opzioni per il ripristino
     * @return Risultato dell'operazione
     */
    suspend fun restoreBackupSync(backupFile: File, options: RestoreOptions = RestoreOptions()): RestoreResult
    
    // ============================================
    // VALIDATION
    // ============================================
    
    /**
     * Valida un file di backup senza ripristinarlo
     * @param backupFile File ZIP da validare
     * @return Metadati se valido, ValidationError se non valido
     */
    suspend fun validateBackup(backupFile: File): Result<BackupMetadata>
    
    /**
     * Valida un file di backup da URI
     * @param backupUri URI del file da validare
     * @return Metadati se valido, errore se non valido
     */
    suspend fun validateBackup(backupUri: Uri): Result<BackupMetadata>
    
    // ============================================
    // UTILITY
    // ============================================
    
    /**
     * Ottiene la lista dei backup disponibili nella directory di default
     * @return Lista di file di backup trovati
     */
    suspend fun getAvailableBackups(): List<BackupFileInfo>
    
    /**
     * Elimina un file di backup
     * @param backupFile File da eliminare
     * @return true se eliminato con successo
     */
    suspend fun deleteBackup(backupFile: File): Boolean
    
    /**
     * Ottiene la dimensione stimata del backup (senza crearlo)
     * @return Dimensione stimata in bytes
     */
    suspend fun estimateBackupSize(): Long
}

/**
 * Informazioni su un file di backup
 */
data class BackupFileInfo(
    val file: File,
    val metadata: BackupMetadata?,
    val sizeBytes: Long,
    val lastModified: Long,
    val isValid: Boolean
)

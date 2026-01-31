package net.calvuz.qstore.backup.domain.usecase

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.backup.domain.model.*
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case per il ripristino di un backup
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    /**
     * Ripristina un backup da file con progress reporting
     * @param backupFile File ZIP del backup
     * @param options Opzioni per il ripristino
     * @return Flow di BackupProgress per monitorare l'avanzamento
     */
    operator fun invoke(
        backupFile: File,
        options: RestoreOptions = RestoreOptions()
    ): Flow<BackupProgress> {
        return backupRepository.restoreBackup(backupFile, options)
    }
    
    /**
     * Ripristina un backup da URI (Document Picker) con progress reporting
     * @param backupUri URI del file di backup
     * @param options Opzioni per il ripristino
     * @return Flow di BackupProgress per monitorare l'avanzamento
     */
    operator fun invoke(
        backupUri: Uri,
        options: RestoreOptions = RestoreOptions()
    ): Flow<BackupProgress> {
        return backupRepository.restoreBackup(backupUri, options)
    }
    
    /**
     * Ripristina un backup in modo sincrono
     * @param backupFile File ZIP del backup
     * @param options Opzioni per il ripristino
     * @return Risultato dell'operazione
     */
    suspend fun sync(
        backupFile: File,
        options: RestoreOptions = RestoreOptions()
    ): RestoreResult {
        return backupRepository.restoreBackupSync(backupFile, options)
    }
    
    /**
     * Valida un backup prima del ripristino
     * @param backupFile File da validare
     * @return Metadati se valido, errore altrimenti
     */
    suspend fun validate(backupFile: File): Result<BackupMetadata> {
        return backupRepository.validateBackup(backupFile)
    }
    
    /**
     * Valida un backup da URI
     * @param backupUri URI del file da validare
     * @return Metadati se valido, errore altrimenti
     */
    suspend fun validate(backupUri: Uri): Result<BackupMetadata> {
        return backupRepository.validateBackup(backupUri)
    }
}

package net.calvuz.qstore.backup.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.backup.domain.model.BackupOptions
import net.calvuz.qstore.backup.domain.model.BackupProgress
import net.calvuz.qstore.backup.domain.model.BackupResult
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case per la creazione di un backup completo del sistema
 */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    /**
     * Crea un backup con progress reporting
     * @param options Opzioni per il backup
     * @return Flow di BackupProgress per monitorare l'avanzamento
     */
    operator fun invoke(options: BackupOptions = BackupOptions()): Flow<BackupProgress> {
        return backupRepository.createBackup(options)
    }
    
    /**
     * Crea un backup in modo sincrono (blocking)
     * @param options Opzioni per il backup
     * @return Risultato dell'operazione
     */
    suspend fun sync(options: BackupOptions = BackupOptions()): BackupResult {
        return backupRepository.createBackupSync(options)
    }
    
    /**
     * Stima la dimensione del backup senza crearlo
     * @return Dimensione stimata in bytes
     */
    suspend fun estimateSize(): Long {
        return backupRepository.estimateBackupSize()
    }
}

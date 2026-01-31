package net.calvuz.qstore.backup.domain.model

import java.io.File

/**
 * Risultato dell'operazione di creazione backup
 */
sealed class BackupResult {
    
    /**
     * Backup creato con successo
     * @param file File ZIP del backup
     * @param metadata Metadati del backup
     * @param sizeBytes Dimensione del file in bytes
     */
    data class Success(
        val file: File,
        val metadata: BackupMetadata,
        val sizeBytes: Long
    ) : BackupResult()
    
    /**
     * Errore durante la creazione del backup
     * @param error Eccezione che ha causato l'errore
     * @param phase Fase in cui si è verificato l'errore
     */
    data class Error(
        val error: Throwable,
        val phase: BackupPhase
    ) : BackupResult()
}

/**
 * Risultato dell'operazione di ripristino backup
 */
sealed class RestoreResult {
    
    /**
     * Ripristino completato con successo
     * @param metadata Metadati del backup ripristinato
     * @param restoredCounts Conteggi degli elementi ripristinati
     */
    data class Success(
        val metadata: BackupMetadata,
        val restoredCounts: BackupCounts
    ) : RestoreResult()
    
    /**
     * Errore durante il ripristino
     * @param error Eccezione che ha causato l'errore
     * @param phase Fase in cui si è verificato l'errore
     * @param rollbackSuccessful Se il rollback è stato eseguito con successo
     */
    data class Error(
        val error: Throwable,
        val phase: RestorePhase,
        val rollbackSuccessful: Boolean = false
    ) : RestoreResult()
    
    /**
     * Backup non valido o incompatibile
     * @param reason Motivo dell'invalidità
     */
    data class Invalid(
        val reason: ValidationError
    ) : RestoreResult()
}

/**
 * Fasi dell'operazione di backup
 */
enum class BackupPhase {
    INITIALIZING,
    EXPORTING_CATEGORIES,
    EXPORTING_ARTICLES,
    EXPORTING_INVENTORY,
    EXPORTING_MOVEMENTS,
    EXPORTING_ARTICLE_IMAGES,
    EXPORTING_SETTINGS,
    COPYING_IMAGE_FILES,
    CREATING_ZIP,
    FINALIZING
}

/**
 * Fasi dell'operazione di ripristino
 */
enum class RestorePhase {
    VALIDATING_ZIP,
    READING_METADATA,
    VALIDATING_CHECKSUMS,
    CLEARING_DATABASE,
    RESTORING_CATEGORIES,
    RESTORING_ARTICLES,
    RESTORING_INVENTORY,
    RESTORING_ARTICLE_IMAGES,
    RESTORING_MOVEMENTS,
    RESTORING_IMAGE_FILES,
    RESTORING_SETTINGS,
    FINALIZING
}

/**
 * Errori di validazione del backup
 */
sealed class ValidationError {
    object InvalidZipStructure : ValidationError()
    object MissingMetadata : ValidationError()
    object MissingDataFiles : ValidationError()
    data class IncompatibleVersion(val backupVersion: Int, val currentVersion: Int) : ValidationError()
    data class ChecksumMismatch(val component: String) : ValidationError()
    data class MissingImages(val missingPaths: List<String>) : ValidationError()
    data class CorruptedData(val details: String) : ValidationError()
}

/**
 * Stato di avanzamento del backup/restore
 */
data class BackupProgress(
    val phase: String,
    val progress: Float, // 0.0 - 1.0
    val currentItem: String? = null,
    val totalItems: Int? = null,
    val processedItems: Int? = null
)

/**
 * Opzioni per la creazione del backup
 */
data class BackupOptions(
    /** Includere le features OpenCV (aumenta dimensione file) */
    val includeFeatures: Boolean = true,
    
    /** Compressione ZIP (0-9, default 6) */
    val compressionLevel: Int = 6,
    
    /** Directory di destinazione (null = usa default Downloads) */
    val destinationDir: File? = null
)

/**
 * Opzioni per il ripristino del backup
 */
data class RestoreOptions(
    /** Verifica checksums prima del ripristino */
    val verifyChecksums: Boolean = true,
    
    /** Crea backup automatico prima del ripristino */
    val createBackupBeforeRestore: Boolean = true
)

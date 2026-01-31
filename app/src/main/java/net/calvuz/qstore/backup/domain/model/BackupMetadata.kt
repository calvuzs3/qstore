package net.calvuz.qstore.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * Metadati del backup - informazioni sul backup stesso
 */
@Serializable
data class BackupMetadata(
    /** Versione dell'applicazione che ha creato il backup */
    val appVersion: String,
    
    /** Version code dell'applicazione */
    val appVersionCode: Int,
    
    /** Versione del database */
    val dbVersion: Int,
    
    /** Data e ora di creazione del backup (ISO 8601 UTC) */
    val backupDate: String,
    
    /** Informazioni sul dispositivo (opzionale) */
    val deviceInfo: String? = null,
    
    /** Conteggi degli elementi nel backup */
    val counts: BackupCounts,
    
    /** Checksums SHA-256 dei vari componenti */
    val checksums: BackupChecksums,
    
    /** Manifest delle immagini (lista dei path relativi) */
    val imagesManifest: List<String>
)

/**
 * Conteggi degli elementi nel backup
 */
@Serializable
data class BackupCounts(
    val categories: Int,
    val articles: Int,
    val inventory: Int,
    val movements: Int,
    val articleImages: Int,
    val imageFiles: Int
)

/**
 * Checksums SHA-256 dei componenti del backup
 */
@Serializable
data class BackupChecksums(
    val categories: String,
    val articles: String,
    val inventory: String,
    val movements: String,
    val articleImages: String,
    val displaySettings: String,
    val recognitionSettings: String,
    val imagesManifest: String
)

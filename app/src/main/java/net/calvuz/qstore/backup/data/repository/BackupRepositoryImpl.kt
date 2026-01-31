package net.calvuz.qstore.backup.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.calvuz.qstore.BuildConfig
import net.calvuz.qstore.app.data.local.database.*
import net.calvuz.qstore.app.data.local.storage.ImageStorageManager
import net.calvuz.qstore.backup.data.serializer.BackupSerializer
import net.calvuz.qstore.backup.data.zip.BackupContentProvider
import net.calvuz.qstore.backup.data.zip.BackupZipContent
import net.calvuz.qstore.backup.data.zip.BackupZipManager
import net.calvuz.qstore.backup.domain.model.*
import net.calvuz.qstore.backup.domain.repository.BackupFileInfo
import net.calvuz.qstore.backup.domain.repository.BackupRepository
import net.calvuz.qstore.categories.data.local.ArticleCategoryDao
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository
import net.calvuz.qstore.settings.domain.repository.RecognitionSettingsRepository
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione del repository per backup e ripristino
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QuickStoreDatabase,
    private val articleDao: ArticleDao,
    private val articleCategoryDao: ArticleCategoryDao,
    private val inventoryDao: InventoryDao,
    private val movementDao: MovementDao,
    private val articleImageDao: ArticleImageDao,
    private val imageStorageManager: ImageStorageManager,
    private val displaySettingsRepository: DisplaySettingsRepository,
    private val recognitionSettingsRepository: RecognitionSettingsRepository,
    private val serializer: BackupSerializer,
    private val zipManager: BackupZipManager
) : BackupRepository {
    
    // ============================================
    // BACKUP CREATION
    // ============================================
    
    override fun createBackup(options: BackupOptions): Flow<BackupProgress> = flow {
        emit(BackupProgress("Inizializzazione...", 0.0f))
        
        try {
            val result = createBackupInternal(options) { phase, progress ->
                emit(BackupProgress(phase, progress))
            }
            
            when (result) {
                is BackupResult.Success -> emit(BackupProgress("Backup completato!", 1.0f))
                is BackupResult.Error -> throw result.error
            }
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun createBackupSync(options: BackupOptions): BackupResult {
        return withContext(Dispatchers.IO) {
            createBackupInternal(options) { _, _ -> }
        }
    }
    
    private suspend fun createBackupInternal(
        options: BackupOptions,
        progressCallback: suspend (String, Float) -> Unit
    ): BackupResult {
        try {
            progressCallback("Esportazione categorie...", 0.05f)
            
            // 1. Carica tutti i dati dal database
            val categories = articleCategoryDao.getAll().map { serializer.mapCategory(it) }
            progressCallback("Esportazione articoli...", 0.15f)
            
            val articles = articleDao.getAll().map { serializer.mapArticle(it) }
            progressCallback("Esportazione inventario...", 0.25f)
            
            val inventory = inventoryDao.getAll().map { serializer.mapInventory(it) }
            progressCallback("Esportazione movimenti...", 0.35f)
            
            val movements = movementDao.getAllMovements().map { serializer.mapMovement(it) }
            progressCallback("Esportazione immagini database...", 0.45f)
            
            val articleImages = articleImageDao.getAll().map { serializer.mapArticleImage(it) }
            progressCallback("Esportazione impostazioni...", 0.50f)
            
            // 2. Carica le impostazioni
            val displaySettings = displaySettingsRepository.getSettings().first()
            val recognitionSettings = recognitionSettingsRepository.getSettings().first()
            val currentPreset = recognitionSettingsRepository.getCurrentPreset().first()
            
            progressCallback("Copia file immagini...", 0.55f)
            
            // 3. Carica i file delle immagini
            val imageFiles = mutableMapOf<String, ByteArray>()
            val imagesManifest = mutableListOf<String>()
            
            articleImages.forEach { image ->
                val imageData = imageStorageManager.readImage(image.imagePath).getOrNull()
                if (imageData != null) {
                    imageFiles[image.imagePath] = imageData
                    imagesManifest.add(image.imagePath)
                }
            }
            
            progressCallback("Serializzazione dati...", 0.70f)
            
            // 4. Serializza tutto in JSON
            val categoriesJson = serializer.serializeCategories(categories)
            val articlesJson = serializer.serializeArticles(articles)
            val inventoryJson = serializer.serializeInventory(inventory)
            val movementsJson = serializer.serializeMovements(movements)
            val articleImagesJson = serializer.serializeArticleImages(articleImages)
            val displaySettingsJson = serializer.serializeDisplaySettings(
                serializer.mapDisplaySettings(displaySettings)
            )
            val recognitionSettingsJson = serializer.serializeRecognitionSettings(
                serializer.mapRecognitionSettings(recognitionSettings, currentPreset)
            )
            
            progressCallback("Calcolo checksum...", 0.80f)
            
            // 5. Calcola i checksum
            val checksums = BackupChecksums(
                categories = serializer.calculateChecksum(categoriesJson),
                articles = serializer.calculateChecksum(articlesJson),
                inventory = serializer.calculateChecksum(inventoryJson),
                movements = serializer.calculateChecksum(movementsJson),
                articleImages = serializer.calculateChecksum(articleImagesJson),
                displaySettings = serializer.calculateChecksum(displaySettingsJson),
                recognitionSettings = serializer.calculateChecksum(recognitionSettingsJson),
                imagesManifest = serializer.calculateChecksumForList(imagesManifest)
            )
            
            // 6. Crea i metadata
            val metadata = BackupMetadata(
                appVersion = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                dbVersion = QuickStoreDatabase.DATABASE_VERSION,
                backupDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}",
                counts = BackupCounts(
                    categories = categories.size,
                    articles = articles.size,
                    inventory = inventory.size,
                    movements = movements.size,
                    articleImages = articleImages.size,
                    imageFiles = imageFiles.size
                ),
                checksums = checksums,
                imagesManifest = imagesManifest
            )
            
            val metadataJson = serializer.serializeMetadata(metadata)
            
            progressCallback("Creazione file ZIP...", 0.90f)
            
            // 7. Crea il content provider per lo ZIP
            val contentProvider = object : BackupContentProvider {
                override fun getCategoriesJson() = categoriesJson
                override fun getArticlesJson() = articlesJson
                override fun getInventoryJson() = inventoryJson
                override fun getMovementsJson() = movementsJson
                override fun getArticleImagesJson() = articleImagesJson
                override fun getDisplaySettingsJson() = displaySettingsJson
                override fun getRecognitionSettingsJson() = recognitionSettingsJson
                override fun getMetadataJson() = metadataJson
                override fun getImageFiles() = imageFiles
            }
            
            // 8. Determina la directory di output
            val outputDir = options.destinationDir ?: zipManager.getDefaultBackupDir()
            val outputFile = File(outputDir, zipManager.generateBackupFileName())
            
            // 9. Crea il file ZIP
            val zipResult = zipManager.createBackupZip(
                outputFile = outputFile,
                compressionLevel = options.compressionLevel,
                contentProvider = contentProvider
            )
            
            progressCallback("Finalizzazione...", 0.95f)
            
            return zipResult.fold(
                onSuccess = { file ->
                    BackupResult.Success(
                        file = file,
                        metadata = metadata,
                        sizeBytes = file.length()
                    )
                },
                onFailure = { error ->
                    BackupResult.Error(error, BackupPhase.CREATING_ZIP)
                }
            )
            
        } catch (e: Exception) {
            return BackupResult.Error(e, BackupPhase.INITIALIZING)
        }
    }
    
    // ============================================
    // BACKUP RESTORE
    // ============================================
    
    override fun restoreBackup(backupFile: File, options: RestoreOptions): Flow<BackupProgress> = flow {
        emit(BackupProgress("Validazione backup...", 0.0f))
        
        try {
            val result = restoreBackupInternal(backupFile, options) { phase, progress ->
                emit(BackupProgress(phase, progress))
            }
            
            when (result) {
                is RestoreResult.Success -> emit(BackupProgress("Ripristino completato!", 1.0f))
                is RestoreResult.Error -> throw result.error
                is RestoreResult.Invalid -> throw Exception("Backup non valido: ${result.reason}")
            }
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    override fun restoreBackup(backupUri: Uri, options: RestoreOptions): Flow<BackupProgress> = flow {
        emit(BackupProgress("Copia file temporaneo...", 0.0f))
        
        // Copia in un file temporaneo
        val tempFile = File(context.cacheDir, "temp_restore_backup.zip")
        try {
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Impossibile aprire il file")
            
            restoreBackup(tempFile, options).collect { progress ->
                emit(progress)
            }
        } finally {
            tempFile.delete()
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun restoreBackupSync(backupFile: File, options: RestoreOptions): RestoreResult {
        return withContext(Dispatchers.IO) {
            restoreBackupInternal(backupFile, options) { _, _ -> }
        }
    }
    
    private suspend fun restoreBackupInternal(
        backupFile: File,
        options: RestoreOptions,
        progressCallback: suspend (String, Float) -> Unit
    ): RestoreResult {
        
        // 1. Valida la struttura dello ZIP
        progressCallback("Validazione struttura ZIP...", 0.05f)
        val structureError = zipManager.validateZipStructure(backupFile)
        if (structureError != null) {
            return RestoreResult.Invalid(structureError)
        }
        
        // 2. Leggi il contenuto dello ZIP
        progressCallback("Lettura backup...", 0.10f)
        val zipContent = zipManager.readBackupZip(backupFile).getOrElse { error ->
            return RestoreResult.Error(error, RestorePhase.READING_METADATA)
        }
        
        // 3. Deserializza i metadata
        progressCallback("Verifica metadata...", 0.15f)
        val metadata = try {
            serializer.deserializeMetadata(zipContent.metadataJson)
        } catch (_: Exception) {
            return RestoreResult.Invalid(ValidationError.MissingMetadata)
        }
        
        // 4. Verifica compatibilità versione DB
        if (metadata.dbVersion > QuickStoreDatabase.DATABASE_VERSION) {
            return RestoreResult.Invalid(
                ValidationError.IncompatibleVersion(
                    metadata.dbVersion,
                    QuickStoreDatabase.DATABASE_VERSION
                )
            )
        }
        
        // 5. Verifica checksums (opzionale)
        if (options.verifyChecksums) {
            progressCallback("Verifica integrità dati...", 0.20f)
            val checksumError = verifyChecksums(zipContent, metadata.checksums)
            if (checksumError != null) {
                return RestoreResult.Invalid(checksumError)
            }
        }
        
        // 6. Crea backup di sicurezza (opzionale)
        if (options.createBackupBeforeRestore) {
            progressCallback("Creazione backup di sicurezza...", 0.25f)
            // Non blocchiamo se fallisce, è solo precauzionale
            try {
                createBackupSync(BackupOptions())
            } catch (_: Exception) { }
        }
        
        // 7. Deserializza tutti i dati
        progressCallback("Deserializzazione dati...", 0.30f)
        val categories = serializer.deserializeCategories(zipContent.categoriesJson)
        val articles = serializer.deserializeArticles(zipContent.articlesJson)
        val inventory = serializer.deserializeInventory(zipContent.inventoryJson)
        val movements = serializer.deserializeMovements(zipContent.movementsJson)
        val articleImages = serializer.deserializeArticleImages(zipContent.articleImagesJson)
        val displaySettings = serializer.deserializeDisplaySettings(zipContent.displaySettingsJson)
        val recognitionSettings = serializer.deserializeRecognitionSettings(zipContent.recognitionSettingsJson)
        
        try {
            // 8. Svuota il database (in ordine FK)
            progressCallback("Pulizia database...", 0.40f)
            database.runInTransaction {
                // L'ordine è importante per le FK
                // Nota: con CASCADE le FK si occupano delle dipendenze,
                // ma per sicurezza cancelliamo in ordine
            }
            
            // Usa query dirette per svuotare
            clearAllTables()
            
            // 9. Elimina tutte le immagini esistenti
            progressCallback("Pulizia immagini...", 0.45f)
            clearAllImages()
            
            // 10. Inserisci i nuovi dati (in ordine FK)
            progressCallback("Ripristino categorie...", 0.50f)
            categories.forEach { category ->
                articleCategoryDao.insert(serializer.mapToCategory(category))
            }
            
            progressCallback("Ripristino articoli...", 0.55f)
            articles.forEach { article ->
                articleDao.insert(serializer.mapToArticle(article))
            }
            
            progressCallback("Ripristino inventario...", 0.60f)
            inventory.forEach { inv ->
                inventoryDao.insert(serializer.mapToInventory(inv))
            }
            
            progressCallback("Ripristino immagini articoli...", 0.65f)
            articleImages.forEach { image ->
                // Nota: non usiamo insertAll perché dobbiamo gestire gli ID
                // Room con autoGenerate gestirà i nuovi ID
                val entity = serializer.mapToArticleImage(image)
                // Impostiamo id=0 per far generare un nuovo ID
                articleImageDao.insert(entity.copy(id = 0))
            }
            
            progressCallback("Ripristino movimenti...", 0.70f)
            movements.forEach { movement ->
                movementDao.insert(serializer.mapToMovement(movement).copy(id = 0))
            }
            
            // 11. Ripristina i file delle immagini
            progressCallback("Ripristino file immagini...", 0.80f)
            zipContent.imageFiles.forEach { (relativePath, imageData) ->
                saveImageFile(relativePath, imageData)
            }
            
            // 12. Ripristina le impostazioni
            progressCallback("Ripristino impostazioni...", 0.90f)
            displaySettingsRepository.updateSettings(serializer.mapToDisplaySettings(displaySettings))
            recognitionSettingsRepository.updateSettings(serializer.mapToRecognitionSettings(recognitionSettings))
            
            // Se c'è un preset salvato, applicalo
            recognitionSettings.presetName?.let { preset ->
                try {
                    recognitionSettingsRepository.applyPreset(preset)
                } catch (_: Exception) { }
            }
            
            progressCallback("Finalizzazione...", 0.95f)
            
            return RestoreResult.Success(
                metadata = metadata,
                restoredCounts = metadata.counts
            )
            
        } catch (e: Exception) {
            // Tentativo di rollback
            return RestoreResult.Error(
                error = e,
                phase = RestorePhase.RESTORING_ARTICLES,
                rollbackSuccessful = false
            )
        }
    }
    
    // ============================================
    // VALIDATION
    // ============================================
    
    override suspend fun validateBackup(backupFile: File): Result<BackupMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val structureError = zipManager.validateZipStructure(backupFile)
                if (structureError != null) {
                    return@withContext Result.failure(Exception("Struttura ZIP non valida"))
                }
                
                val zipContent = zipManager.readBackupZip(backupFile).getOrThrow()
                val metadata = serializer.deserializeMetadata(zipContent.metadataJson)
                
                Result.success(metadata)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun validateBackup(backupUri: Uri): Result<BackupMetadata> {
        return withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_validate_backup.zip")
            try {
                context.contentResolver.openInputStream(backupUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(Exception("Impossibile aprire il file"))
                
                validateBackup(tempFile)
            } finally {
                tempFile.delete()
            }
        }
    }
    
    // ============================================
    // UTILITY
    // ============================================
    
    override suspend fun getAvailableBackups(): List<BackupFileInfo> {
        return withContext(Dispatchers.IO) {
            zipManager.listBackupFiles().map { file ->
                val metadata = try {
                    validateBackup(file).getOrNull()
                } catch (_: Exception) {
                    null
                }
                
                BackupFileInfo(
                    file = file,
                    metadata = metadata,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    isValid = metadata != null
                )
            }
        }
    }
    
    override suspend fun deleteBackup(backupFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                backupFile.delete()
            } catch (_: Exception) {
                false
            }
        }
    }
    
    override suspend fun estimateBackupSize(): Long {
        return withContext(Dispatchers.IO) {
            var estimatedSize = 0L
            
            // Stima dimensione JSON (approssimativa)
            val articlesCount = articleDao.count()
            val movementsCount = movementDao.getAllMovements().size
            val imagesCount = articleImageDao.getAll().size
            
            // ~500 bytes per articolo, ~200 per movimento, ~2KB per image record
            estimatedSize += articlesCount * 500L
            estimatedSize += movementsCount * 200L
            estimatedSize += imagesCount * 2048L
            
            // Aggiungi dimensione immagini
            estimatedSize += imageStorageManager.getUsedSpace()
            
            // Considera compressione (~50%)
            estimatedSize = (estimatedSize * 0.5).toLong()
            
            estimatedSize
        }
    }
    
    // ============================================
    // PRIVATE HELPERS
    // ============================================
    
    private fun verifyChecksums(content: BackupZipContent, expected: BackupChecksums): ValidationError? {
        if (!serializer.verifyChecksum(content.categoriesJson, expected.categories)) {
            return ValidationError.ChecksumMismatch("categories")
        }
        if (!serializer.verifyChecksum(content.articlesJson, expected.articles)) {
            return ValidationError.ChecksumMismatch("articles")
        }
        if (!serializer.verifyChecksum(content.inventoryJson, expected.inventory)) {
            return ValidationError.ChecksumMismatch("inventory")
        }
        if (!serializer.verifyChecksum(content.movementsJson, expected.movements)) {
            return ValidationError.ChecksumMismatch("movements")
        }
        if (!serializer.verifyChecksum(content.articleImagesJson, expected.articleImages)) {
            return ValidationError.ChecksumMismatch("articleImages")
        }
        if (!serializer.verifyChecksum(content.displaySettingsJson, expected.displaySettings)) {
            return ValidationError.ChecksumMismatch("displaySettings")
        }
        if (!serializer.verifyChecksum(content.recognitionSettingsJson, expected.recognitionSettings)) {
            return ValidationError.ChecksumMismatch("recognitionSettings")
        }
        return null
    }
    
    private fun clearAllTables() {
        // Nota: questo va dentro una transazione per sicurezza
        database.clearAllTables()
    }
    
    private fun clearAllImages() {
        val imagesDir = File(context.filesDir, "article_images")
        if (imagesDir.exists()) {
            imagesDir.deleteRecursively()
        }
    }
    
    private fun saveImageFile(relativePath: String, imageData: ByteArray) {
        val imagesDir = File(context.filesDir, "article_images")
        val imageFile = File(imagesDir, relativePath)
        imageFile.parentFile?.mkdirs()
        imageFile.writeBytes(imageData)
    }
}

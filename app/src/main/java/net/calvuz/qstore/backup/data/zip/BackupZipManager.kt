package net.calvuz.qstore.backup.data.zip

import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qstore.backup.domain.model.ValidationError
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager per la creazione e lettura di file ZIP di backup
 */
@Singleton
class BackupZipManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Struttura ZIP
        const val METADATA_FILE = "metadata.json"
        const val DATA_DIR = "data/"
        const val IMAGES_DIR = "images/"
        const val SETTINGS_DIR = "settings/"
        
        // File JSON
        const val CATEGORIES_FILE = "${DATA_DIR}categories.json"
        const val ARTICLES_FILE = "${DATA_DIR}articles.json"
        const val INVENTORY_FILE = "${DATA_DIR}inventory.json"
        const val MOVEMENTS_FILE = "${DATA_DIR}movements.json"
        const val ARTICLE_IMAGES_FILE = "${DATA_DIR}article_images.json"
        const val DISPLAY_SETTINGS_FILE = "${SETTINGS_DIR}display_settings.json"
        const val RECOGNITION_SETTINGS_FILE = "${SETTINGS_DIR}recognition_settings.json"
        
        // Pattern nome file
        private const val BACKUP_FILE_PREFIX = "qstore_backup_"
        private const val BACKUP_FILE_EXTENSION = ".zip"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
    }
    
    /**
     * Crea un nome file per il backup con timestamp
     */
    fun generateBackupFileName(): String {
        val timestamp = DATE_FORMAT.format(Date())
        return "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"
    }
    
    /**
     * Ottiene la directory di default per i backup (Downloads)
     */
    fun getDefaultBackupDir(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupDir = File(downloadsDir, "QStore")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }
    
    /**
     * Crea un file ZIP di backup
     */
    fun createBackupZip(
        outputFile: File,
        compressionLevel: Int = 6,
        contentProvider: BackupContentProvider
    ): Result<File> {
        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
                zipOut.setLevel(compressionLevel)
                
                // Aggiungi i file JSON
                addJsonEntry(zipOut, CATEGORIES_FILE, contentProvider.getCategoriesJson())
                addJsonEntry(zipOut, ARTICLES_FILE, contentProvider.getArticlesJson())
                addJsonEntry(zipOut, INVENTORY_FILE, contentProvider.getInventoryJson())
                addJsonEntry(zipOut, MOVEMENTS_FILE, contentProvider.getMovementsJson())
                addJsonEntry(zipOut, ARTICLE_IMAGES_FILE, contentProvider.getArticleImagesJson())
                
                // Aggiungi settings
                addJsonEntry(zipOut, DISPLAY_SETTINGS_FILE, contentProvider.getDisplaySettingsJson())
                addJsonEntry(zipOut, RECOGNITION_SETTINGS_FILE, contentProvider.getRecognitionSettingsJson())
                
                // Aggiungi immagini
                contentProvider.getImageFiles().forEach { (relativePath, imageData) ->
                    addBinaryEntry(zipOut, "$IMAGES_DIR$relativePath", imageData)
                }
                
                // Aggiungi metadata (ultimo, dopo aver calcolato tutti i checksum)
                addJsonEntry(zipOut, METADATA_FILE, contentProvider.getMetadataJson())
            }
            
            Result.success(outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        }
    }
    
    /**
     * Legge un file ZIP di backup
     */
    fun readBackupZip(backupFile: File): Result<BackupZipContent> {
        return try {
            ZipFile(backupFile).use { zipFile ->
                val content = BackupZipContent(
                    metadataJson = readZipEntry(zipFile, METADATA_FILE)
                        ?: return Result.failure(Exception("Missing metadata.json")),
                    categoriesJson = readZipEntry(zipFile, CATEGORIES_FILE)
                        ?: return Result.failure(Exception("Missing categories.json")),
                    articlesJson = readZipEntry(zipFile, ARTICLES_FILE)
                        ?: return Result.failure(Exception("Missing articles.json")),
                    inventoryJson = readZipEntry(zipFile, INVENTORY_FILE)
                        ?: return Result.failure(Exception("Missing inventory.json")),
                    movementsJson = readZipEntry(zipFile, MOVEMENTS_FILE)
                        ?: return Result.failure(Exception("Missing movements.json")),
                    articleImagesJson = readZipEntry(zipFile, ARTICLE_IMAGES_FILE)
                        ?: return Result.failure(Exception("Missing article_images.json")),
                    displaySettingsJson = readZipEntry(zipFile, DISPLAY_SETTINGS_FILE)
                        ?: return Result.failure(Exception("Missing display_settings.json")),
                    recognitionSettingsJson = readZipEntry(zipFile, RECOGNITION_SETTINGS_FILE)
                        ?: return Result.failure(Exception("Missing recognition_settings.json")),
                    imageFiles = readImageEntries(zipFile)
                )
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Legge un file ZIP da URI (per Document Picker)
     */
    fun readBackupZip(backupUri: Uri): Result<BackupZipContent> {
        return try {
            // Copia in un file temporaneo
            val tempFile = File(context.cacheDir, "temp_backup.zip")
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot open URI"))
            
            val result = readBackupZip(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Valida la struttura di un file ZIP di backup
     */
    fun validateZipStructure(backupFile: File): ValidationError? {
        return try {
            ZipFile(backupFile).use { zipFile ->
                val requiredFiles = listOf(
                    METADATA_FILE,
                    CATEGORIES_FILE,
                    ARTICLES_FILE,
                    INVENTORY_FILE,
                    MOVEMENTS_FILE,
                    ARTICLE_IMAGES_FILE,
                    DISPLAY_SETTINGS_FILE,
                    RECOGNITION_SETTINGS_FILE
                )
                
                val missingFiles = requiredFiles.filter { zipFile.getEntry(it) == null }
                if (missingFiles.isNotEmpty()) {
                    ValidationError.MissingDataFiles
                } else {
                    null // Valido
                }
            }
        } catch (e: Exception) {
            ValidationError.InvalidZipStructure
        }
    }
    
    /**
     * Lista tutti i file di backup nella directory di default
     */
    fun listBackupFiles(): List<File> {
        val backupDir = getDefaultBackupDir()
        return backupDir.listFiles { file ->
            file.isFile && 
            file.name.startsWith(BACKUP_FILE_PREFIX) && 
            file.name.endsWith(BACKUP_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    // ============================================
    // PRIVATE HELPERS
    // ============================================
    
    private fun addJsonEntry(zipOut: ZipOutputStream, entryName: String, json: String) {
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        zipOut.write(json.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }
    
    private fun addBinaryEntry(zipOut: ZipOutputStream, entryName: String, data: ByteArray) {
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }
    
    private fun readZipEntry(zipFile: ZipFile, entryName: String): String? {
        val entry = zipFile.getEntry(entryName) ?: return null
        return zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
    }
    
    private fun readImageEntries(zipFile: ZipFile): Map<String, ByteArray> {
        val images = mutableMapOf<String, ByteArray>()
        
        zipFile.entries().asSequence()
            .filter { it.name.startsWith(IMAGES_DIR) && !it.isDirectory }
            .forEach { entry ->
                val relativePath = entry.name.removePrefix(IMAGES_DIR)
                val data = zipFile.getInputStream(entry).use { it.readBytes() }
                images[relativePath] = data
            }
        
        return images
    }
}

/**
 * Provider di contenuti per la creazione del backup
 */
interface BackupContentProvider {
    fun getCategoriesJson(): String
    fun getArticlesJson(): String
    fun getInventoryJson(): String
    fun getMovementsJson(): String
    fun getArticleImagesJson(): String
    fun getDisplaySettingsJson(): String
    fun getRecognitionSettingsJson(): String
    fun getMetadataJson(): String
    fun getImageFiles(): Map<String, ByteArray> // relativePath -> imageData
}

/**
 * Contenuto di un file ZIP di backup
 */
data class BackupZipContent(
    val metadataJson: String,
    val categoriesJson: String,
    val articlesJson: String,
    val inventoryJson: String,
    val movementsJson: String,
    val articleImagesJson: String,
    val displaySettingsJson: String,
    val recognitionSettingsJson: String,
    val imageFiles: Map<String, ByteArray>
)

package net.calvuz.qstore.backup.data.repository

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.calvuz.qstore.app.data.local.database.QuickStoreDatabase
import net.calvuz.qstore.app.data.local.entity.ArticleCategoryEntity
import net.calvuz.qstore.app.data.local.entity.ArticleEntity
import net.calvuz.qstore.app.data.local.entity.ArticleImageEntity
import net.calvuz.qstore.app.data.local.entity.InventoryEntity
import net.calvuz.qstore.app.data.local.entity.LocationEntity
import net.calvuz.qstore.app.data.local.entity.MovementEntity
import net.calvuz.qstore.app.data.local.storage.ImageStorageManager
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.backup.data.serializer.BackupSerializer
import net.calvuz.qstore.backup.data.zip.BackupZipManager
import net.calvuz.qstore.backup.domain.model.BackupOptions
import net.calvuz.qstore.backup.domain.model.BackupResult
import net.calvuz.qstore.backup.domain.model.RestoreOptions
import net.calvuz.qstore.backup.domain.model.RestoreResult
import net.calvuz.qstore.backup.domain.model.ValidationError
import net.calvuz.qstore.backup.util.FakeDisplaySettingsRepository
import net.calvuz.qstore.backup.util.FakeRecognitionSettingsRepository
import net.calvuz.qstore.backup.util.IsolatedFilesDirContext
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.model.RecognitionSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Test end-to-end di [BackupRepositoryImpl]: DB Room reale in memoria + file system reale
 * (isolato dai dati veri dell'app tramite [IsolatedFilesDirContext], vedi commento lì) +
 * [BackupSerializer]/[BackupZipManager] reali. Le impostazioni usano fake in-memory per
 * evitare di dipendere da DataStore reale tra un test e l'altro.
 *
 * Copre: round trip completo backup->restore (dati + foto + impostazioni), rifiuto di un
 * backup con versione DB incompatibile, rifiuto per checksum non valido (e bypass quando
 * disattivato), rollback atomico del DB quando il ripristino fallisce a metà.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: QuickStoreDatabase
    private lateinit var testContext: IsolatedFilesDirContext
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var serializer: BackupSerializer
    private lateinit var zipManager: BackupZipManager
    private lateinit var displaySettingsRepository: FakeDisplaySettingsRepository
    private lateinit var recognitionSettingsRepository: FakeRecognitionSettingsRepository
    private lateinit var repository: BackupRepositoryImpl

    // Popolati da seedBaseData()
    private lateinit var locationUuid: String
    private lateinit var categoryUuid: String
    private lateinit var articleUuid1: String
    private lateinit var articleUuid2: String
    private lateinit var imageBytes: ByteArray
    private lateinit var savedImagePath: String

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        testContext = IsolatedFilesDirContext(
            base = appContext,
            isolatedFilesDir = tempFolder.newFolder("files"),
            isolatedCacheDir = tempFolder.newFolder("cache")
        )

        database = Room.inMemoryDatabaseBuilder(appContext, QuickStoreDatabase::class.java).build()
        imageStorageManager = ImageStorageManager(testContext)
        serializer = BackupSerializer()
        zipManager = BackupZipManager(testContext)
        displaySettingsRepository = FakeDisplaySettingsRepository(
            DisplaySettings(
                articleCardStyle = ArticleCardStyle.FULL,
                showStockIndicators = false,
                showArticleImages = false,
                showArticleActions = false,
                gridColumns = 2,
                showDashboardStats = false,
                showRecentMovements = false,
                showRecentArticles = false
            )
        )
        recognitionSettingsRepository = FakeRecognitionSettingsRepository(
            RecognitionSettings.getPresetPrecise()
        )

        repository = BackupRepositoryImpl(
            context = testContext,
            database = database,
            articleDao = database.articleDao(),
            articleCategoryDao = database.articleCategoryDao(),
            inventoryDao = database.inventoryDao(),
            movementDao = database.movementDao(),
            locationDao = database.locationDao(),
            articleImageDao = database.articleImageDao(),
            imageStorageManager = imageStorageManager,
            displaySettingsRepository = displaySettingsRepository,
            recognitionSettingsRepository = recognitionSettingsRepository,
            serializer = serializer,
            zipManager = zipManager
        )

        runBlocking { seedBaseData() }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedBaseData() {
        val now = System.currentTimeMillis()

        locationUuid = UUID.randomUUID().toString()
        database.locationDao().insert(
            LocationEntity(locationUuid, "Sede", "", now, now)
        )

        categoryUuid = UUID.randomUUID().toString()
        database.articleCategoryDao().insert(
            ArticleCategoryEntity(categoryUuid, "Elettrica", "desc", "note", now, now)
        )

        articleUuid1 = UUID.randomUUID().toString()
        database.articleDao().insert(
            ArticleEntity(
                uuid = articleUuid1,
                name = "Cuscinetto 6205",
                description = "cuscinetto a sfere",
                categoryId = categoryUuid,
                unitOfMeasure = "pz",
                reorderLevel = 5.0,
                notes = "scaffale A3",
                codeOEM = "OEM-1",
                codeERP = "ERP-1",
                codeBM = "BM-1",
                createdAt = now,
                updatedAt = now
            )
        )
        articleUuid2 = UUID.randomUUID().toString()
        database.articleDao().insert(
            ArticleEntity(
                uuid = articleUuid2,
                name = "Vite M6x20",
                description = "",
                categoryId = categoryUuid,
                unitOfMeasure = "pz",
                reorderLevel = 0.0,
                notes = "",
                createdAt = now,
                updatedAt = now
            )
        )

        database.inventoryDao().insert(
            InventoryEntity(articleUuid1, locationUuid, currentQuantity = 12.0, lastMovementAt = now)
        )
        database.inventoryDao().insert(
            InventoryEntity(articleUuid2, locationUuid, currentQuantity = 3.5, lastMovementAt = now)
        )

        database.movementDao().insert(
            MovementEntity(
                id = UUID.randomUUID().toString(),
                articleUuid = articleUuid1,
                type = MovementType.IN,
                fromLocationUuid = null,
                toLocationUuid = locationUuid,
                quantity = 12.0,
                notes = "carico iniziale",
                createdAt = now
            )
        )
        database.movementDao().insert(
            MovementEntity(
                id = UUID.randomUUID().toString(),
                articleUuid = articleUuid2,
                type = MovementType.OUT,
                fromLocationUuid = locationUuid,
                toLocationUuid = null,
                quantity = 1.0,
                notes = "scarico test",
                createdAt = now + 1
            )
        )

        // Immagine reale su disco (isolata, vedi IsolatedFilesDirContext) tramite lo stesso
        // ImageStorageManager usato in produzione, cosi il path relativo è quello vero.
        // saveImage() decodifica e ricomprime il JPEG (anche a qualità 100 non è garantito
        // bit-identico all'input) — imageBytes deve quindi essere quanto è REALMENTE finito
        // su disco, non l'input pre-salvataggio, altrimenti il confronto post-restore fallisce
        // per un motivo estraneo al backup.
        savedImagePath = imageStorageManager.saveImage(tinyJpegBytes(), articleUuid1).getOrThrow()
        imageBytes = imageStorageManager.readImage(savedImagePath).getOrThrow()

        database.articleImageDao().insert(
            ArticleImageEntity(
                uuid = UUID.randomUUID().toString(),
                articleUuid = articleUuid1,
                imagePath = savedImagePath,
                featuresData = byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -128, 127),
                createdAt = now
            )
        )
    }

    private fun tinyJpegBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    // ============================================
    // CREATE BACKUP
    // ============================================

    @Test
    fun createBackupSync_producesValidZipWithMatchingCounts() = runBlocking {
        val outputDir = tempFolder.newFolder("backups")
        val result = repository.createBackupSync(BackupOptions(destinationDir = outputDir))

        assertTrue("expected Success but was $result", result is BackupResult.Success)
        result as BackupResult.Success

        assertTrue(result.file.exists())
        assertEquals(outputDir, result.file.parentFile)
        assertTrue(result.sizeBytes > 0)

        val counts = result.metadata.counts
        assertEquals(1, counts.categories)
        assertEquals(2, counts.articles)
        assertEquals(2, counts.inventory)
        assertEquals(2, counts.movements)
        assertEquals(1, counts.articleImages)
        assertEquals(1, counts.imageFiles)
        assertEquals(QuickStoreDatabase.DATABASE_VERSION, result.metadata.dbVersion)
    }

    @Test
    fun validateBackup_onFreshlyCreatedFile_returnsMatchingMetadata() = runBlocking {
        val backupFile = createBackup()

        val validation = repository.validateBackup(backupFile)

        assertTrue(validation.isSuccess)
        assertEquals(2, validation.getOrThrow().counts.articles)
    }

    @Test
    fun validateBackup_onCorruptFile_fails() = runBlocking {
        val corrupt = tempFolder.newFile("corrupt.zip")
        corrupt.writeBytes(byteArrayOf(1, 2, 3))

        val validation = repository.validateBackup(corrupt)

        assertTrue(validation.isFailure)
    }

    // ============================================
    // FULL ROUND TRIP
    // ============================================

    @Test
    fun restoreBackupSync_fullRoundTrip_restoresAllDataAndImageFile() = runBlocking {
        val backupFile = createBackup()

        val result = repository.restoreBackupSync(
            backupFile,
            RestoreOptions(createBackupBeforeRestore = false)
        )

        assertTrue("expected Success but was $result", result is RestoreResult.Success)
        result as RestoreResult.Success
        assertEquals(2, result.restoredCounts.articles)

        // Categorie e articoli mantengono il loro UUID originale (il formato di backup li
        // porta esplicitamente) — verificabile con una getByUuid diretta.
        val restoredCategory = database.articleCategoryDao().getByUuid(categoryUuid)
        assertNotNull(restoredCategory)
        assertEquals("Elettrica", restoredCategory!!.name)

        val restoredArticle1 = database.articleDao().getByUuid(articleUuid1)
        assertNotNull(restoredArticle1)
        assertEquals("Cuscinetto 6205", restoredArticle1!!.name)
        assertEquals("OEM-1", restoredArticle1.codeOEM)
        assertEquals(5.0, restoredArticle1.reorderLevel, 0.0)

        val restoredArticle2 = database.articleDao().getByUuid(articleUuid2)
        assertNotNull(restoredArticle2)

        // L'ubicazione non è portata dal formato di backup (limite noto, vedi CLAUDE.md):
        // dopo il restore ne esiste esattamente una, ricreata come "Magazzino principale",
        // con un UUID diverso da quello originale.
        val locationsAfterRestore = database.locationDao().getAll()
        assertEquals(1, locationsAfterRestore.size)
        val newLocationUuid = locationsAfterRestore.first().uuid
        assertEquals("Magazzino principale", locationsAfterRestore.first().name)
        assertNotEquals(locationUuid, newLocationUuid)

        // L'inventario è riassegnato alla nuova ubicazione ma la quantità è preservata.
        val inventory1 = database.inventoryDao().getByArticleUuid(articleUuid1)
        assertEquals(1, inventory1.size)
        assertEquals(newLocationUuid, inventory1.first().locationUuid)
        assertEquals(12.0, inventory1.first().currentQuantity, 0.0)

        val inventory2 = database.inventoryDao().getByArticleUuid(articleUuid2)
        assertEquals(3.5, inventory2.first().currentQuantity, 0.0)

        // I movimenti mantengono tipo/quantità/note/data ma non l'id originale (era un Long
        // autogenerato non rappresentabile, vedi TODO in BackupSerializer) né la vecchia
        // ubicazione.
        val movements1 = database.movementDao().getByArticleUuid(articleUuid1)
        assertEquals(1, movements1.size)
        assertEquals(MovementType.IN, movements1.first().type)
        assertEquals(newLocationUuid, movements1.first().toLocationUuid)
        assertNull(movements1.first().fromLocationUuid)
        assertEquals(12.0, movements1.first().quantity, 0.0)
        assertEquals("carico iniziale", movements1.first().notes)

        val movements2 = database.movementDao().getByArticleUuid(articleUuid2)
        assertEquals(MovementType.OUT, movements2.first().type)
        assertEquals(newLocationUuid, movements2.first().fromLocationUuid)

        // Immagine: metadati + features binarie + file JPEG fisico, tutti ripristinati.
        val restoredImages = database.articleImageDao().getByArticleUuid(articleUuid1)
        assertEquals(1, restoredImages.size)
        assertEquals(savedImagePath, restoredImages.first().imagePath)
        assertTrue(imageStorageManager.imageExists(savedImagePath))
        val restoredImageBytes = imageStorageManager.readImage(savedImagePath).getOrThrow()
        assertTrue(imageBytes.contentEquals(restoredImageBytes))

        // Impostazioni display/riconoscimento, incluse le flag più recenti (showArticleActions
        // etc.) che il formato di backup non copriva fino a questo fix.
        val restoredDisplay = displaySettingsRepository.current()
        assertEquals(ArticleCardStyle.FULL, restoredDisplay.articleCardStyle)
        assertEquals(false, restoredDisplay.showStockIndicators)
        assertEquals(false, restoredDisplay.showArticleActions)
        assertEquals(false, restoredDisplay.showDashboardStats)
        assertEquals(false, restoredDisplay.showRecentMovements)
        assertEquals(false, restoredDisplay.showRecentArticles)
        assertEquals(2, restoredDisplay.gridColumns)

        val restoredRecognition = recognitionSettingsRepository.current()
        assertEquals(RecognitionSettings.getPresetPrecise().minFeatures, restoredRecognition.minFeatures)
    }

    @Test
    fun restoreBackupSync_clearsPreviousDataBeforeRestoring() = runBlocking {
        val backupFile = createBackup()

        // Aggiunge un articolo "spurio" dopo il backup, per verificare che il restore lo
        // rimuova invece di limitarsi ad aggiungere i dati del backup sopra quelli esistenti.
        val extraUuid = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.articleDao().insert(
            ArticleEntity(
                uuid = extraUuid, name = "Articolo spurio", categoryId = categoryUuid,
                unitOfMeasure = "pz", createdAt = now, updatedAt = now
            )
        )

        repository.restoreBackupSync(backupFile, RestoreOptions(createBackupBeforeRestore = false))

        assertNull(database.articleDao().getByUuid(extraUuid))
        assertEquals(2, database.articleDao().getAll().size)
    }

    // ============================================
    // VERSION / CHECKSUM VALIDATION
    // ============================================

    @Test
    fun restoreBackupSync_rejectsIncompatibleFutureDbVersion() = runBlocking {
        val backupFile = buildCustomZip(dbVersionOverride = QuickStoreDatabase.DATABASE_VERSION + 1)

        val result = repository.restoreBackupSync(backupFile, RestoreOptions(createBackupBeforeRestore = false))

        assertTrue(result is RestoreResult.Invalid)
        val reason = (result as RestoreResult.Invalid).reason
        assertTrue(reason is ValidationError.IncompatibleVersion)
        reason as ValidationError.IncompatibleVersion
        assertEquals(QuickStoreDatabase.DATABASE_VERSION + 1, reason.backupVersion)
        assertEquals(QuickStoreDatabase.DATABASE_VERSION, reason.currentVersion)

        // Un backup rifiutato non deve toccare i dati esistenti.
        assertEquals(2, database.articleDao().getAll().size)
    }

    @Test
    fun restoreBackupSync_rejectsTamperedContentWhenChecksumVerificationEnabled() = runBlocking {
        val backupFile = createBackup()
        tamperCategoriesEntry(backupFile)

        val result = repository.restoreBackupSync(
            backupFile,
            RestoreOptions(verifyChecksums = true, createBackupBeforeRestore = false)
        )

        assertTrue(result is RestoreResult.Invalid)
        assertTrue((result as RestoreResult.Invalid).reason is ValidationError.ChecksumMismatch)
        // I dati originali restano intatti: il restore non è mai partito.
        assertEquals(2, database.articleDao().getAll().size)
    }

    @Test
    fun restoreBackupSync_acceptsTamperedContentWhenChecksumVerificationDisabled() = runBlocking {
        val backupFile = createBackup()
        tamperCategoriesEntry(backupFile)

        val result = repository.restoreBackupSync(
            backupFile,
            RestoreOptions(verifyChecksums = false, createBackupBeforeRestore = false)
        )

        assertTrue("expected Success but was $result", result is RestoreResult.Success)
        val tamperedCategory = database.articleCategoryDao().getByUuid(categoryUuid)
        assertEquals("TAMPERED", tamperedCategory?.name)
    }

    // ============================================
    // ATOMICITY / ROLLBACK
    // ============================================

    @Test
    fun restoreBackupSync_rollsBackEntirelyWhenAnInsertFailsMidway() = runBlocking {
        // Backup con due articoli che condividono lo stesso UUID: il secondo insert viola
        // il vincolo di chiave primaria (ArticleDao usa OnConflictStrategy.ABORT) e deve far
        // fallire l'intero ripristino, non solo quell'articolo.
        val backupFile = buildCustomZip(duplicateArticleUuid = true)

        val result = repository.restoreBackupSync(backupFile, RestoreOptions(createBackupBeforeRestore = false))

        assertTrue("expected Error but was $result", result is RestoreResult.Error)

        // Il DB deve essere tornato esattamente allo stato pre-restore (grazie a
        // database.withTransaction), non restare con un ripristino a metà.
        assertEquals(2, database.articleDao().getAll().size)
        assertNotNull(database.articleDao().getByUuid(articleUuid1))
        assertNotNull(database.articleDao().getByUuid(articleUuid2))
        assertEquals(1, database.locationDao().getAll().size)
        assertEquals(locationUuid, database.locationDao().getAll().first().uuid)
        assertEquals(2, database.inventoryDao().getAll().size)
    }

    // ============================================
    // HELPERS
    // ============================================

    private suspend fun createBackup(): File {
        val result = repository.createBackupSync(
            BackupOptions(destinationDir = tempFolder.newFolder("backup-${UUID.randomUUID()}"))
        )
        check(result is BackupResult.Success) { "setup failed to create backup: $result" }
        return result.file
    }

    /**
     * Costruisce uno ZIP di backup "a mano" (bypassando il repository) con checksum coerenti,
     * per testare i percorsi di validazione senza dover corrompere un file reale.
     */
    private fun buildCustomZip(
        dbVersionOverride: Int = QuickStoreDatabase.DATABASE_VERSION,
        duplicateArticleUuid: Boolean = false
    ): File {
        val now = System.currentTimeMillis()
        val categories = listOf(serializer.mapCategory(ArticleCategoryEntity(categoryUuid, "Elettrica", "", "", now, now)))
        val articles = if (duplicateArticleUuid) {
            listOf(
                serializer.mapArticle(ArticleEntity(articleUuid1, "A", categoryId = categoryUuid, unitOfMeasure = "pz", createdAt = now, updatedAt = now)),
                serializer.mapArticle(ArticleEntity(articleUuid1, "B", categoryId = categoryUuid, unitOfMeasure = "pz", createdAt = now, updatedAt = now))
            )
        } else {
            listOf(serializer.mapArticle(ArticleEntity(articleUuid1, "A", categoryId = categoryUuid, unitOfMeasure = "pz", createdAt = now, updatedAt = now)))
        }

        val categoriesJson = serializer.serializeCategories(categories)
        val articlesJson = serializer.serializeArticles(articles)
        val inventoryJson = serializer.serializeInventory(emptyList())
        val movementsJson = serializer.serializeMovements(emptyList())
        val articleImagesJson = serializer.serializeArticleImages(emptyList())
        val displaySettingsJson = serializer.serializeDisplaySettings(serializer.mapDisplaySettings(DisplaySettings.getDefault()))
        val recognitionSettingsJson = serializer.serializeRecognitionSettings(serializer.mapRecognitionSettings(RecognitionSettings.getDefault(), null))

        val metadata = net.calvuz.qstore.backup.domain.model.BackupMetadata(
            appVersion = "test",
            appVersionCode = 1,
            dbVersion = dbVersionOverride,
            backupDate = "2026-01-01T00:00:00Z",
            counts = net.calvuz.qstore.backup.domain.model.BackupCounts(
                categories.size, articles.size, 0, 0, 0, 0
            ),
            checksums = net.calvuz.qstore.backup.domain.model.BackupChecksums(
                categories = serializer.calculateChecksum(categoriesJson),
                articles = serializer.calculateChecksum(articlesJson),
                inventory = serializer.calculateChecksum(inventoryJson),
                movements = serializer.calculateChecksum(movementsJson),
                articleImages = serializer.calculateChecksum(articleImagesJson),
                displaySettings = serializer.calculateChecksum(displaySettingsJson),
                recognitionSettings = serializer.calculateChecksum(recognitionSettingsJson),
                imagesManifest = serializer.calculateChecksumForList(emptyList())
            ),
            imagesManifest = emptyList()
        )

        val outputFile = File(tempFolder.newFolder("custom-${UUID.randomUUID()}"), "custom.zip")
        val zipResult = zipManager.createBackupZip(
            outputFile = outputFile,
            contentProvider = object : net.calvuz.qstore.backup.data.zip.BackupContentProvider {
                override fun getCategoriesJson() = categoriesJson
                override fun getArticlesJson() = articlesJson
                override fun getInventoryJson() = inventoryJson
                override fun getMovementsJson() = movementsJson
                override fun getArticleImagesJson() = articleImagesJson
                override fun getDisplaySettingsJson() = displaySettingsJson
                override fun getRecognitionSettingsJson() = recognitionSettingsJson
                override fun getMetadataJson() = serializer.serializeMetadata(metadata)
                override fun getImageFiles() = emptyMap<String, ByteArray>()
            }
        )
        return zipResult.getOrThrow()
    }

    /** Riscrive l'entry categories.json dentro uno ZIP esistente con un contenuto diverso, lasciando il resto invariato (incluso metadata.json con il vecchio checksum). */
    private fun tamperCategoriesEntry(zipFile: File) {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(zipFile.inputStream()).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                entries[entry.name] = input.readBytes()
                entry = input.nextEntry
            }
        }

        entries[BackupZipManager.CATEGORIES_FILE] = """[{"uuid":"$categoryUuid","name":"TAMPERED","description":"","notes":"","createdAt":0,"updatedAt":0}]""".toByteArray()

        ZipOutputStream(zipFile.outputStream()).use { output ->
            entries.forEach { (name, bytes) ->
                output.putNextEntry(ZipEntry(name))
                output.write(bytes)
                output.closeEntry()
            }
        }
    }
}

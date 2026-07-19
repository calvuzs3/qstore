package net.calvuz.qstore.backup.data.zip

import android.content.Context
import io.mockk.mockk
import net.calvuz.qstore.backup.domain.model.ValidationError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Verifica [BackupZipManager] su file reali (via [TemporaryFolder]): round-trip
 * scrittura/lettura dello ZIP, e i casi di validazione (struttura mancante, file corrotto).
 *
 * Il Context non è mai toccato dai metodi testati qui (createBackupZip, readBackupZip(File),
 * validateZipStructure) — un mock "relaxed" basta, non serve Robolectric.
 */
class BackupZipManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var zipManager: BackupZipManager

    private fun fakeContentProvider(
        images: Map<String, ByteArray> = emptyMap()
    ) = object : BackupContentProvider {
        override fun getCategoriesJson() = """[{"uuid":"c1"}]"""
        override fun getArticlesJson() = """[{"uuid":"a1"}]"""
        override fun getInventoryJson() = """[{"articleUuid":"a1"}]"""
        override fun getMovementsJson() = """[{"articleUuid":"a1"}]"""
        override fun getArticleImagesJson() = """[{"uuid":"i1"}]"""
        override fun getDisplaySettingsJson() = """{"articleCardStyle":"FULL"}"""
        override fun getRecognitionSettingsJson() = """{"minFeatures":30}"""
        override fun getMetadataJson() = """{"appVersion":"1.0"}"""
        override fun getImageFiles() = images
    }

    @Before
    fun setUp() {
        zipManager = BackupZipManager(mockk<Context>(relaxed = true))
    }

    @Test
    fun `createBackupZip then readBackupZip round trips json and image bytes`() {
        val outputFile = File(tempFolder.root, "backup.zip")
        val provider = fakeContentProvider(
            images = mapOf(
                "a1/photo1.jpg" to byteArrayOf(1, 2, 3),
                "a1/sub/photo2.jpg" to byteArrayOf(4, 5, 6, 7)
            )
        )

        val createResult = zipManager.createBackupZip(outputFile, contentProvider = provider)
        assertTrue(createResult.isSuccess)
        assertTrue(outputFile.exists())

        val readResult = zipManager.readBackupZip(outputFile)
        assertTrue(readResult.isSuccess)

        val content = readResult.getOrThrow()
        assertEquals(provider.getCategoriesJson(), content.categoriesJson)
        assertEquals(provider.getArticlesJson(), content.articlesJson)
        assertEquals(provider.getInventoryJson(), content.inventoryJson)
        assertEquals(provider.getMovementsJson(), content.movementsJson)
        assertEquals(provider.getArticleImagesJson(), content.articleImagesJson)
        assertEquals(provider.getDisplaySettingsJson(), content.displaySettingsJson)
        assertEquals(provider.getRecognitionSettingsJson(), content.recognitionSettingsJson)
        assertEquals(provider.getMetadataJson(), content.metadataJson)

        assertEquals(2, content.imageFiles.size)
        assertArrayEquals(byteArrayOf(1, 2, 3), content.imageFiles["a1/photo1.jpg"])
        assertArrayEquals(byteArrayOf(4, 5, 6, 7), content.imageFiles["a1/sub/photo2.jpg"])
    }

    @Test
    fun `createBackupZip with no images produces empty image map on read`() {
        val outputFile = File(tempFolder.root, "no-images.zip")
        zipManager.createBackupZip(outputFile, contentProvider = fakeContentProvider())

        val content = zipManager.readBackupZip(outputFile).getOrThrow()
        assertTrue(content.imageFiles.isEmpty())
    }

    @Test
    fun `createBackupZip deletes partial output file on failure`() {
        // outputFile punta dentro una directory inesistente: FileOutputStream fallisce subito.
        val outputFile = File(tempFolder.root, "missing-dir/backup.zip")

        val result = zipManager.createBackupZip(outputFile, contentProvider = fakeContentProvider())

        assertTrue(result.isFailure)
        assertTrue(!outputFile.exists())
    }

    @Test
    fun `readBackupZip fails when metadata entry is missing`() {
        val zipFile = File(tempFolder.root, "no-metadata.zip")
        ZipOutputStream(zipFile.outputStream()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(BackupZipManager.CATEGORIES_FILE))
            zipOut.write("[]".toByteArray())
            zipOut.closeEntry()
        }

        val result = zipManager.readBackupZip(zipFile)
        assertTrue(result.isFailure)
    }

    @Test
    fun `readBackupZip fails on a non-zip file`() {
        val notAZip = tempFolder.newFile("not-a-zip.zip")
        notAZip.writeText("this is definitely not a zip archive")

        val result = zipManager.readBackupZip(notAZip)
        assertTrue(result.isFailure)
    }

    @Test
    fun `validateZipStructure returns null for a complete backup`() {
        val outputFile = File(tempFolder.root, "valid.zip")
        zipManager.createBackupZip(outputFile, contentProvider = fakeContentProvider())

        assertNull(zipManager.validateZipStructure(outputFile))
    }

    @Test
    fun `validateZipStructure reports MissingDataFiles when an entry is absent`() {
        val zipFile = File(tempFolder.root, "incomplete.zip")
        ZipOutputStream(zipFile.outputStream()).use { zipOut ->
            // Solo metadata.json — mancano tutti i data/*.json e settings/*.json richiesti.
            zipOut.putNextEntry(ZipEntry(BackupZipManager.METADATA_FILE))
            zipOut.write("{}".toByteArray())
            zipOut.closeEntry()
        }

        val error = zipManager.validateZipStructure(zipFile)
        assertEquals(ValidationError.MissingDataFiles, error)
    }

    @Test
    fun `validateZipStructure reports InvalidZipStructure for a corrupt file`() {
        val corruptFile = tempFolder.newFile("corrupt.zip")
        corruptFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))

        val error = zipManager.validateZipStructure(corruptFile)
        assertEquals(ValidationError.InvalidZipStructure, error)
    }

    @Test
    fun `validateZipStructure reports InvalidZipStructure for a nonexistent file`() {
        val error = zipManager.validateZipStructure(File(tempFolder.root, "does-not-exist.zip"))
        assertEquals(ValidationError.InvalidZipStructure, error)
    }

    @Test
    fun `generateBackupFileName has expected prefix, extension and timestamp pattern`() {
        val name = zipManager.generateBackupFileName()

        assertTrue(name.startsWith("qstore_backup_"))
        assertTrue(name.endsWith(".zip"))
        val timestamp = name.removePrefix("qstore_backup_").removeSuffix(".zip")
        assertTrue(
            "unexpected timestamp format: $timestamp",
            timestamp.matches(Regex("""\d{4}-\d{2}-\d{2}_\d{6}"""))
        )
    }
}

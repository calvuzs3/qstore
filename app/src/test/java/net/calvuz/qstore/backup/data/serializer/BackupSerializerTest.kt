package net.calvuz.qstore.backup.data.serializer

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import net.calvuz.qstore.app.data.local.entity.ArticleCategoryEntity
import net.calvuz.qstore.app.data.local.entity.ArticleEntity
import net.calvuz.qstore.app.data.local.entity.ArticleImageEntity
import net.calvuz.qstore.app.data.local.entity.InventoryEntity
import net.calvuz.qstore.app.data.local.entity.MovementEntity
import net.calvuz.qstore.app.domain.model.enum.MovementType
import net.calvuz.qstore.backup.domain.model.ArticleBackup
import net.calvuz.qstore.backup.domain.model.ArticleImageBackup
import net.calvuz.qstore.backup.domain.model.BackupChecksums
import net.calvuz.qstore.backup.domain.model.BackupCounts
import net.calvuz.qstore.backup.domain.model.BackupMetadata
import net.calvuz.qstore.backup.domain.model.CategoryBackup
import net.calvuz.qstore.backup.domain.model.DisplaySettingsBackup
import net.calvuz.qstore.backup.domain.model.InventoryBackup
import net.calvuz.qstore.backup.domain.model.MovementBackup
import net.calvuz.qstore.backup.domain.model.RecognitionSettingsBackup
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.model.RecognitionSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Verifica che [BackupSerializer] preservi fedelmente i dati attraverso mapping
 * entity<->backup, round-trip JSON, e che i checksum si comportino in modo corretto
 * (deterministici, sensibili a qualunque modifica del contenuto).
 */
class BackupSerializerTest {

    private val serializer = BackupSerializer()

    @Before
    fun setUp() {
        // android.util.Base64 non ha un corpo reale nello stub jar usato dai test JVM —
        // deleghiamo a java.util.Base64 (stesso alfabeto standard) per far funzionare
        // mapArticleImage/mapToArticleImage senza Robolectric.
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    // ============================================
    // ENTITY -> BACKUP -> ENTITY (round trip)
    // ============================================

    @Test
    fun `mapCategory preserves all fields`() {
        val entity = ArticleCategoryEntity(
            uuid = "cat-1",
            name = "Elettrica",
            description = "Materiale elettrico",
            notes = "note",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val backup = serializer.mapCategory(entity)

        assertEquals(entity.uuid, backup.uuid)
        assertEquals(entity.name, backup.name)
        assertEquals(entity.description, backup.description)
        assertEquals(entity.notes, backup.notes)
        assertEquals(entity.createdAt, backup.createdAt)
        assertEquals(entity.updatedAt, backup.updatedAt)

        val restored = serializer.mapToCategory(backup)
        assertEquals(entity, restored)
    }

    @Test
    fun `mapArticle preserves all fields including external codes`() {
        val entity = ArticleEntity(
            uuid = "art-1",
            name = "Cuscinetto",
            description = "6205-2RS",
            categoryId = "cat-1",
            unitOfMeasure = "pz",
            reorderLevel = 5.0,
            notes = "scaffale A3",
            codeOEM = "OEM123",
            codeERP = "ERP456",
            codeBM = "BM789",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val backup = serializer.mapArticle(entity)
        val restored = serializer.mapToArticle(backup)

        // mapToArticle non riceve isDeleted dal backup (non fa parte del formato) — un
        // articolo cancellato non finisce comunque nel backup lato repository, quindi il
        // confronto va fatto sui soli campi che il formato porta con sé.
        assertEquals(entity.uuid, restored.uuid)
        assertEquals(entity.name, restored.name)
        assertEquals(entity.description, restored.description)
        assertEquals(entity.categoryId, restored.categoryId)
        assertEquals(entity.unitOfMeasure, restored.unitOfMeasure)
        assertEquals(entity.reorderLevel, restored.reorderLevel, 0.0)
        assertEquals(entity.notes, restored.notes)
        assertEquals(entity.codeOEM, restored.codeOEM)
        assertEquals(entity.codeERP, restored.codeERP)
        assertEquals(entity.codeBM, restored.codeBM)
        assertEquals(entity.createdAt, restored.createdAt)
        assertEquals(entity.updatedAt, restored.updatedAt)
    }

    @Test
    fun `mapInventory preserves quantity and last movement timestamp`() {
        val entity = InventoryEntity(
            articleUuid = "art-1",
            locationUuid = "loc-1",
            currentQuantity = 3.5,
            lastMovementAt = 5000L
        )

        val backup = serializer.mapInventory(entity)
        assertEquals(entity.articleUuid, backup.articleUuid)
        assertEquals(entity.currentQuantity, backup.currentQuantity, 0.0)
        assertEquals(entity.lastMovementAt, backup.lastMovementAt)

        // mapToInventory riassegna l'ubicazione al momento del restore (il formato di
        // backup non porta ancora le ubicazioni, vedi commento in BackupSerializer).
        val restored = serializer.mapToInventory(backup, "loc-restored")
        assertEquals("loc-restored", restored.locationUuid)
        assertEquals(entity.currentQuantity, restored.currentQuantity, 0.0)
        assertEquals(entity.lastMovementAt, restored.lastMovementAt)
    }

    @Test
    fun `mapMovement IN assigns only toLocationUuid on restore`() {
        val entity = MovementEntity(
            id = "mov-1",
            articleUuid = "art-1",
            type = MovementType.IN,
            fromLocationUuid = null,
            toLocationUuid = "loc-original",
            quantity = 2.0,
            notes = "carico iniziale",
            createdAt = 1000L
        )

        val backup = serializer.mapMovement(entity)
        assertEquals(entity.articleUuid, backup.articleUuid)
        assertEquals(entity.type.name, backup.type)
        assertEquals(entity.quantity, backup.quantity, 0.0)
        assertEquals(entity.notes, backup.notes)
        assertEquals(entity.createdAt, backup.createdAt)

        val restored = serializer.mapToMovement(backup, "loc-restored")
        assertEquals(MovementType.IN, restored.type)
        assertEquals("loc-restored", restored.toLocationUuid)
        assertNull(restored.fromLocationUuid)
        // L'id originale non è preservato dal formato attuale (era un Long autogenerato,
        // l'entity ora usa UUID) — deve comunque essere un id valido e non vuoto.
        assertTrue(restored.id.isNotBlank())
    }

    @Test
    fun `mapMovement OUT assigns only fromLocationUuid on restore`() {
        val entity = MovementEntity(
            id = "mov-2",
            articleUuid = "art-1",
            type = MovementType.OUT,
            fromLocationUuid = "loc-original",
            toLocationUuid = null,
            quantity = 1.0,
            notes = "",
            createdAt = 1500L
        )

        val backup = serializer.mapMovement(entity)
        val restored = serializer.mapToMovement(backup, "loc-restored")

        assertEquals(MovementType.OUT, restored.type)
        assertEquals("loc-restored", restored.fromLocationUuid)
        assertNull(restored.toLocationUuid)
    }

    @Test
    fun `mapArticleImage round trip preserves binary features via Base64`() {
        val featuresBytes = byteArrayOf(1, 2, 3, 4, 5, -1, -128, 127)
        val entity = ArticleImageEntity(
            uuid = "img-1",
            articleUuid = "art-1",
            imagePath = "art-1/photo.jpg",
            featuresData = featuresBytes,
            createdAt = 1000L
        )

        val backup = serializer.mapArticleImage(entity)
        assertEquals(entity.uuid, backup.uuid)
        assertEquals(entity.articleUuid, backup.articleUuid)
        assertEquals(entity.imagePath, backup.imagePath)
        assertEquals(entity.createdAt, backup.createdAt)

        val restored = serializer.mapToArticleImage(backup)
        assertTrue(featuresBytes.contentEquals(restored.featuresData))
        assertEquals(entity.uuid, restored.uuid)
        assertEquals(entity.articleUuid, restored.articleUuid)
        assertEquals(entity.imagePath, restored.imagePath)
        assertEquals(entity.createdAt, restored.createdAt)
    }

    @Test
    fun `mapArticleImage handles empty features array`() {
        val entity = ArticleImageEntity(
            uuid = "img-2",
            articleUuid = "art-1",
            imagePath = "art-1/empty.jpg",
            featuresData = ByteArray(0),
            createdAt = 1000L
        )

        val backup = serializer.mapArticleImage(entity)
        val restored = serializer.mapToArticleImage(backup)

        assertTrue(restored.featuresData.isEmpty())
    }

    @Test
    fun `mapDisplaySettings and mapToDisplaySettings round trip all flags`() {
        val settings = DisplaySettings(
            articleCardStyle = ArticleCardStyle.FULL,
            showStockIndicators = false,
            showArticleImages = false,
            showArticleActions = false,
            gridColumns = 2,
            showDashboardStats = false,
            showRecentMovements = false,
            showRecentArticles = false
        )

        val backup = serializer.mapDisplaySettings(settings)
        val restored = serializer.mapToDisplaySettings(backup)

        assertEquals(settings, restored)
    }

    @Test
    fun `mapDisplaySettings with unknown card style name falls back to default on restore`() {
        val backup = DisplaySettingsBackup(
            articleCardStyle = "SOMETHING_THAT_NO_LONGER_EXISTS",
            showStockIndicators = true,
            showArticleImages = true,
            gridColumns = 1
        )

        val restored = serializer.mapToDisplaySettings(backup)

        assertEquals(ArticleCardStyle.DEFAULT, restored.articleCardStyle)
    }

    @Test
    fun `mapRecognitionSettings and mapToRecognitionSettings round trip numeric parameters`() {
        val settings = RecognitionSettings(
            loweRatioThreshold = 0.65f,
            absoluteDistanceThreshold = 300f,
            minFeatures = 42,
            matchRatioWeight = 0.4,
            densityWeight = 0.2,
            distanceQualityWeight = 0.3,
            consistencyWeight = 0.1,
            defaultMatchingThreshold = 0.75,
            minFeaturesForValidation = 25,
            idealFeaturesForValidation = 180
        )

        val backup = serializer.mapRecognitionSettings(settings, "Preciso")
        assertEquals("Preciso", backup.presetName)

        val restored = serializer.mapToRecognitionSettings(backup)
        assertEquals(settings, restored)
    }

    @Test
    fun `mapRecognitionSettings preserves null preset name`() {
        val backup = serializer.mapRecognitionSettings(RecognitionSettings.getDefault(), null)
        assertNull(backup.presetName)
    }

    // ============================================
    // JSON SERIALIZE / DESERIALIZE ROUND TRIP
    // ============================================

    @Test
    fun `serialize and deserialize categories list round trip`() {
        val categories = listOf(
            CategoryBackup("c1", "Elettrica", "desc", "note", 1L, 2L),
            CategoryBackup("c2", "Meccanica", "", "", 3L, 4L)
        )

        val json = serializer.serializeCategories(categories)
        val restored = serializer.deserializeCategories(json)

        assertEquals(categories, restored)
    }

    @Test
    fun `serialize and deserialize empty lists round trip`() {
        assertEquals(emptyList<CategoryBackup>(), serializer.deserializeCategories(serializer.serializeCategories(emptyList())))
        assertEquals(emptyList<ArticleBackup>(), serializer.deserializeArticles(serializer.serializeArticles(emptyList())))
        assertEquals(emptyList<InventoryBackup>(), serializer.deserializeInventory(serializer.serializeInventory(emptyList())))
        assertEquals(emptyList<MovementBackup>(), serializer.deserializeMovements(serializer.serializeMovements(emptyList())))
        assertEquals(emptyList<ArticleImageBackup>(), serializer.deserializeArticleImages(serializer.serializeArticleImages(emptyList())))
    }

    @Test
    fun `serialize and deserialize articles list round trip`() {
        val articles = listOf(
            ArticleBackup(
                uuid = "a1", name = "Vite M6", description = "", categoryId = "c1",
                unitOfMeasure = "pz", reorderLevel = 10.0, notes = "",
                codeOEM = "", codeERP = "", codeBM = "", createdAt = 1L, updatedAt = 2L
            )
        )

        val json = serializer.serializeArticles(articles)
        assertEquals(articles, serializer.deserializeArticles(json))
    }

    @Test
    fun `serialize and deserialize inventory list round trip`() {
        val inventory = listOf(InventoryBackup("a1", 12.5, 999L))
        val json = serializer.serializeInventory(inventory)
        assertEquals(inventory, serializer.deserializeInventory(json))
    }

    @Test
    fun `serialize and deserialize movements list round trip`() {
        val movements = listOf(
            MovementBackup(0L, "a1", MovementType.IN.name, 3.0, "nota", 111L),
            MovementBackup(0L, "a1", MovementType.OUT.name, 1.0, "", 222L)
        )
        val json = serializer.serializeMovements(movements)
        assertEquals(movements, serializer.deserializeMovements(json))
    }

    @Test
    fun `serialize and deserialize article images list round trip`() {
        val images = listOf(ArticleImageBackup("i1", "a1", "a1/x.jpg", "QUJD", 100L))
        val json = serializer.serializeArticleImages(images)
        assertEquals(images, serializer.deserializeArticleImages(json))
    }

    @Test
    fun `serialize and deserialize settings round trip`() {
        val display = DisplaySettingsBackup("FULL", true, false, 2, false, true, false, true)
        assertEquals(display, serializer.deserializeDisplaySettings(serializer.serializeDisplaySettings(display)))

        val recognition = RecognitionSettingsBackup(
            0.7f, 280f, 30, 0.5, 0.15, 0.25, 0.1, 0.7, 30, 200, "Bilanciato"
        )
        assertEquals(
            recognition,
            serializer.deserializeRecognitionSettings(serializer.serializeRecognitionSettings(recognition))
        )
    }

    @Test
    fun `serialize and deserialize metadata round trip`() {
        val metadata = BackupMetadata(
            appVersion = "1.4.8",
            appVersionCode = 3,
            dbVersion = 7,
            backupDate = "2026-07-19T10:00:00Z",
            deviceInfo = "Test Device",
            counts = BackupCounts(1, 2, 3, 4, 5, 6),
            checksums = BackupChecksums("a", "b", "c", "d", "e", "f", "g", "h"),
            imagesManifest = listOf("a1/x.jpg", "a2/y.jpg")
        )

        val json = serializer.serializeMetadata(metadata)
        assertEquals(metadata, serializer.deserializeMetadata(json))
    }

    @Test
    fun `deserializeMetadata rejects malformed json`() {
        try {
            serializer.deserializeMetadata("{ not valid json")
            fail("Expected a serialization exception")
        } catch (_: Exception) {
            // atteso
        }
    }

    // ============================================
    // CHECKSUM
    // ============================================

    @Test
    fun `calculateChecksum is deterministic`() {
        val data = """{"a":1}"""
        assertEquals(serializer.calculateChecksum(data), serializer.calculateChecksum(data))
    }

    @Test
    fun `calculateChecksum differs for different content`() {
        val checksumA = serializer.calculateChecksum("""{"a":1}""")
        val checksumB = serializer.calculateChecksum("""{"a":2}""")
        assertNotEquals(checksumA, checksumB)
    }

    @Test
    fun `calculateChecksum has sha256 prefix`() {
        assertTrue(serializer.calculateChecksum("x").startsWith("sha256:"))
    }

    @Test
    fun `verifyChecksum true for matching data, false after tampering`() {
        val original = """{"articles":[]}"""
        val checksum = serializer.calculateChecksum(original)

        assertTrue(serializer.verifyChecksum(original, checksum))
        assertFalse(serializer.verifyChecksum(original + " ", checksum))
    }

    @Test
    fun `calculateChecksumForList is order independent`() {
        val a = serializer.calculateChecksumForList(listOf("x/1.jpg", "y/2.jpg", "z/3.jpg"))
        val b = serializer.calculateChecksumForList(listOf("z/3.jpg", "x/1.jpg", "y/2.jpg"))
        assertEquals(a, b)
    }

    @Test
    fun `calculateChecksumForList differs when content differs`() {
        val a = serializer.calculateChecksumForList(listOf("x/1.jpg"))
        val b = serializer.calculateChecksumForList(listOf("x/1.jpg", "y/2.jpg"))
        assertNotEquals(a, b)
    }

    @Test
    fun `calculateChecksumForList of empty list is stable`() {
        assertEquals(
            serializer.calculateChecksumForList(emptyList()),
            serializer.calculateChecksumForList(emptyList())
        )
    }
}

package net.calvuz.qstore.backup.data.serializer

import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.calvuz.qstore.app.data.local.entity.*
import net.calvuz.qstore.backup.domain.model.*
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.model.RecognitionSettings
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializer per convertire i dati del database in formato JSON per il backup
 */
@Singleton
class BackupSerializer @Inject constructor() {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ============================================
    // ENTITY TO BACKUP MAPPING
    // ============================================
    
    fun mapCategory(entity: ArticleCategoryEntity): CategoryBackup {
        return CategoryBackup(
            uuid = entity.uuid,
            name = entity.name,
            description = entity.description,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    fun mapArticle(entity: ArticleEntity): ArticleBackup {
        return ArticleBackup(
            uuid = entity.uuid,
            name = entity.name,
            description = entity.description,
            categoryId = entity.categoryId,
            unitOfMeasure = entity.unitOfMeasure,
            reorderLevel = entity.reorderLevel,
            notes = entity.notes,
            codeOEM = entity.codeOEM,
            codeERP = entity.codeERP,
            codeBM = entity.codeBM,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    fun mapInventory(entity: InventoryEntity): InventoryBackup {
        return InventoryBackup(
            articleUuid = entity.articleUuid,
            currentQuantity = entity.currentQuantity,
            lastMovementAt = entity.lastMovementAt
        )
    }
    
    fun mapMovement(entity: MovementEntity): MovementBackup {
        return MovementBackup(
            id = entity.id,
            articleUuid = entity.articleUuid,
            type = entity.type.name,
            quantity = entity.quantity,
            notes = entity.notes,
            createdAt = entity.createdAt
        )
    }
    
    fun mapArticleImage(entity: ArticleImageEntity): ArticleImageBackup {
        return ArticleImageBackup(
            id = entity.id,
            articleUuid = entity.articleUuid,
            imagePath = entity.imagePath,
            featuresDataBase64 = Base64.encodeToString(entity.featuresData, Base64.NO_WRAP),
            createdAt = entity.createdAt
        )
    }
    
    fun mapDisplaySettings(settings: DisplaySettings): DisplaySettingsBackup {
        return DisplaySettingsBackup(
            articleCardStyle = settings.articleCardStyle.name,
            showStockIndicators = settings.showStockIndicators,
            showArticleImages = settings.showArticleImages,
            gridColumns = settings.gridColumns
        )
    }
    
    fun mapRecognitionSettings(settings: RecognitionSettings, presetName: String?): RecognitionSettingsBackup {
        return RecognitionSettingsBackup(
            loweRatioThreshold = settings.loweRatioThreshold,
            absoluteDistanceThreshold = settings.absoluteDistanceThreshold,
            minFeatures = settings.minFeatures,
            matchRatioWeight = settings.matchRatioWeight,
            densityWeight = settings.densityWeight,
            distanceQualityWeight = settings.distanceQualityWeight,
            consistencyWeight = settings.consistencyWeight,
            defaultMatchingThreshold = settings.defaultMatchingThreshold,
            minFeaturesForValidation = settings.minFeaturesForValidation,
            idealFeaturesForValidation = settings.idealFeaturesForValidation,
            presetName = presetName
        )
    }
    
    // ============================================
    // BACKUP TO ENTITY MAPPING (for restore)
    // ============================================
    
    fun mapToCategory(backup: CategoryBackup): ArticleCategoryEntity {
        return ArticleCategoryEntity(
            uuid = backup.uuid,
            name = backup.name,
            description = backup.description,
            notes = backup.notes,
            createdAt = backup.createdAt,
            updatedAt = backup.updatedAt
        )
    }
    
    fun mapToArticle(backup: ArticleBackup): ArticleEntity {
        return ArticleEntity(
            uuid = backup.uuid,
            name = backup.name,
            description = backup.description,
            categoryId = backup.categoryId,
            unitOfMeasure = backup.unitOfMeasure,
            reorderLevel = backup.reorderLevel,
            notes = backup.notes,
            codeOEM = backup.codeOEM,
            codeERP = backup.codeERP,
            codeBM = backup.codeBM,
            createdAt = backup.createdAt,
            updatedAt = backup.updatedAt
        )
    }
    
    fun mapToInventory(backup: InventoryBackup): InventoryEntity {
        return InventoryEntity(
            articleUuid = backup.articleUuid,
            currentQuantity = backup.currentQuantity,
            lastMovementAt = backup.lastMovementAt
        )
    }
    
    fun mapToMovement(backup: MovementBackup): MovementEntity {
        return MovementEntity(
            id = backup.id,
            articleUuid = backup.articleUuid,
            type = net.calvuz.qstore.app.domain.model.enum.MovementType.valueOf(backup.type),
            quantity = backup.quantity,
            notes = backup.notes,
            createdAt = backup.createdAt
        )
    }
    
    fun mapToArticleImage(backup: ArticleImageBackup): ArticleImageEntity {
        return ArticleImageEntity(
            id = backup.id,
            articleUuid = backup.articleUuid,
            imagePath = backup.imagePath,
            featuresData = Base64.decode(backup.featuresDataBase64, Base64.NO_WRAP),
            createdAt = backup.createdAt
        )
    }
    
    fun mapToDisplaySettings(backup: DisplaySettingsBackup): DisplaySettings {
        return DisplaySettings(
            articleCardStyle = ArticleCardStyle.fromName(backup.articleCardStyle),
            showStockIndicators = backup.showStockIndicators,
            showArticleImages = backup.showArticleImages,
            gridColumns = backup.gridColumns
        )
    }
    
    fun mapToRecognitionSettings(backup: RecognitionSettingsBackup): RecognitionSettings {
        return RecognitionSettings(
            loweRatioThreshold = backup.loweRatioThreshold,
            absoluteDistanceThreshold = backup.absoluteDistanceThreshold,
            minFeatures = backup.minFeatures,
            matchRatioWeight = backup.matchRatioWeight,
            densityWeight = backup.densityWeight,
            distanceQualityWeight = backup.distanceQualityWeight,
            consistencyWeight = backup.consistencyWeight,
            defaultMatchingThreshold = backup.defaultMatchingThreshold,
            minFeaturesForValidation = backup.minFeaturesForValidation,
            idealFeaturesForValidation = backup.idealFeaturesForValidation
        )
    }
    
    // ============================================
    // JSON SERIALIZATION
    // ============================================
    
    fun serializeCategories(categories: List<CategoryBackup>): String {
        return json.encodeToString(categories)
    }
    
    fun serializeArticles(articles: List<ArticleBackup>): String {
        return json.encodeToString(articles)
    }
    
    fun serializeInventory(inventory: List<InventoryBackup>): String {
        return json.encodeToString(inventory)
    }
    
    fun serializeMovements(movements: List<MovementBackup>): String {
        return json.encodeToString(movements)
    }
    
    fun serializeArticleImages(images: List<ArticleImageBackup>): String {
        return json.encodeToString(images)
    }
    
    fun serializeDisplaySettings(settings: DisplaySettingsBackup): String {
        return json.encodeToString(settings)
    }
    
    fun serializeRecognitionSettings(settings: RecognitionSettingsBackup): String {
        return json.encodeToString(settings)
    }
    
    fun serializeMetadata(metadata: BackupMetadata): String {
        return json.encodeToString(metadata)
    }
    
    // ============================================
    // JSON DESERIALIZATION
    // ============================================
    
    fun deserializeCategories(jsonString: String): List<CategoryBackup> {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeArticles(jsonString: String): List<ArticleBackup> {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeInventory(jsonString: String): List<InventoryBackup> {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeMovements(jsonString: String): List<MovementBackup> {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeArticleImages(jsonString: String): List<ArticleImageBackup> {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeDisplaySettings(jsonString: String): DisplaySettingsBackup {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeRecognitionSettings(jsonString: String): RecognitionSettingsBackup {
        return json.decodeFromString(jsonString)
    }
    
    fun deserializeMetadata(jsonString: String): BackupMetadata {
        return json.decodeFromString(jsonString)
    }
    
    // ============================================
    // CHECKSUM
    // ============================================
    
    /**
     * Calcola il checksum SHA-256 di una stringa
     */
    fun calculateChecksum(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Calcola il checksum SHA-256 di una lista di stringhe (per imagesManifest)
     */
    fun calculateChecksumForList(items: List<String>): String {
        val combined = items.sorted().joinToString("\n")
        return calculateChecksum(combined)
    }
    
    /**
     * Verifica che un checksum corrisponda ai dati
     */
    fun verifyChecksum(data: String, expectedChecksum: String): Boolean {
        return calculateChecksum(data) == expectedChecksum
    }
}

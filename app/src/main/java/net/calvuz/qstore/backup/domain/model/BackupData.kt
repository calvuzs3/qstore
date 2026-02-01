package net.calvuz.qstore.backup.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qstore.app.domain.model.enum.MovementType

/**
 * Modelli serializzabili per l'export JSON
 * Questi sono separati dalle Entity per disaccoppiare il formato di backup dal database
 */

// ============================================
// CATEGORY
// ============================================

@Serializable
data class CategoryBackup(
    val uuid: String,
    val name: String,
    val description: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ============================================
// ARTICLE
// ============================================

@Serializable
data class ArticleBackup(
    val uuid: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val unitOfMeasure: String,
    val reorderLevel: Double,
    val notes: String,
    val codeOEM: String,
    val codeERP: String,
    val codeBM: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ============================================
// INVENTORY
// ============================================

@Serializable
data class InventoryBackup(
    val articleUuid: String,
    val currentQuantity: Double,
    val lastMovementAt: Long
)

// ============================================
// MOVEMENT
// ============================================

@Serializable
data class MovementBackup(
    val id: Long,
    val articleUuid: String,
    val type: String, // Serializzato come stringa per compatibilità
    val quantity: Double,
    val notes: String,
    val createdAt: Long
)

// ============================================
// ARTICLE IMAGE
// ============================================

@Serializable
data class ArticleImageBackup(
    val uuid: String,
    val articleUuid: String,
    val imagePath: String,
    /** Features OpenCV serializzate come Base64 */
    val featuresDataBase64: String,
    val createdAt: Long
)

// ============================================
// SETTINGS
// ============================================

@Serializable
data class DisplaySettingsBackup(
    val articleCardStyle: String,
    val showStockIndicators: Boolean,
    val showArticleImages: Boolean,
    val gridColumns: Int
)

@Serializable
data class RecognitionSettingsBackup(
    val loweRatioThreshold: Float,
    val absoluteDistanceThreshold: Float,
    val minFeatures: Int,
    val matchRatioWeight: Double,
    val densityWeight: Double,
    val distanceQualityWeight: Double,
    val consistencyWeight: Double,
    val defaultMatchingThreshold: Double,
    val minFeaturesForValidation: Int,
    val idealFeaturesForValidation: Int,
    val presetName: String?
)

// ============================================
// COMPLETE BACKUP DATA
// ============================================

/**
 * Contenitore per tutti i dati del backup (uso interno, non serializzato direttamente)
 */
data class BackupData(
    val categories: List<CategoryBackup>,
    val articles: List<ArticleBackup>,
    val inventory: List<InventoryBackup>,
    val movements: List<MovementBackup>,
    val articleImages: List<ArticleImageBackup>,
    val displaySettings: DisplaySettingsBackup,
    val recognitionSettings: RecognitionSettingsBackup
)

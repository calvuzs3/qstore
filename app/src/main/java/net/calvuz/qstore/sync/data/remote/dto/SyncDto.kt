package net.calvuz.qstore.sync.data.remote.dto

import kotlinx.serialization.Serializable

// Stessa shape esatta dei DTO server-side (quickstore-server SyncDto.kt) — nessuno di
// questi porta orgId: non è mai un valore che il client fornisce, lo inietta il server
// leggendo il claim orgId del JWT (vedi quickstore-server/CLAUDE.md sezione 5).

@Serializable
data class ArticleCategoryDto(
    val id: String,
    val name: String,
    val description: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@Serializable
data class ArticleDto(
    val id: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val unitOfMeasure: String,
    val reorderLevel: Double,
    val notes: String,
    val codeOem: String,
    val codeErp: String,
    val codeBm: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@Serializable
data class MovementDto(
    val id: String,
    val articleId: String,
    val type: String, // IN | OUT | ADJUSTMENT | TRANSFER
    val fromLocationId: String?,
    val toLocationId: String?,
    val quantity: Double,
    val notes: String,
    val createdBy: String,
    val createdAt: Long
)

@Serializable
data class ArticleImageDto(
    val id: String,
    val articleId: String,
    val imagePath: String,
    val featuresData: String, // Base64
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@Serializable
data class LocationDto(
    val id: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@Serializable
data class ArticleLocationThresholdDto(
    val id: String,
    val articleId: String,
    val locationId: String,
    val reorderLevel: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val articleCategories: List<ArticleCategoryDto> = emptyList(),
    val locations: List<LocationDto> = emptyList(),
    val articles: List<ArticleDto> = emptyList(),
    val articleLocationThresholds: List<ArticleLocationThresholdDto> = emptyList(),
    val movements: List<MovementDto> = emptyList(),
    val articleImages: List<ArticleImageDto> = emptyList()
)

@Serializable
data class RejectedEntry(val id: String, val reason: String)

@Serializable
data class SyncPushResponse(
    val acceptedIds: List<String>,
    val rejectedIds: List<RejectedEntry> = emptyList(),
    val serverTimestamp: Long
)

@Serializable
data class SyncPullResponse(
    val serverTimestamp: Long,
    val articleCategories: List<ArticleCategoryDto>,
    val locations: List<LocationDto>,
    val articles: List<ArticleDto>,
    val articleLocationThresholds: List<ArticleLocationThresholdDto>,
    val movements: List<MovementDto>,
    val articleImages: List<ArticleImageDto>
)

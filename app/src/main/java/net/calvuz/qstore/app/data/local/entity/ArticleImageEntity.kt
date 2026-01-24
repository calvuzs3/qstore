package net.calvuz.qstore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity per la tabella article_images - Immagini e features OpenCV degli articoli
 */
@Entity(
    tableName = "article_images",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["article_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["article_uuid"])
    ]
)
data class ArticleImageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "image_path")
    val imagePath: String, // Path relativo in internal storage

    @ColumnInfo(name = "features_data")
    val featuresData: ByteArray, // OpenCV Mat descriptors serializzati

    @ColumnInfo(name = "created_at")
    val createdAt: Long // Unix timestamp UTC
) {
    // Override equals e hashCode per ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArticleImageEntity

        if (id != other.id) return false
        if (articleUuid != other.articleUuid) return false
        if (imagePath != other.imagePath) return false
        if (!featuresData.contentEquals(other.featuresData)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + articleUuid.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + featuresData.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
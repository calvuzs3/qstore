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
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "image_path")
    val imagePath: String, // Path relativo in internal storage

    @ColumnInfo(name = "features_data")
    val featuresData: ByteArray, // OpenCV Mat descriptors serializzati

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp UTC

    // Prima di questo campo si usava created_at come proxy per il dirty-tracking del
    // sync — funzionava per il push (le righe non si aggiornavano mai) ma non per una
    // cancellazione, che è concettualmente un update e deve poter far avanzare questo
    // timestamp senza intaccare il vero istante di creazione.
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = createdAt,

    // false per le immagini scattate su questo device (il JPEG deve ancora essere caricato
    // su /images/upload/{id}); true per quelle arrivate via /sync/pull — sono per
    // definizione già sul server, non vanno ricaricate. Vedi sync/data/worker/ImageTransferWorker.
    @ColumnInfo(name = "is_uploaded", defaultValue = "0")
    val isUploaded: Boolean = false,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
) {
    // Override equals e hashCode per ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArticleImageEntity

        if (uuid != other.uuid) return false
        if (articleUuid != other.articleUuid) return false
        if (imagePath != other.imagePath) return false
        if (!featuresData.contentEquals(other.featuresData)) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (isUploaded != other.isUploaded) return false
        if (isDeleted != other.isDeleted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + articleUuid.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + featuresData.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + isUploaded.hashCode()
        result = 31 * result + isDeleted.hashCode()
        return result
    }
}
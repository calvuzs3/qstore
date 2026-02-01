package net.calvuz.qstore.app.domain.model

/**
 * Domain Model per ArticleImage
 * Rappresenta un'immagine associata a un articolo con le sue features OpenCV
 * per il riconoscimento visuale.
 */
data class ArticleImage(
    val uuid: String,
    val articleUuid: String,
    val imagePath: String,          // Path relativo in internal storage
    val featuresData: ByteArray,    // OpenCV Mat descriptors serializzati
    val createdAt: Long             // Unix timestamp UTC in milliseconds
) {
    // Override equals e hashCode per ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArticleImage

        if (uuid != other.uuid) return false
        if (articleUuid != other.articleUuid) return false
        if (imagePath != other.imagePath) return false
        if (!featuresData.contentEquals(other.featuresData)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + articleUuid.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + featuresData.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
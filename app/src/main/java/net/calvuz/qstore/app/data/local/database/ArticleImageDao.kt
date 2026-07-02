package net.calvuz.qstore.app.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.data.local.entity.ArticleImageEntity

/**
 * DAO per operazioni sulla tabella article_images
 */
@Dao
interface ArticleImageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(image: ArticleImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(image: ArticleImageEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(images: List<ArticleImageEntity>)

    @Delete
    suspend fun delete(image: ArticleImageEntity)

    @Query("DELETE FROM article_images WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    // Non filtrata su is_deleted: usata anche dal worker di trasferimento foto e dal sync,
    // che devono vedere anche le righe cancellate (tombstone). Il matching per
    // riconoscimento (searchArticlesByImage) filtra a parte, vedi ImageRecognitionRepositoryImpl.
    @Query("SELECT * FROM article_images")
    suspend fun getAll(): List<ArticleImageEntity>

    // Non filtrata su is_deleted: usata anche da delete/update e dal sync (LWW su pull).
    @Query("SELECT * FROM article_images WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ArticleImageEntity?

    @Query("SELECT * FROM article_images WHERE article_uuid = :articleUuid AND is_deleted = 0")
    suspend fun getByArticleUuid(articleUuid: String): List<ArticleImageEntity>

    @Query("SELECT * FROM article_images WHERE article_uuid = :articleUuid AND is_deleted = 0")
    fun observeByArticleUuid(articleUuid: String): Flow<List<ArticleImageEntity>>

    @Query("SELECT COUNT(*) FROM article_images WHERE article_uuid = :articleUuid AND is_deleted = 0")
    suspend fun getCountByArticleUuid(articleUuid: String): Int

    /**
     * Elimina tutte le immagini di un articolo
     * @param articleUuid UUID dell'articolo
     * @return Numero di righe eliminate
     */
    @Query("DELETE FROM article_images WHERE article_uuid = :articleUuid")
    suspend fun deleteByArticleUuid(articleUuid: String): Int

    /**
     * Conta le immagini di un articolo
     * @param articleUuid UUID dell'articolo
     * @return Numero di immagini
     */
    @Query("SELECT COUNT(*) FROM article_images WHERE article_uuid = :articleUuid AND is_deleted = 0")
    suspend fun countByArticleUuid(articleUuid: String): Int

    /**
     * Verifica se un articolo ha immagini
     * @param articleUuid UUID dell'articolo
     * @return true se ha almeno un'immagine
     */
    @Query("SELECT COUNT(*) > 0 FROM article_images WHERE article_uuid = :articleUuid AND is_deleted = 0")
    suspend fun hasImages(articleUuid: String): Boolean

    /**
     * Righe modificate dopo il cursore di sync — usata per costruire il payload di push.
     * Non filtrata su is_deleted: una cancellazione (is_deleted=1, updated_at=now) DEVE
     * essere raccolta qui per essere propagata al server.
     */
    @Query("SELECT * FROM article_images WHERE updated_at > :since")
    suspend fun getUpdatedSince(since: Long): List<ArticleImageEntity>

    /**
     * Immagini il cui JPEG non è ancora stato caricato su /images/upload/{id}. Esclude le
     * già cancellate: non ha senso caricare il file di una foto che stiamo per dire al
     * server essere sparita — basta il metadato isDeleted=true nel push.
     */
    @Query("SELECT * FROM article_images WHERE is_uploaded = 0 AND is_deleted = 0")
    suspend fun getPendingUpload(): List<ArticleImageEntity>

    @Query("UPDATE article_images SET is_uploaded = 1 WHERE uuid = :uuid")
    suspend fun markUploaded(uuid: String)

    /** Soft-delete di una singola immagine — usata da ImageRecognitionRepositoryImpl.deleteImage(). */
    @Query("UPDATE article_images SET is_deleted = 1, updated_at = :updatedAt WHERE uuid = :uuid")
    suspend fun markDeleted(uuid: String, updatedAt: Long)

    /**
     * Soft-delete di tutte le immagini di un articolo — usata sia da
     * ImageRecognitionRepositoryImpl.deleteImages() sia da ArticleRepositoryImpl.deleteArticle()
     * (cascade esplicito in codice: il CASCADE del FK di Room non scatta più, dato che
     * l'articolo viene marcato cancellato con un UPDATE, non un DELETE fisico).
     */
    @Query("UPDATE article_images SET is_deleted = 1, updated_at = :updatedAt WHERE article_uuid = :articleUuid AND is_deleted = 0")
    suspend fun markAllDeletedByArticleUuid(articleUuid: String, updatedAt: Long)
}
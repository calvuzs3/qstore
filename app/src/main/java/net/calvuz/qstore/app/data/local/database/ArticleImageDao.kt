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

    @Query("SELECT * FROM article_images")
    suspend fun getAll(): List<ArticleImageEntity>

    @Query("SELECT * FROM article_images WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ArticleImageEntity?

    @Query("SELECT * FROM article_images WHERE article_uuid = :articleUuid")
    suspend fun getByArticleUuid(articleUuid: String): List<ArticleImageEntity>

    @Query("SELECT * FROM article_images WHERE article_uuid = :articleUuid")
    fun observeByArticleUuid(articleUuid: String): Flow<List<ArticleImageEntity>>

    @Query("SELECT COUNT(*) FROM article_images WHERE article_uuid = :articleUuid")
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
    @Query("SELECT COUNT(*) FROM article_images WHERE article_uuid = :articleUuid")
    suspend fun countByArticleUuid(articleUuid: String): Int

    /**
     * Verifica se un articolo ha immagini
     * @param articleUuid UUID dell'articolo
     * @return true se ha almeno un'immagine
     */
    @Query("SELECT COUNT(*) > 0 FROM article_images WHERE article_uuid = :articleUuid")
    suspend fun hasImages(articleUuid: String): Boolean

    /**
     * Righe modificate dopo il cursore di sync. Basata su created_at (questa tabella non
     * ha updated_at/is_deleted lato client, a differenza dello schema server — le
     * immagini locali sono hard-delete, non propagano una cancellazione al server).
     */
    @Query("SELECT * FROM article_images WHERE created_at > :since")
    suspend fun getUpdatedSince(since: Long): List<ArticleImageEntity>

    /** Immagini il cui JPEG non è ancora stato caricato su /images/upload/{id}. */
    @Query("SELECT * FROM article_images WHERE is_uploaded = 0")
    suspend fun getPendingUpload(): List<ArticleImageEntity>

    @Query("UPDATE article_images SET is_uploaded = 1 WHERE uuid = :uuid")
    suspend fun markUploaded(uuid: String)
}
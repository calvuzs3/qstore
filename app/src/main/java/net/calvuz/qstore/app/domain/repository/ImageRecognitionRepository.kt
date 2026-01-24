package net.calvuz.qstore.app.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.ArticleImage

/**
 * Repository Interface per gestione immagini e riconoscimento OpenCV
 *
 * Definisce i contratti per salvare immagini, estrarre features OpenCV
 * e cercare articoli tramite riconoscimento visuale.
 */
interface ImageRecognitionRepository {

    /**
     * Salva un'immagine di un articolo ed estrae le features OpenCV
     *
     * @param articleUuid UUID dell'articolo
     * @param imageData ByteArray dell'immagine (JPEG/PNG)
     * @return Result con ArticleImage salvata, errore altrimenti
     */
    suspend fun saveArticleImage(
        articleUuid: String,
        imageData: ByteArray
    ): Result<ArticleImage>

    /**
     * Ottiene tutte le immagini di un articolo
     */
    suspend fun getArticleImages(articleUuid: String): Result<List<ArticleImage>>
    suspend fun getArticleImageById(imageId: Long): Result<ArticleImage?>

    /**
     * Osserva le immagini di un articolo
     */
    fun observeArticleImages(articleUuid: String): Flow<List<ArticleImage>>

    /**
     * Elimina un'immagine
     */
    suspend fun deleteImage(imageId: Long): Result<Unit>
    suspend fun deleteImages(articleUuid: String): Result<Int>

    /**
     * Cerca articoli tramite riconoscimento immagine
     *
     * Confronta l'immagine fornita con tutte le immagini salvate
     * usando OpenCV feature matching.
     *
     * @param imageData ByteArray dell'immagine da cercare
     * @param threshold Soglia di similarità (0.0-1.0), default 0.7
     * @return Lista di UUID articoli ordinati per similarità (più simili primi)
     */
    suspend fun searchArticlesByImage(
        imageData: ByteArray,
        threshold: Double = 0.7
    ): Result<List<String>>

    /**
     * Ottiene il path completo di un'immagine
     */
    suspend fun getImagePath(imageId: Long): Result<String?>
}
package net.calvuz.qstore.domain.repository

import net.calvuz.qstore.domain.model.Article
import net.calvuz.qstore.domain.model.Inventory
import kotlinx.coroutines.flow.Flow

/**
 * Repository Interface per gestione articoli e inventario
 *
 * Definisce i contratti per l'accesso ai dati degli articoli.
 * L'implementazione sarà nel data layer.
 */
interface ArticleRepository {

    /**
     * Inserisce un nuovo articolo con inventario iniziale
     * @return true se l'inserimento è riuscito, false altrimenti
     */
    suspend fun insertArticle(article: Article, initialQuantity: Double): Result<Unit>

    /**
     * Aggiorna i dati anagrafici di un articolo
     */
    suspend fun updateArticle(article: Article): Result<Unit>

    /**
     * Elimina un articolo (cascade su inventory, movements, images)
     */
    suspend fun deleteArticle(uuid: String): Result<Unit>

    /**
     * Recupera tutti gli articoli
     */
    suspend fun getAll(): Result<List<Article>>

    /**
     * Ottiene un articolo per UUID
     */
    suspend fun getByUuid(uuid: String): Result<Article?>

    /**
     * Cerca articoli per nome
     */
    suspend fun searchByName(query: String): Result<List<Article>>

    /**
     * Recupera articoli per categoria
     */
    suspend fun getByCategory(category: String): Result<List<Article>>

    /**
     * Osserva tutti gli articoli
     */
    fun observeAll(): Flow<List<Article>>

    /**
     * Osserva un articolo per UUID (Flow reattivo)
     */
    fun observeByUuid(uuid: String): Flow<Article?>

    /**
     * Ottiene l'inventario di un articolo
     */
    suspend fun getInventory(articleUuid: String): Result<Inventory?>

    /**
     * Osserva l'inventario di un articolo
     */
    fun observeInventory(articleUuid: String): Flow<Inventory?>

    /**
     * Ottiene il conteggio totale degli articoli
     */
    suspend fun getArticlesCount(): Result<Int>
}
package net.calvuz.qstore.app.domain.usecase.article

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.model.Inventory
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Use Case per recuperare articoli dal magazzino
 */
class GetArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    /**
    * Recupera tutti gli articoli
    */
    suspend fun getAll(): Result<List<Article>> {
        return try {
            articleRepository.getAll()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene un articolo per UUID
     */
    suspend fun getByUuid(uuid: String): Result<Article?> {
        return try {
            if (uuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID non valido"))
            }

            articleRepository.getByUuid(uuid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva tutti gli articoli
     */
    fun observeAll(): Flow<List<Article>> {
        return articleRepository.observeAll()
    }

    /**
     * Osserva un articolo per UUID (Flow reattivo)
     */
    fun observeByUuid(uuid: String): Flow<Article?> {
        return articleRepository.observeByUuid(uuid)
    }

    /**
     * Cerca articoli per nome
     */
    suspend fun searchByName(query: String): Result<List<Article>> {
        return try {
            if (query.isBlank()) {
                return getAll()
            }

            articleRepository.searchByName(query)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene articoli per categoria
     */
    suspend fun getByCategory(category: String): Result<List<Article>> {
        return try {
            if (category.isBlank()) {
                return getAll()
            }

            articleRepository.getByCategory(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene il conteggio totale degli articoli
     */
    suspend fun getCount(): Result<Int> {
        return articleRepository.getArticlesCount()
    }

    /**
     * Recupera inventario di un articolo
     */
    suspend fun getInventory(articleUuid: String): Result<Inventory?> {
        return try {
            if (articleUuid.isBlank()) {
                return Result.failure(IllegalArgumentException("UUID non valido"))
            }

            articleRepository.getInventory(articleUuid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Osserva inventario di un articolo
     */
    fun observeInventory(articleUuid: String): Flow<Inventory?> {
        require(articleUuid.isNotBlank()) { "UUID non valido" }
        return articleRepository.observeInventory(articleUuid)
    }
}
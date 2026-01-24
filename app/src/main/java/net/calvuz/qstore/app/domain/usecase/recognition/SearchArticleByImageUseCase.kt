package net.calvuz.qstore.app.domain.usecase.recognition

import net.calvuz.qstore.app.domain.model.Article
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.repository.ImageRecognitionRepository
import javax.inject.Inject

/**
 * Use Case migliorato per cercare articoli tramite riconoscimento immagine
 *
 * Utilizza algoritmi OpenCV avanzati e soglie dinamiche per risultati più accurati
 */
class SearchArticleByImageUseCase @Inject constructor(
    private val imageRecognitionRepository: ImageRecognitionRepository,
    private val articleRepository: ArticleRepository
) {
    /**
     * Cerca articoli tramite immagine con soglia adattiva
     *
     * @param imageData ByteArray dell'immagine da cercare (JPEG/PNG)
     * @param searchMode Modalità di ricerca (STRICT, NORMAL, LOOSE)
     * @return Result con SearchResult contenente articoli e metadati
     */
    suspend operator fun invoke(
        imageData: ByteArray,
        searchMode: SearchMode = SearchMode.NORMAL
    ): Result<SearchResult> {
        // Validazione
        if (imageData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Image data cannot be empty"))
        }

        val threshold = when (searchMode) {
            SearchMode.STRICT -> STRICT_THRESHOLD
            SearchMode.NORMAL -> NORMAL_THRESHOLD
            SearchMode.LOOSE -> LOOSE_THRESHOLD
        }

        // Cerca UUID articoli tramite image matching
        val articleUuids = imageRecognitionRepository.searchArticlesByImage(imageData, threshold)
            .getOrElse {
                return Result.failure(it)
            }

        // Se non ci sono match con la soglia normale, prova con soglia più permissiva
        val finalArticleUuids = if (articleUuids.isEmpty() && searchMode == SearchMode.NORMAL) {
            imageRecognitionRepository.searchArticlesByImage(imageData, FALLBACK_THRESHOLD)
                .getOrElse { emptyList() }
        } else {
            articleUuids
        }

        // Recupera gli articoli completi con priorità
        val articlesWithPriority = retrieveArticlesWithPriority(finalArticleUuids)

        val searchResult = SearchResult(
            articles = articlesWithPriority.map { it.first },
            totalMatches = finalArticleUuids.size,
            searchMode = searchMode,
            threshold = threshold,
            hasUsedFallback = articleUuids.isEmpty() && finalArticleUuids.isNotEmpty(),
            confidence = calculateOverallConfidence(articlesWithPriority)
        )

        return Result.success(searchResult)
    }

    /**
     * Cerca con più soglie in sequenza per massimizzare risultati
     */
    suspend fun searchMultiThreshold(imageData: ByteArray): Result<SearchResult> {
        val thresholds = listOf(STRICT_THRESHOLD, NORMAL_THRESHOLD, LOOSE_THRESHOLD)

        for (threshold in thresholds) {
            val result = imageRecognitionRepository.searchArticlesByImage(imageData, threshold)

            if (result.isSuccess) {
                val articleUuids = result.getOrNull()!!
                if (articleUuids.isNotEmpty()) {
                    val articlesWithPriority = retrieveArticlesWithPriority(articleUuids)

                    return Result.success(
                        SearchResult(
                            articles = articlesWithPriority.map { it.first },
                            totalMatches = articleUuids.size,
                            searchMode = when (threshold) {
                                STRICT_THRESHOLD -> SearchMode.STRICT
                                NORMAL_THRESHOLD -> SearchMode.NORMAL
                                else -> SearchMode.LOOSE
                            },
                            threshold = threshold,
                            hasUsedFallback = threshold != STRICT_THRESHOLD,
                            confidence = calculateOverallConfidence(articlesWithPriority)
                        )
                    )
                }
            }
        }

        // Nessun risultato trovato
        return Result.success(
            SearchResult(
                articles = emptyList(),
                totalMatches = 0,
                searchMode = SearchMode.NORMAL,
                threshold = NORMAL_THRESHOLD,
                hasUsedFallback = true,
                confidence = 0.0
            )
        )
    }

    /**
     * Cerca solo articoli con alta confidence
     */
    suspend fun searchHighConfidence(imageData: ByteArray): Result<SearchResult> {
        return invoke(imageData, SearchMode.STRICT)
    }

    /**
     * Cerca con soglia permissiva per esplorare opzioni
     */
    suspend fun searchExploratory(imageData: ByteArray): Result<SearchResult> {
        return invoke(imageData, SearchMode.LOOSE)
    }

    /**
     * Recupera articoli con priorità basata su vari fattori
     */
    private suspend fun retrieveArticlesWithPriority(
        articleUuids: List<String>
    ): List<Pair<Article, Double>> {
        val articlesWithPriority = mutableListOf<Pair<Article, Double>>()

        for ((index, uuid) in articleUuids.withIndex()) {
            val article = articleRepository.getByUuid(uuid).getOrNull()

            if (article != null) {
                // Calcola priorità basata su:
                // 1. Posizione nella lista di match (più in alto = migliore)
                // 2. Disponibilità in magazzino
                // 3. Frequenza di utilizzo (se disponibile)

                val positionScore = 1.0 - (index.toDouble() / articleUuids.size)
                val availabilityBonus = 0.0 // TODO: if (article.quantity > 0) 0.15 else -0.1 // Penalizza articoli esauriti
                val recentUsageBonus = 0.0 // TODO: implementare basandosi su movimenti recenti

                // Bonus per articoli con più immagini (più dati = più affidabile)
                val imageCountBonus = kotlin.math.min(0.1, article.uuid.length * 0.001) // Placeholder

                val priority = (positionScore + availabilityBonus + recentUsageBonus + imageCountBonus)
                    .coerceIn(0.0, 1.0)

                articlesWithPriority.add(article to priority)
            }
        }

        // Ordina per priorità decrescente e mantieni solo i migliori se troppi risultati
        val sortedResults = articlesWithPriority.sortedByDescending { it.second }

        // Limita a 5 risultati per evitare confusione
        return sortedResults.take(5)
    }

    /**
     * Calcola confidence complessiva del risultato di ricerca
     */
    private fun calculateOverallConfidence(
        articlesWithPriority: List<Pair<Article, Double>>
    ): Double {
        if (articlesWithPriority.isEmpty()) return 0.0

        // Media pesata delle priorità, con peso maggiore per i primi risultati
        val weightedSum = articlesWithPriority.mapIndexed { index, (_, priority) ->
            val weight = 1.0 / (index + 1) // Peso decrescente: 1.0, 0.5, 0.33, ...
            priority * weight
        }.sum()

        val totalWeight = articlesWithPriority.mapIndexed { index, _ ->
            1.0 / (index + 1)
        }.sum()

        return (weightedSum / totalWeight).coerceIn(0.0, 1.0)
    }

    /**
     * Modalità di ricerca
     */
    enum class SearchMode {
        STRICT,   // Alta precisione, pochi falsi positivi
        NORMAL,   // Bilanciata
        LOOSE     // Alta recall, più risultati possibili
    }

    /**
     * Risultato della ricerca con metadati
     */
    data class SearchResult(
        val articles: List<Article>,
        val totalMatches: Int,
        val searchMode: SearchMode,
        val threshold: Double,
        val hasUsedFallback: Boolean,
        val confidence: Double
    ) {
        val isEmpty: Boolean get() = articles.isEmpty()
        val isHighConfidence: Boolean get() = confidence >= 0.7
        val isLowConfidence: Boolean get() = confidence < 0.4

        /**
         * Ottieni solo gli articoli con alta confidence
         */
        fun getHighConfidenceArticles(): List<Article> {
            return if (isHighConfidence) articles else articles.take(2)
        }

        /**
         * Messaggio user-friendly basato sui risultati
         */
        fun getUserMessage(): String {
            return when {
                isEmpty -> "Nessun articolo trovato. Prova con un'altra immagine."
                isHighConfidence -> "Ho trovato ${articles.size} articoli corrispondenti con alta precisione."
                isLowConfidence -> "Ho trovato ${articles.size} possibili corrispondenze, ma la precisione è bassa."
                hasUsedFallback -> "Ho trovato ${articles.size} articoli usando criteri più permissivi."
                else -> "Ho trovato ${articles.size} articoli corrispondenti."
            }
        }
    }

    companion object {
        // Soglie riequilibrate per ridurre confusione
        private const val STRICT_THRESHOLD = 0.5     // Era 0.4 - ora più restrittivo
        private const val NORMAL_THRESHOLD = 0.30    // Era 0.25 - più selettivo
        private const val LOOSE_THRESHOLD = 0.20     // Era 0.15 - manteniamo per fallback
        private const val FALLBACK_THRESHOLD = 0.15  // Era 0.1 - ultima risorsa
    }
}
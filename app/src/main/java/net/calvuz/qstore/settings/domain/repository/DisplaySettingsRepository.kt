package net.calvuz.qstore.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings

/**
 * Repository per le impostazioni di visualizzazione.
 *
 * Gestisce la persistenza delle preferenze relative all'aspetto
 * e al layout dell'interfaccia utente.
 *
 * Implementazione tipica: DataStore Preferences
 */
interface DisplaySettingsRepository {

    /**
     * Osserva le impostazioni di visualizzazione.
     * Emette un nuovo valore ogni volta che cambiano.
     */
    fun getSettings(): Flow<DisplaySettings>

    /**
     * Aggiorna tutte le impostazioni di visualizzazione.
     */
    suspend fun updateSettings(settings: DisplaySettings)

    /**
     * Ripristina le impostazioni ai valori di default.
     */
    suspend fun resetToDefault()

    // === Metodi di convenienza per singole preferenze ===

    /**
     * Osserva lo stile delle card articoli.
     */
    fun getArticleCardStyle(): Flow<ArticleCardStyle>

    /**
     * Aggiorna lo stile delle card articoli.
     */
    suspend fun setArticleCardStyle(style: ArticleCardStyle)

    /**
     * Osserva se mostrare gli indicatori di stock.
     */
    fun getShowStockIndicators(): Flow<Boolean>

    /**
     * Aggiorna la visibilità degli indicatori di stock.
     */
    suspend fun setShowStockIndicators(show: Boolean)

    /**
     * Osserva se mostrare le immagini degli articoli.
     */
    fun getShowArticleImages(): Flow<Boolean>

    /**
     * Aggiorna la visibilità delle immagini articoli.
     */
    suspend fun setShowArticleImages(show: Boolean)
}

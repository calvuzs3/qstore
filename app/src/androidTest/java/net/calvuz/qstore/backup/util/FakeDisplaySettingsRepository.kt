package net.calvuz.qstore.backup.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository

/**
 * Implementazione in-memory di [DisplaySettingsRepository] per i test — evita di dipendere
 * da DataStore/file system reali, che non si azzerano automaticamente tra un test e l'altro.
 */
class FakeDisplaySettingsRepository(
    initial: DisplaySettings = DisplaySettings.getDefault()
) : DisplaySettingsRepository {

    private val state = MutableStateFlow(initial)

    override fun getSettings(): Flow<DisplaySettings> = state

    override suspend fun updateSettings(settings: DisplaySettings) {
        state.value = settings
    }

    override suspend fun resetToDefault() {
        state.value = DisplaySettings.getDefault()
    }

    override fun getArticleCardStyle(): Flow<ArticleCardStyle> = state.map { it.articleCardStyle }

    override suspend fun setArticleCardStyle(style: ArticleCardStyle) {
        state.value = state.value.copy(articleCardStyle = style)
    }

    override fun getShowStockIndicators(): Flow<Boolean> = state.map { it.showStockIndicators }

    override suspend fun setShowStockIndicators(show: Boolean) {
        state.value = state.value.copy(showStockIndicators = show)
    }

    override fun getShowArticleImages(): Flow<Boolean> = state.map { it.showArticleImages }

    override suspend fun setShowArticleImages(show: Boolean) {
        state.value = state.value.copy(showArticleImages = show)
    }

    override fun getShowArticleActions(): Flow<Boolean> = state.map { it.showArticleActions }

    override suspend fun setShowArticleActions(show: Boolean) {
        state.value = state.value.copy(showArticleActions = show)
    }

    override suspend fun setShowDashboardStats(show: Boolean) {
        state.value = state.value.copy(showDashboardStats = show)
    }

    override suspend fun setShowRecentMovements(show: Boolean) {
        state.value = state.value.copy(showRecentMovements = show)
    }

    override suspend fun setShowRecentArticles(show: Boolean) {
        state.value = state.value.copy(showRecentArticles = show)
    }

    /** Valore corrente, per asserzioni dirette nei test senza dover collezionare il Flow. */
    fun current(): DisplaySettings = state.value
}

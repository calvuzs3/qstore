package net.calvuz.qstore.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qstore.settings.domain.model.ArticleCardStyle
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

// Extension per DataStore
private val Context.displaySettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "display_settings"
)

/**
 * Implementazione di [DisplaySettingsRepository] usando DataStore Preferences.
 *
 * Le preferenze sono persistite nel file "display_settings.preferences_pb"
 * nella directory dell'app.
 */
@Singleton
class DisplaySettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DisplaySettingsRepository {

    private val dataStore = context.displaySettingsDataStore

    // === Keys per DataStore ===
    private object PreferenceKeys {
        val ARTICLE_CARD_STYLE = stringPreferencesKey("article_card_style")
        val SHOW_STOCK_INDICATORS = booleanPreferencesKey("show_stock_indicators")
        val SHOW_ARTICLE_IMAGES = booleanPreferencesKey("show_article_images")
        val SHOW_ARTICLE_ACTIONS = booleanPreferencesKey("show_article_actions")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_DASHBOARD_STATS = booleanPreferencesKey("show_dashboard_stats")
        val SHOW_RECENT_MOVEMENTS = booleanPreferencesKey("show_recent_movements")
        val SHOW_RECENT_ARTICLES = booleanPreferencesKey("show_recent_articles")
    }

    // === Implementazione Repository ===

    override fun getSettings(): Flow<DisplaySettings> {
        return dataStore.data.map { preferences ->
            DisplaySettings(
                articleCardStyle = ArticleCardStyle.fromName(
                    preferences[PreferenceKeys.ARTICLE_CARD_STYLE]
                ),
                showStockIndicators = preferences[PreferenceKeys.SHOW_STOCK_INDICATORS] ?: true,
                showArticleImages = preferences[PreferenceKeys.SHOW_ARTICLE_IMAGES] ?: true,
                showArticleActions = preferences[PreferenceKeys.SHOW_ARTICLE_ACTIONS] ?: true,
                gridColumns = preferences[PreferenceKeys.GRID_COLUMNS] ?: 1,
                showDashboardStats = preferences[PreferenceKeys.SHOW_DASHBOARD_STATS] ?: true,
                showRecentMovements = preferences[PreferenceKeys.SHOW_RECENT_MOVEMENTS] ?: true,
                showRecentArticles = preferences[PreferenceKeys.SHOW_RECENT_ARTICLES] ?: true
            )
        }
    }

    override suspend fun updateSettings(settings: DisplaySettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ARTICLE_CARD_STYLE] = settings.articleCardStyle.name
            preferences[PreferenceKeys.SHOW_STOCK_INDICATORS] = settings.showStockIndicators
            preferences[PreferenceKeys.SHOW_ARTICLE_IMAGES] = settings.showArticleImages
            preferences[PreferenceKeys.SHOW_ARTICLE_ACTIONS] = settings.showArticleActions
            preferences[PreferenceKeys.GRID_COLUMNS] = settings.gridColumns
            preferences[PreferenceKeys.SHOW_DASHBOARD_STATS] = settings.showDashboardStats
            preferences[PreferenceKeys.SHOW_RECENT_MOVEMENTS] = settings.showRecentMovements
            preferences[PreferenceKeys.SHOW_RECENT_ARTICLES] = settings.showRecentArticles
        }
    }

    override suspend fun resetToDefault() {
        val default = DisplaySettings.getDefault()
        updateSettings(default)
    }

    // === Metodi di convenienza ===

    override fun getArticleCardStyle(): Flow<ArticleCardStyle> {
        return dataStore.data.map { preferences ->
            ArticleCardStyle.fromName(preferences[PreferenceKeys.ARTICLE_CARD_STYLE])
        }
    }

    override suspend fun setArticleCardStyle(style: ArticleCardStyle) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ARTICLE_CARD_STYLE] = style.name
        }
    }

    override fun getShowStockIndicators(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.SHOW_STOCK_INDICATORS] ?: true
        }
    }

    override suspend fun setShowStockIndicators(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_STOCK_INDICATORS] = show
        }
    }

    override fun getShowArticleImages(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.SHOW_ARTICLE_IMAGES] ?: true
        }
    }

    override suspend fun setShowArticleImages(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_ARTICLE_IMAGES] = show
        }
    }

    override fun getShowArticleActions(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.SHOW_ARTICLE_ACTIONS] ?: true
        }
    }

    override suspend fun setShowArticleActions(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_ARTICLE_ACTIONS] = show
        }
    }

    override suspend fun setShowDashboardStats(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_DASHBOARD_STATS] = show
        }
    }

    override suspend fun setShowRecentMovements(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_RECENT_MOVEMENTS] = show
        }
    }

    override suspend fun setShowRecentArticles(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_RECENT_ARTICLES] = show
        }
    }
}

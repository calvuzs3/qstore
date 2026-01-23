package net.calvuz.qstore.presentation.ui.camera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.calvuz.qstore.data.opencv.ImageRecognitionValidator
import net.calvuz.qstore.domain.usecase.recognition.SaveArticleImageUseCase
import net.calvuz.qstore.domain.usecase.recognition.SearchArticleByImageUseCase
import net.calvuz.qstore.domain.model.Article
import javax.inject.Inject

/**
 * ViewModel per Camera Screen
 *
 * Gestisce:
 * - Cattura foto
 * - Ricerca articoli per immagine con algoritmi migliorati
 * - Salvataggio immagine articolo
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val saveArticleImageUseCase: SaveArticleImageUseCase,
    private val searchArticleByImageUseCase: SearchArticleByImageUseCase,
    private val imageRecognitionValidator: ImageRecognitionValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Ready)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /**
     * Cerca articoli che corrispondono alla foto catturata
     * Usa la nuova API con SearchResult
     */
    fun searchByImage(
        imageBytes: ByteArray,
        searchMode: SearchArticleByImageUseCase.SearchMode = SearchArticleByImageUseCase.SearchMode.NORMAL
    ) {
        // Imposta immediatamente lo stato di ricerca
        _uiState.value = CameraUiState.Searching

        viewModelScope.launch {
            // Debug dell'immagine prima della ricerca
            debugImageQuality(imageBytes)

            searchArticleByImageUseCase(imageBytes, searchMode)
                .onSuccess { searchResult ->
                    if (searchResult.isEmpty) {
                        _uiState.value = CameraUiState.NoResults(
                            message = searchResult.getUserMessage(),
                            searchResult = searchResult
                        )
                    } else {
                        _uiState.value = CameraUiState.SearchSuccess(
                            searchResult = searchResult
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = CameraUiState.Error(
                        error.message ?: "Errore durante la ricerca"
                    )
                }
        }
    }

    /**
     * Cerca con modalità strict (alta precisione)
     */
    fun searchByImageStrict(imageBytes: ByteArray) {
        searchByImage(imageBytes, SearchArticleByImageUseCase.SearchMode.STRICT)
    }

    /**
     * Cerca con modalità loose (più permissiva)
     */
    fun searchByImageLoose(imageBytes: ByteArray) {
        searchByImage(imageBytes, SearchArticleByImageUseCase.SearchMode.LOOSE)
    }

    /**
     * Cerca con più soglie in sequenza per massimizzare risultati
     */
    fun searchByImageMultiThreshold(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Searching

            searchArticleByImageUseCase.searchMultiThreshold(imageBytes)
                .onSuccess { searchResult ->
                    if (searchResult.isEmpty) {
                        _uiState.value = CameraUiState.NoResults(
                            message = "Nessun articolo trovato anche con ricerca estesa",
                            searchResult = searchResult
                        )
                    } else {
                        _uiState.value = CameraUiState.SearchSuccess(
                            searchResult = searchResult
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = CameraUiState.Error(
                        error.message ?: "Errore durante la ricerca estesa"
                    )
                }
        }
    }

    /**
     * Salva immagine per un articolo esistente
     */
    fun saveImage(articleUuid: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Saving

            saveArticleImageUseCase(articleUuid, imageBytes)
                .onSuccess { savedImage ->
                    _uiState.value = CameraUiState.SaveSuccess(savedImage.id)
                }
                .onFailure { error ->
                    _uiState.value = CameraUiState.Error(
                        error.message ?: "Errore durante il salvataggio"
                    )
                }
        }
    }

    /**
     * Riprova ricerca con soglia più permissiva se non ci sono risultati
     */
    fun retrySearchWithLowerThreshold(imageBytes: ByteArray) {
        searchByImageLoose(imageBytes)
    }

    /**
     * Reset stato a Ready
     */
    fun resetState() {
        _uiState.value = CameraUiState.Ready
    }

    /**
     * Ottieni suggerimenti basati sui risultati della ricerca
     */
    fun getSuggestionsFromResult(searchResult: SearchArticleByImageUseCase.SearchResult): List<String> {
        return when {
            searchResult.isEmpty -> listOf(
                "Prova a scattare una foto più vicina all'oggetto",
                "Assicurati che l'illuminazione sia buona",
                "L'oggetto potrebbe non essere ancora nel database"
            )
            searchResult.isLowConfidence -> listOf(
                "I risultati potrebbero non essere precisi",
                "Controlla manualmente i risultati",
                "Prova a scattare un'altra foto da angolazione diversa"
            )
            searchResult.hasUsedFallback -> listOf(
                "Risultati trovati con criteri più permissivi",
                "Verifica che sia l'articolo corretto"
            )
            else -> listOf(
                "Risultati trovati con buona precisione",
                "Puoi procedere con sicurezza"
            )
        }
    }

    // =============================================================================
    // METODI DI DEBUG - Chiamati automaticamente durante searchByImage
    // =============================================================================

    /**
     * Debug della qualità dell'immagine - chiamato automaticamente
     */
    private fun debugImageQuality(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                Log.d("DEBUG_CAMERA", "🔍 === IMAGE QUALITY ANALYSIS ===")

                val qualityResult = imageRecognitionValidator.validateImageQuality(imageBytes)
                Log.d("DEBUG_CAMERA", "📸 Valid: ${qualityResult.isValid}")
                Log.d("DEBUG_CAMERA", "📊 Score: ${qualityResult.score}")
                Log.d("DEBUG_CAMERA", "⭐ Quality: ${qualityResult.getQualityLabel()}")
                Log.d("DEBUG_CAMERA", "🚨 Issues: ${qualityResult.issues}")
                Log.d("DEBUG_CAMERA", "📐 Resolution: ${qualityResult.resolution}")
                Log.d("DEBUG_CAMERA", "💾 File size: ${qualityResult.fileSize?.let { "${it / 1024}KB" }}")

                // Test anche la performance di matching
                val performanceResult = imageRecognitionValidator.testMatchingPerformance(imageBytes)
                Log.d("DEBUG_CAMERA", "🔍 === MATCHING PERFORMANCE ===")
                Log.d("DEBUG_CAMERA", "✅ Success: ${performanceResult.success}")
                if (performanceResult.success) {
                    Log.d("DEBUG_CAMERA", "🗂️ Total images in DB: ${performanceResult.totalImagesInDb}")
                    Log.d("DEBUG_CAMERA", "✅ Valid descriptors: ${performanceResult.validDescriptors}")
                    Log.d("DEBUG_CAMERA", "🎯 Matches found: ${performanceResult.matchesFound}")
                    Log.d("DEBUG_CAMERA", "📈 Best similarity: ${performanceResult.bestSimilarity}")
                    Log.d("DEBUG_CAMERA", "⏱️ Total time: ${performanceResult.totalTimeMs}ms")
                    Log.d("DEBUG_CAMERA", "🔄 Matching time: ${performanceResult.matchingTimeMs}ms")
                } else {
                    Log.e("DEBUG_CAMERA", "❌ Error: ${performanceResult.error}")
                }

            } catch (e: Exception) {
                Log.e("DEBUG_CAMERA", "💥 Debug failed", e)
            }
        }
    }

    /**
     * Analizza la qualità del database - chiamata manualmente se necessario
     */
    fun debugDatabaseQuality() {
        viewModelScope.launch {
            try {
                Log.d("DEBUG_DATABASE", "🗄️ === DATABASE ANALYSIS ===")

                val dbAnalysis = imageRecognitionValidator.analyzeDatabaseQuality()
                Log.d("DEBUG_DATABASE", "📊 Total images: ${dbAnalysis.totalImages}")
                Log.d("DEBUG_DATABASE", "✅ Valid images: ${dbAnalysis.validImages}")
                Log.d("DEBUG_DATABASE", "❌ Corrupted images: ${dbAnalysis.corruptedImages}")
                Log.d("DEBUG_DATABASE", "⭐ Average quality: ${dbAnalysis.averageQuality}")
                Log.d("DEBUG_DATABASE", "🔢 Average features: ${dbAnalysis.averageFeatures}")

                if (dbAnalysis.issues.isNotEmpty()) {
                    Log.w("DEBUG_DATABASE", "🚨 Issues found:")
                    dbAnalysis.issues.forEach { issue ->
                        Log.w("DEBUG_DATABASE", "   • $issue")
                    }
                }

            } catch (e: Exception) {
                Log.e("DEBUG_DATABASE", "💥 Database analysis failed", e)
            }
        }
    }
}

/**
 * Stati UI della Camera Screen aggiornati
 */
sealed class CameraUiState {
    /** Pronto per catturare foto */
    data object Ready : CameraUiState()

    /** Ricerca in corso */
    data object Searching : CameraUiState()

    /** Salvataggio in corso */
    data object Saving : CameraUiState()

    /** Ricerca completata con successo */
    data class SearchSuccess(
        val searchResult: SearchArticleByImageUseCase.SearchResult
    ) : CameraUiState() {
        val articles: List<Article> get() = searchResult.articles
        val confidence: Double get() = searchResult.confidence
        val isHighConfidence: Boolean get() = searchResult.isHighConfidence
        val userMessage: String get() = searchResult.getUserMessage()
    }

    /** Nessun risultato trovato */
    data class NoResults(
        val message: String,
        val searchResult: SearchArticleByImageUseCase.SearchResult
    ) : CameraUiState()

    /** Salvataggio completato */
    data class SaveSuccess(val imageId: Long) : CameraUiState()

    /** Errore */
    data class Error(val message: String) : CameraUiState()
}
package net.calvuz.qstore.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.domain.model.RecognitionSettings

/**
 * Repository interface per gestione impostazioni riconoscimento immagini
 */
interface RecognitionSettingsRepository {

    /**
     * Ottiene le impostazioni correnti come Flow reattivo
     */
    fun getSettings(): Flow<RecognitionSettings>

    /**
     * Aggiorna le impostazioni correnti
     */
    suspend fun updateSettings(settings: RecognitionSettings)

    /**
     * Applica un preset predefinito
     * @param presetName "Preciso", "Bilanciato", o "Veloce"
     */
    suspend fun applyPreset(presetName: String)

    /**
     * Ottiene il nome del preset correntemente attivo
     */
    fun getCurrentPreset(): Flow<String?>

    /**
     * Ripristina alle impostazioni di default
     */
    suspend fun resetToDefault()
}

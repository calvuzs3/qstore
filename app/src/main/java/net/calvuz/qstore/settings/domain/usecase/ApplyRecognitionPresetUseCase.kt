package net.calvuz.qstore.settings.domain.usecase

import net.calvuz.qstore.settings.domain.repository.RecognitionSettingsRepository
import javax.inject.Inject

/**
 * Use case per applicare preset
 */
class ApplyRecognitionPresetUseCase @Inject constructor(
    private val repository: RecognitionSettingsRepository
) {
    suspend operator fun invoke(presetName: String): Result<Unit> {
        return try {
            if (presetName !in listOf("Preciso", "Bilanciato", "Veloce")) {
                return Result.failure(IllegalArgumentException("Preset non riconosciuto: $presetName"))
            }

            repository.applyPreset(presetName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
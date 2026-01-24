package net.calvuz.qstore.app.domain.usecase.settings

import net.calvuz.qstore.app.domain.repository.RecognitionSettingsRepository
import javax.inject.Inject

/**
 * Use case per reset alle impostazioni di default
 */
class ResetRecognitionSettingsUseCase @Inject constructor(
    private val repository: RecognitionSettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            repository.resetToDefault()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
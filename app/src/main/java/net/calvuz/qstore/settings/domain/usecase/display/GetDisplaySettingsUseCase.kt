package net.calvuz.qstore.settings.domain.usecase.display

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.settings.domain.model.DisplaySettings
import net.calvuz.qstore.settings.domain.repository.DisplaySettingsRepository
import javax.inject.Inject

class GetDisplaySettingsUseCase @Inject constructor(
    private val repository: DisplaySettingsRepository
) {
    operator fun invoke(): Flow<DisplaySettings> = repository.getSettings()
}
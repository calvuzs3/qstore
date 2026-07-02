package net.calvuz.qstore.sync.domain.usecase

import net.calvuz.qstore.sync.domain.repository.SyncSettingsRepository
import javax.inject.Inject

class SetAllowMeteredNetworkUseCase @Inject constructor(
    private val syncSettingsRepository: SyncSettingsRepository
) {
    suspend operator fun invoke(allow: Boolean) = syncSettingsRepository.setAllowMeteredNetworkForImages(allow)
}

package net.calvuz.qstore.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.calvuz.qstore.sync.domain.repository.SyncSettingsRepository
import javax.inject.Inject

class ObserveAllowMeteredNetworkUseCase @Inject constructor(
    private val syncSettingsRepository: SyncSettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = syncSettingsRepository.observeAllowMeteredNetworkForImages()
}

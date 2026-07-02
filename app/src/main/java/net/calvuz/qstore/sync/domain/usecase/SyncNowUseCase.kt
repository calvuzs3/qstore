package net.calvuz.qstore.sync.domain.usecase

import net.calvuz.qstore.sync.domain.model.SyncSummary
import net.calvuz.qstore.sync.domain.repository.SyncRepository
import javax.inject.Inject

class SyncNowUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<SyncSummary> = syncRepository.syncNow()
}

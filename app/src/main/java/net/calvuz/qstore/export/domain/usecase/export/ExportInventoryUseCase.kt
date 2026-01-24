package net.calvuz.qstore.export.domain.usecase.export

import net.calvuz.qstore.export.domain.model.ExportOptions
import net.calvuz.qstore.export.domain.model.ExportResult
import net.calvuz.qstore.export.domain.repository.ExportRepository
import javax.inject.Inject

class ExportInventoryUseCase @Inject constructor(
    private val exportRepository: ExportRepository
) {
    suspend operator fun invoke(options: ExportOptions): ExportResult {
        return exportRepository.exportInventory(options)
    }
}

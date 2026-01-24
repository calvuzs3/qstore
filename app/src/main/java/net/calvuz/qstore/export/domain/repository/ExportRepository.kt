package net.calvuz.qstore.export.domain.repository

import net.calvuz.qstore.export.domain.model.ExportOptions
import net.calvuz.qstore.export.domain.model.ExportResult

interface ExportRepository {
    suspend fun exportInventory(options: ExportOptions): ExportResult
}

package net.calvuz.qstore.export.domain.model

enum class ExportFormat {
    CSV,
    EXCEL
}

data class ExportOptions(
    val format: ExportFormat,
    val includePhotos: Boolean = false,
    val fileName: String = "inventory_export"
)

sealed class ExportResult {
    data class Success(val filePath: String, val itemCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

/**
 * Dati aggregati per l'esportazione inventario.
 * Combina Article + Inventory + Category + Images paths.
 */
data class InventoryExportItem(
    val articleUuid: String,
    val name: String,
    val description: String,
    val categoryName: String,
    val unitOfMeasure: String,
    val currentQuantity: Double,
    val reorderLevel: Double,
    val codeOEM: String,
    val codeERP: String,
    val codeBM: String,
    val notes: String,
    val imagePaths: List<String> = emptyList()
)

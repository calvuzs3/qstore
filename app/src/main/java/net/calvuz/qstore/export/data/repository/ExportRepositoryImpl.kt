package net.calvuz.qstore.export.data.repository

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.calvuz.qstore.app.domain.repository.ArticleRepository
import net.calvuz.qstore.app.domain.repository.ImageRecognitionRepository
import net.calvuz.qstore.app.domain.repository.InventoryRepository
import net.calvuz.qstore.categories.domain.repository.ArticleCategoryRepository
import net.calvuz.qstore.export.domain.model.ExportFormat
import net.calvuz.qstore.export.domain.model.ExportOptions
import net.calvuz.qstore.export.domain.model.ExportResult
import net.calvuz.qstore.export.domain.model.InventoryExportItem
import net.calvuz.qstore.export.domain.repository.ExportRepository
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val articleRepository: ArticleRepository,
    private val inventoryRepository: InventoryRepository,
    private val categoryRepository: ArticleCategoryRepository,
    private val imageRecognitionRepository: ImageRecognitionRepository
) : ExportRepository {

    private val excelWriter = SimpleExcelWriter()

    override suspend fun exportInventory(options: ExportOptions): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                val items = buildExportItems(options.includePhotos)
                if (items.isEmpty()) {
                    return@withContext ExportResult.Error("Nessun articolo da esportare")
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val baseFileName = "${options.fileName}_$timestamp"

                val exportDir = getExportDirectory()
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val filePath = when (options.format) {
                    ExportFormat.CSV -> exportToCsv(items, exportDir, baseFileName, options.includePhotos)
                    ExportFormat.EXCEL -> exportToExcel(items, exportDir, baseFileName, options.includePhotos)
                }

                ExportResult.Success(filePath, items.size)
            } catch (e: Exception) {
                ExportResult.Error("Errore esportazione: ${e.message}")
            }
        }

    private suspend fun buildExportItems(includePhotos: Boolean): List<InventoryExportItem> {
        val articles = articleRepository.observeAll().first()
        val categories = categoryRepository.observeAll().first()
        val categoryMap = categories.associateBy { it.uuid }

        return articles.map { article ->
            val inventory = inventoryRepository.getByArticleUuid(article.uuid)
            val category = categoryMap[article.categoryId]

            val imagePaths = if (includePhotos) {
                imageRecognitionRepository.getArticleImages(article.uuid)
                    .getOrNull()
                    ?.map { it.imagePath }
                    ?: emptyList()
            } else {
                emptyList()
            }

            InventoryExportItem(
                articleUuid = article.uuid,
                name = article.name,
                description = article.description,
                categoryName = category?.name ?: "",
                unitOfMeasure = article.unitOfMeasure,
                currentQuantity = inventory?.currentQuantity ?: 0.0,
                reorderLevel = article.reorderLevel,
                codeOEM = article.codeOEM,
                codeERP = article.codeERP,
                codeBM = article.codeBM,
                notes = article.notes,
                imagePaths = imagePaths
            )
        }
    }

    private fun exportToCsv(
        items: List<InventoryExportItem>,
        exportDir: File,
        baseFileName: String,
        includePhotos: Boolean
    ): String {
        val csvFile = File(exportDir, "$baseFileName.csv")

        FileWriter(csvFile).use { writer ->
            val headers = mutableListOf(
                "UUID", "Nome", "Descrizione", "Categoria", "Unità",
                "Quantità", "Livello Riordino",
                "Codice OEM", "Codice ERP", "Codice BM", "Note"
            )
            if (includePhotos) {
                headers.add("Foto")
            }
            writer.write(headers.joinToString(";") + "\n")

            items.forEach { item ->
                val row = mutableListOf(
                    escapeCSV(item.articleUuid),
                    escapeCSV(item.name),
                    escapeCSV(item.description),
                    escapeCSV(item.categoryName),
                    escapeCSV(item.unitOfMeasure),
                    formatQuantity(item.currentQuantity),
                    formatQuantity(item.reorderLevel),
                    escapeCSV(item.codeOEM),
                    escapeCSV(item.codeERP),
                    escapeCSV(item.codeBM),
                    escapeCSV(item.notes)
                )
                if (includePhotos) {
                    row.add(escapeCSV(item.imagePaths.joinToString(",")))
                }
                writer.write(row.joinToString(";") + "\n")
            }
        }

        return if (includePhotos && items.any { it.imagePaths.isNotEmpty() }) {
            createZipWithPhotos(csvFile, items, exportDir, baseFileName)
        } else {
            csvFile.absolutePath
        }
    }

    private fun exportToExcel(
        items: List<InventoryExportItem>,
        exportDir: File,
        baseFileName: String,
        includePhotos: Boolean
    ): String {
        val headers = mutableListOf(
            "UUID", "Nome", "Descrizione", "Categoria", "Unità",
            "Quantità", "Livello Riordino",
            "Codice OEM", "Codice ERP", "Codice BM", "Note"
        )
        if (includePhotos) {
            headers.add("Foto")
        }

        val rows = items.map { item ->
            val row = mutableListOf<Any?>(
                item.articleUuid,
                item.name,
                item.description,
                item.categoryName,
                item.unitOfMeasure,
                item.currentQuantity,
                item.reorderLevel,
                item.codeOEM,
                item.codeERP,
                item.codeBM,
                item.notes
            )
            if (includePhotos) {
                row.add(item.imagePaths.joinToString(", "))
            }
            row
        }

        val excelFile = File(exportDir, "$baseFileName.xlsx")
        excelWriter.writeExcel(excelFile, "Inventario", headers, rows)

        return if (includePhotos && items.any { it.imagePaths.isNotEmpty() }) {
            createZipWithPhotos(excelFile, items, exportDir, baseFileName)
        } else {
            excelFile.absolutePath
        }
    }

    private fun createZipWithPhotos(
        dataFile: File,
        items: List<InventoryExportItem>,
        exportDir: File,
        baseFileName: String
    ): String {
        val zipFile = File(exportDir, "$baseFileName.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(dataFile.name))
            dataFile.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()

            val addedPhotos = mutableSetOf<String>()
            items.forEach { item ->
                item.imagePaths.forEach { imagePath ->
                    if (imagePath !in addedPhotos) {
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            zipOut.putNextEntry(ZipEntry("photos/${imageFile.name}"))
                            imageFile.inputStream().use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                            addedPhotos.add(imagePath)
                        }
                    }
                }
            }
        }

        dataFile.delete()
        return zipFile.absolutePath
    }

    private fun getExportDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "QStore/Export"
        )
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatQuantity(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}

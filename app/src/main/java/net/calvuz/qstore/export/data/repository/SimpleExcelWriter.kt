package net.calvuz.qstore.export.data.repository

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generatore Excel leggero per Android.
 * Scrive file XLSX senza dipendenze esterne (Apache POI).
 * 
 * XLSX è un formato ZIP contenente file XML.
 */
class SimpleExcelWriter {

    fun writeExcel(
        file: File,
        sheetName: String,
        headers: List<String>,
        rows: List<List<Any?>>
    ) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            // [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypesXml().toByteArray())
            zip.closeEntry()

            // _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(relsXml().toByteArray())
            zip.closeEntry()

            // xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(workbookRelsXml().toByteArray())
            zip.closeEntry()

            // xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(workbookXml(sheetName).toByteArray())
            zip.closeEntry()

            // xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(stylesXml().toByteArray())
            zip.closeEntry()

            // xl/sharedStrings.xml
            val allStrings = collectStrings(headers, rows)
            val stringIndex = allStrings.withIndex().associate { it.value to it.index }
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml(allStrings).toByteArray())
            zip.closeEntry()

            // xl/worksheets/sheet1.xml
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml(headers, rows, stringIndex).toByteArray())
            zip.closeEntry()
        }
    }

    private fun collectStrings(headers: List<String>, rows: List<List<Any?>>): List<String> {
        val strings = mutableListOf<String>()
        headers.forEach { strings.add(it) }
        rows.forEach { row ->
            row.forEach { cell ->
                if (cell is String && cell.isNotEmpty()) {
                    if (cell !in strings) strings.add(cell)
                }
            }
        }
        return strings
    }

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private fun relsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    private fun workbookXml(sheetName: String) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="2">
        <font><sz val="11"/><name val="Calibri"/></font>
        <font><b/><sz val="11"/><name val="Calibri"/></font>
    </fonts>
    <fills count="2">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
    </fills>
    <borders count="1">
        <border><left/><right/><top/><bottom/><diagonal/></border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    </cellXfs>
</styleSheet>"""

    private fun sharedStringsXml(strings: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
        strings.forEach { s ->
            sb.append("<si><t>${escapeXml(s)}</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun sheetXml(
        headers: List<String>,
        rows: List<List<Any?>>,
        stringIndex: Map<String, Int>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        
        // Column widths
        sb.append("<cols>")
        headers.forEachIndexed { index, _ ->
            val width = when (index) {
                0 -> 36    // UUID
                1 -> 25    // Nome
                2 -> 40    // Descrizione
                10 -> 30   // Note
                else -> 15
            }
            sb.append("""<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>""")
        }
        sb.append("</cols>")
        
        sb.append("<sheetData>")

        // Header row
        sb.append("""<row r="1">""")
        headers.forEachIndexed { colIndex, header ->
            val cellRef = getCellRef(colIndex, 0)
            val strIdx = stringIndex[header] ?: 0
            sb.append("""<c r="$cellRef" t="s" s="1"><v>$strIdx</v></c>""")
        }
        sb.append("</row>")

        // Data rows
        rows.forEachIndexed { rowIndex, row ->
            val rowNum = rowIndex + 2
            sb.append("""<row r="$rowNum">""")
            row.forEachIndexed { colIndex, cell ->
                val cellRef = getCellRef(colIndex, rowIndex + 1)
                when (cell) {
                    is Number -> {
                        sb.append("""<c r="$cellRef"><v>$cell</v></c>""")
                    }
                    is String -> {
                        if (cell.isEmpty()) {
                            sb.append("""<c r="$cellRef"/>""")
                        } else {
                            val strIdx = stringIndex[cell] ?: 0
                            sb.append("""<c r="$cellRef" t="s"><v>$strIdx</v></c>""")
                        }
                    }
                    null -> {
                        sb.append("""<c r="$cellRef"/>""")
                    }
                    else -> {
                        val str = cell.toString()
                        val strIdx = stringIndex[str] ?: 0
                        sb.append("""<c r="$cellRef" t="s"><v>$strIdx</v></c>""")
                    }
                }
            }
            sb.append("</row>")
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun getCellRef(col: Int, row: Int): String {
        val colLetter = buildString {
            var c = col
            while (c >= 0) {
                insert(0, ('A' + (c % 26)))
                c = c / 26 - 1
            }
        }
        return "$colLetter${row + 1}"
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

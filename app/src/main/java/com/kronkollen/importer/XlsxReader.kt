package com.kronkollen.importer

import android.util.Xml
import com.kronkollen.util.Dates
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * A small, dependency-free .xlsx reader. An xlsx is a zip of XML parts; we read only the
 * three we need (shared strings, styles, the first worksheet) using Android's built-in
 * pull parser. Numeric cells whose style is a date format are normalised to ISO
 * yyyy-MM-dd so the mapping UI sees real dates instead of Excel serial numbers.
 *
 * This deliberately avoids Apache POI / fastexcel (the latter needs javax.xml.stream,
 * which Android does not ship).
 */
object XlsxReader {

    /** Built-in numFmtId values that represent dates. */
    private val BUILTIN_DATE_IDS = setOf(14, 15, 16, 17, 22)

    fun read(input: InputStream): SheetData {
        var sharedStringsXml: ByteArray? = null
        var stylesXml: ByteArray? = null
        val worksheets = mutableMapOf<String, ByteArray>()

        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == "xl/sharedStrings.xml" -> sharedStringsXml = zip.readBytes()
                    name == "xl/styles.xml" -> stylesXml = zip.readBytes()
                    name.startsWith("xl/worksheets/") && name.endsWith(".xml") ->
                        worksheets[name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sharedStrings = sharedStringsXml?.let(::parseSharedStrings) ?: emptyList()
        val dateStyleIndexes = stylesXml?.let(::parseDateStyles) ?: emptySet()

        val sheetBytes = worksheets.entries
            .sortedBy { it.key }
            .firstOrNull()?.value
            ?: return SheetData(emptyList())

        return parseSheet(sheetBytes, sharedStrings, dateStyleIndexes)
    }

    // ---- shared strings -----------------------------------------------------

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = newParser(bytes)
        var current = StringBuilder()
        var inSi = false
        var inT = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { inSi = true; current = StringBuilder() }
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inSi && inT) current.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "si" -> { result.add(current.toString()); inSi = false }
                }
            }
            event = parser.next()
        }
        return result
    }

    // ---- styles (which cell-style indexes are dates) ------------------------

    private fun parseDateStyles(bytes: ByteArray): Set<Int> {
        val customDateFmtIds = mutableSetOf<Int>()
        val cellXfNumFmtIds = mutableListOf<Int>()
        val parser = newParser(bytes)
        var inCellXfs = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "numFmt" -> {
                        val id = parser.getAttributeValue(null, "numFmtId")?.toIntOrNull()
                        val code = parser.getAttributeValue(null, "formatCode")?.lowercase()
                        if (id != null && code != null && isDateFormatCode(code)) {
                            customDateFmtIds.add(id)
                        }
                    }
                    "cellXfs" -> inCellXfs = true
                    "xf" -> if (inCellXfs) {
                        val id = parser.getAttributeValue(null, "numFmtId")?.toIntOrNull() ?: 0
                        cellXfNumFmtIds.add(id)
                    }
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "cellXfs") {
                inCellXfs = false
            }
            event = parser.next()
        }
        // Map each cell-style index to whether its numFmt is a date format.
        val dateStyleIndexes = mutableSetOf<Int>()
        cellXfNumFmtIds.forEachIndexed { index, numFmtId ->
            if (numFmtId in BUILTIN_DATE_IDS || numFmtId in customDateFmtIds) {
                dateStyleIndexes.add(index)
            }
        }
        return dateStyleIndexes
    }

    private fun isDateFormatCode(code: String): Boolean {
        // Strip quoted literals and colour/locale tokens, then look for date tokens.
        val stripped = code.replace(Regex("\\[[^]]*]"), "").replace(Regex("\".*?\""), "")
        return stripped.contains('y') || stripped.contains('d') ||
            (stripped.contains('m') && !stripped.contains("mm:")) // avoid time-only mm:ss
    }

    // ---- worksheet ----------------------------------------------------------

    private fun parseSheet(
        bytes: ByteArray,
        sharedStrings: List<String>,
        dateStyleIndexes: Set<Int>,
    ): SheetData {
        val rows = mutableListOf<List<String>>()
        val parser = newParser(bytes)

        var currentRow: MutableList<String> = mutableListOf()
        var cellType: String? = null
        var cellStyle: Int = -1
        var cellCol: Int = -1
        var valueBuilder = StringBuilder()
        var inValue = false
        var inInlineString = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        cellType = parser.getAttributeValue(null, "t")
                        cellStyle = parser.getAttributeValue(null, "s")?.toIntOrNull() ?: -1
                        cellCol = columnIndex(parser.getAttributeValue(null, "r"))
                        valueBuilder = StringBuilder()
                    }
                    "v" -> inValue = true
                    "is" -> inInlineString = true
                    "t" -> if (inInlineString) inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) valueBuilder.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inValue = false
                    "t" -> if (inInlineString) inValue = false
                    "is" -> inInlineString = false
                    "c" -> {
                        val value = resolveCell(
                            cellType, cellStyle, valueBuilder.toString(),
                            sharedStrings, dateStyleIndexes,
                        )
                        setAt(currentRow, if (cellCol >= 0) cellCol else currentRow.size, value)
                    }
                    "row" -> rows.add(currentRow)
                }
            }
            event = parser.next()
        }
        return SheetData(rows)
    }

    private fun resolveCell(
        type: String?,
        styleIndex: Int,
        rawValue: String,
        sharedStrings: List<String>,
        dateStyleIndexes: Set<Int>,
    ): String {
        if (rawValue.isEmpty()) return ""
        return when (type) {
            "s" -> rawValue.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
            "inlineStr", "str" -> rawValue
            "b" -> if (rawValue == "1") "TRUE" else "FALSE"
            else -> {
                // Numeric. If the cell carries a date style, render as ISO date.
                val number = rawValue.toDoubleOrNull()
                if (number != null && styleIndex in dateStyleIndexes) {
                    Dates.displayIso(Dates.fromExcelSerial(number))
                } else {
                    rawValue
                }
            }
        }
    }

    /** "AB12" -> 27 (zero-based). Empty/invalid -> -1. */
    private fun columnIndex(ref: String?): Int {
        if (ref.isNullOrEmpty()) return -1
        var col = 0
        var seen = false
        for (c in ref) {
            if (c in 'A'..'Z') {
                col = col * 26 + (c - 'A' + 1)
                seen = true
            } else if (c in 'a'..'z') {
                col = col * 26 + (c - 'a' + 1)
                seen = true
            } else {
                break
            }
        }
        return if (seen) col - 1 else -1
    }

    private fun setAt(list: MutableList<String>, index: Int, value: String) {
        while (list.size <= index) list.add("")
        list[index] = value
    }

    private fun newParser(bytes: ByteArray): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        return parser
    }
}

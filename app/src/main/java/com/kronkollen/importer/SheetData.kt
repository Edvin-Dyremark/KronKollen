package com.kronkollen.importer

/**
 * The raw contents of the first worksheet: a list of rows, each a list of string cells.
 * Date-formatted cells are normalised to ISO yyyy-MM-dd by [XlsxReader].
 */
data class SheetData(
    val rows: List<List<String>>,
) {
    /** Number of columns = widest row. */
    val columnCount: Int = rows.maxOfOrNull { it.size } ?: 0

    fun cell(row: Int, col: Int): String = rows.getOrNull(row)?.getOrNull(col).orEmpty()
}

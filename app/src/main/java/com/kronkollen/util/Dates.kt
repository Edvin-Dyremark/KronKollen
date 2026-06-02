package com.kronkollen.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Date display + flexible parsing for bank exports. Display uses ISO yyyy-MM-dd (Swedish). */
object Dates {

    val SWEDISH: Locale = Locale.forLanguageTag("sv-SE")

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Excel's epoch (with the well-known 1900 leap-year bug, day 60 == 1900-02-29). */
    private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)

    private val explicitFormats: Map<DateFormatOption, DateTimeFormatter> = mapOf(
        DateFormatOption.ISO_YMD to ofPattern("yyyy-MM-dd"),
        DateFormatOption.DMY_DOT to ofPattern("dd.MM.yyyy"),
        DateFormatOption.DMY_SLASH to ofPattern("dd/MM/yyyy"),
        DateFormatOption.MDY_SLASH to ofPattern("MM/dd/yyyy"),
    )

    private fun ofPattern(p: String) = DateTimeFormatter.ofPattern(p, SWEDISH)

    fun displayIso(date: LocalDate): String = date.format(isoFormatter)

    /** e.g. "maj 2026" — used for axis/section labels. */
    fun monthYear(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.FULL, SWEDISH)
        return "$month ${date.year}"
    }

    /** Convert an Excel serial day number to a date. */
    fun fromExcelSerial(serial: Double): LocalDate = EXCEL_EPOCH.plusDays(serial.toLong())

    /**
     * Parse a raw cell with the chosen [option]. With [DateFormatOption.AUTO] it tries
     * ISO, the common dotted/slashed European forms, then an Excel serial number.
     */
    fun parse(raw: String, option: DateFormatOption): LocalDate? {
        val s = raw.trim()
        if (s.isEmpty()) return null

        if (option != DateFormatOption.AUTO && option != DateFormatOption.EXCEL_SERIAL) {
            return tryFormat(s, explicitFormats.getValue(option))
        }
        if (option == DateFormatOption.EXCEL_SERIAL) {
            return s.toDoubleOrNull()?.let { fromExcelSerial(it) }
        }

        // AUTO
        // The XlsxReader already normalises style-detected dates to ISO, so try that first.
        tryFormat(s, isoFormatter)?.let { return it }
        for (fmt in explicitFormats.values) {
            tryFormat(s, fmt)?.let { return it }
        }
        // Bare serial number fallback (only plausible 5-digit-ish values).
        s.toDoubleOrNull()?.let { d ->
            if (d > 20000 && d < 80000) return fromExcelSerial(d)
        }
        return null
    }

    private fun tryFormat(s: String, fmt: DateTimeFormatter): LocalDate? = try {
        LocalDate.parse(s, fmt)
    } catch (e: Exception) {
        null
    }
}

enum class DateFormatOption(val label: String) {
    AUTO("Auto-detect"),
    ISO_YMD("yyyy-MM-dd"),
    DMY_DOT("dd.MM.yyyy"),
    DMY_SLASH("dd/MM/yyyy"),
    MDY_SLASH("MM/dd/yyyy"),
    EXCEL_SERIAL("Excel serial number"),
}

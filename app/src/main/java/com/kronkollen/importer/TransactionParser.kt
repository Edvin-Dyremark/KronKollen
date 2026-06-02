package com.kronkollen.importer

import com.kronkollen.data.repo.ParsedTransaction
import com.kronkollen.util.Dates
import com.kronkollen.util.Money

/**
 * Turns the raw [SheetData] into [ParsedTransaction]s according to the user's
 * [ColumnMapping]. Rows without a parseable date or amount are skipped and counted.
 * Category assignment happens later (TransactionRepository.categorize).
 */
object TransactionParser {

    data class Output(
        val parsed: List<ParsedTransaction>,
        val skippedUnparseable: Int,
    )

    fun parse(sheet: SheetData, mapping: ColumnMapping): Output {
        val parsed = mutableListOf<ParsedTransaction>()
        var skipped = 0
        val firstRow = if (mapping.hasHeaderRow) 1 else 0

        for (r in firstRow until sheet.rows.size) {
            val rawDate = sheet.cell(r, mapping.dateColumn)
            val description = sheet.cell(r, mapping.descriptionColumn).trim()
            val date = Dates.parse(rawDate, mapping.dateFormat)

            val amount = when (mapping.amountMode) {
                AmountMode.SINGLE -> {
                    Money.parseToOre(sheet.cell(r, mapping.amountColumn))
                        ?.let { if (mapping.flipSign) -it else it }
                }
                AmountMode.SPLIT -> {
                    val out = Money.parseToOre(sheet.cell(r, mapping.outColumn))
                    val inn = Money.parseToOre(sheet.cell(r, mapping.inColumn))
                    if (out == null && inn == null) null
                    else (inn ?: 0L) - kotlin.math.abs(out ?: 0L)
                }
            }

            // Skip fully empty rows silently; skip otherwise-non-empty rows that fail.
            val isBlank = rawDate.isBlank() && description.isBlank() &&
                sheet.cell(r, mapping.amountColumn).isBlank()
            if (date == null || amount == null) {
                if (!isBlank) skipped++
                continue
            }
            parsed.add(
                ParsedTransaction(
                    date = date,
                    description = description,
                    amount = amount,
                    categoryId = null,
                ),
            )
        }
        return Output(parsed, skipped)
    }
}

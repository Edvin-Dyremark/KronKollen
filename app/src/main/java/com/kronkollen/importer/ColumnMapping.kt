package com.kronkollen.importer

import com.kronkollen.util.DateFormatOption

/** How the amount is represented in the sheet. */
enum class AmountMode { SINGLE, SPLIT }

/**
 * User's choice of how the columns of an xlsx map onto a transaction. Persisted so the
 * next import pre-fills the same selection.
 */
data class ColumnMapping(
    val hasHeaderRow: Boolean = true,
    val dateColumn: Int = 0,
    val descriptionColumn: Int = 1,
    val amountMode: AmountMode = AmountMode.SINGLE,
    /** SINGLE mode: the one signed amount column. */
    val amountColumn: Int = 2,
    /** SINGLE mode: multiply parsed amount by -1 (when expenses are stored positive). */
    val flipSign: Boolean = false,
    /** SPLIT mode: column holding expense (money-out) magnitudes. */
    val outColumn: Int = 2,
    /** SPLIT mode: column holding income (money-in) magnitudes. */
    val inColumn: Int = 3,
    val dateFormat: DateFormatOption = DateFormatOption.AUTO,
)

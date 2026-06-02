package com.kronkollen.data.db

import java.time.LocalDate

/** SUM(amount) per category over a period (categoryId null = Uncategorized). */
data class CategoryAmount(
    val categoryId: Long?,
    val total: Long,
)

/** A single (date, amount) pair, used to build the monthly trend in memory. */
data class DateAmount(
    val date: LocalDate,
    val amount: Long,
)

/** Natural key used by the import duplicate guard. */
data class DupKey(
    val date: LocalDate,
    val amount: Long,
    val description: String,
)

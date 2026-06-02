package com.kronkollen.data.repo

import com.kronkollen.categorize.KeywordMatcher
import com.kronkollen.categorize.Rule
import com.kronkollen.data.db.CategoryAmount
import com.kronkollen.data.db.DateAmount
import com.kronkollen.data.db.KeywordRuleDao
import com.kronkollen.data.db.TransactionDao
import com.kronkollen.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Outcome of an import, surfaced to the UI for the confirmation message. */
data class ImportResult(
    val inserted: Int,
    val skippedDuplicates: Int,
    val autoCategorized: Int,
    val latestDate: LocalDate?,
)

/** A transaction parsed from a file but not yet written (used for preview + insert). */
data class ParsedTransaction(
    val date: LocalDate,
    val description: String,
    val amount: Long,
    val categoryId: Long?,
)

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val keywordRuleDao: KeywordRuleDao,
) {
    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeFiltered(
        start: LocalDate?,
        end: LocalDate?,
        categoryId: Long?,
        onlyUncategorized: Boolean,
        query: String,
    ): Flow<List<TransactionEntity>> =
        transactionDao.observeFiltered(start, end, categoryId, onlyUncategorized, query)

    fun observeLatestDate(): Flow<LocalDate?> = transactionDao.observeLatestDate()
    fun observeEarliestDate(): Flow<LocalDate?> = transactionDao.observeEarliestDate()
    fun observeCount(): Flow<Int> = transactionDao.observeCount()

    fun observeCategoryTotals(start: LocalDate, end: LocalDate): Flow<List<CategoryAmount>> =
        transactionDao.observeCategoryTotals(start, end)

    fun observeExpenseRows(start: LocalDate, end: LocalDate): Flow<List<DateAmount>> =
        transactionDao.observeExpenseRows(start, end)

    suspend fun setCategory(transactionId: Long, categoryId: Long?) =
        transactionDao.updateCategory(transactionId, categoryId)

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    private suspend fun currentRules(): List<Rule> =
        keywordRuleDao.getAll().map { Rule(it.keyword, it.categoryId) }

    /** Auto-categorize a batch of parsed rows against the current keyword rules. */
    suspend fun categorize(rows: List<ParsedTransaction>): List<ParsedTransaction> {
        val rules = currentRules()
        return rows.map { row ->
            if (row.categoryId != null) row
            else row.copy(categoryId = KeywordMatcher.match(row.description, rules))
        }
    }

    /**
     * Persist parsed rows, skipping any that duplicate an existing (date, amount,
     * description) triple.
     */
    suspend fun commitImport(rows: List<ParsedTransaction>): ImportResult {
        val existing = transactionDao.getDuplicateKeys()
            .map { Triple(it.date, it.amount, it.description) }
            .toHashSet()

        val now = System.currentTimeMillis()
        val toInsert = mutableListOf<TransactionEntity>()
        var skipped = 0
        var autoCat = 0
        // Track keys within this batch too, so a file with internal duplicates is handled.
        val seen = HashSet<Triple<LocalDate, Long, String>>()

        for (row in rows) {
            val key = Triple(row.date, row.amount, row.description)
            if (key in existing || key in seen) {
                skipped++
                continue
            }
            seen.add(key)
            if (row.categoryId != null) autoCat++
            toInsert.add(
                TransactionEntity(
                    date = row.date,
                    description = row.description,
                    amount = row.amount,
                    categoryId = row.categoryId,
                    rawDescription = row.description,
                    importedAt = now,
                ),
            )
        }

        if (toInsert.isNotEmpty()) transactionDao.insertAll(toInsert)
        val latest = toInsert.maxOfOrNull { it.date }
        return ImportResult(
            inserted = toInsert.size,
            skippedDuplicates = skipped,
            autoCategorized = autoCat,
            latestDate = latest,
        )
    }

    /**
     * Apply a freshly created keyword rule to existing **uncategorized** transactions.
     * Returns the number of rows re-categorized.
     */
    suspend fun applyKeywordToUncategorized(keyword: String, categoryId: Long): Int {
        val rule = listOf(Rule(keyword.lowercase(), categoryId))
        val uncategorized = transactionDao.getUncategorized()
        var count = 0
        for (tx in uncategorized) {
            if (KeywordMatcher.match(tx.description, rule) != null) {
                transactionDao.updateCategory(tx.id, categoryId)
                count++
            }
        }
        return count
    }
}

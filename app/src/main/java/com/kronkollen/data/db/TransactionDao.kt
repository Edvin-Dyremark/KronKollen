package com.kronkollen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kronkollen.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /**
     * Filtered, reverse-chronological list for the Transactions screen.
     * - [categoryId] null + [onlyUncategorized] false => any category
     * - [onlyUncategorized] true => only rows with no category
     * - [query] empty => no text filter
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:start IS NULL OR date >= :start)
          AND (:end IS NULL OR date <= :end)
          AND (:onlyUncategorized = 0 OR categoryId IS NULL)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:query = '' OR description LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeFiltered(
        start: LocalDate?,
        end: LocalDate?,
        categoryId: Long?,
        onlyUncategorized: Boolean,
        query: String,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT MAX(date) FROM transactions")
    fun observeLatestDate(): Flow<LocalDate?>

    @Query("SELECT MIN(date) FROM transactions")
    fun observeEarliestDate(): Flow<LocalDate?>

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    /** Per-category expense totals (amount < 0) within an inclusive date range. */
    @Query(
        """
        SELECT categoryId AS categoryId, SUM(amount) AS total
        FROM transactions
        WHERE date BETWEEN :start AND :end AND amount < 0
        GROUP BY categoryId
        """,
    )
    fun observeCategoryTotals(start: LocalDate, end: LocalDate): Flow<List<CategoryAmount>>

    /** Raw expense rows (amount < 0) in range, for building the monthly trend. */
    @Query(
        "SELECT date AS date, amount AS amount FROM transactions " +
            "WHERE date BETWEEN :start AND :end AND amount < 0",
    )
    fun observeExpenseRows(start: LocalDate, end: LocalDate): Flow<List<DateAmount>>

    @Query("SELECT * FROM transactions WHERE categoryId IS NULL")
    suspend fun getUncategorized(): List<TransactionEntity>

    @Query("SELECT date AS date, amount AS amount, description AS description FROM transactions")
    suspend fun getDuplicateKeys(): List<DupKey>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateCategory(id: Long, categoryId: Long?)

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

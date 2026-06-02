package com.kronkollen.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("date"),
        Index("categoryId"),
        // Used by the duplicate guard on import.
        Index(value = ["date", "amount", "description"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val description: String,
    /** Amount in öre. Negative = expense (money out), positive = income. */
    val amount: Long,
    /** Null means "Uncategorized". */
    val categoryId: Long? = null,
    /** Original description text as read from the file, kept for re-matching. */
    val rawDescription: String,
    /** Epoch millis of the import that created this row. */
    val importedAt: Long,
)

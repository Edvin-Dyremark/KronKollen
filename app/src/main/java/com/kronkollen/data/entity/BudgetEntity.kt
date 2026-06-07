package com.kronkollen.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring monthly spending limit for a single category. One row per category — the
 * [categoryId] is the primary key — so the same limit applies every month.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["categoryId"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey val categoryId: Long,
    /** Monthly limit in öre (positive). */
    val amountOre: Long,
)

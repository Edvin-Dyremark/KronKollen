package com.kronkollen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kronkollen.data.entity.BudgetEntity
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.KeywordRuleEntity
import com.kronkollen.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        KeywordRuleEntity::class,
        BudgetEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        // Reset previously-seeded category colours to 0 ("no override") so they pick up the
        // current palette by sort index. Transactions and keyword rules are untouched.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE categories SET colorArgb = 0")
            }
        }

        // Add the budgets table (one recurring monthly limit per category). Column and index
        // names match what Room generates so the schema validates against the entity.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        categoryId INTEGER NOT NULL,
                        amountOre INTEGER NOT NULL,
                        PRIMARY KEY(categoryId),
                        FOREIGN KEY(categoryId) REFERENCES categories(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId " +
                        "ON budgets(categoryId)",
                )
            }
        }
    }
}

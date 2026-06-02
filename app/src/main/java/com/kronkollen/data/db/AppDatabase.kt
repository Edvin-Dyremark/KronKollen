package com.kronkollen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.KeywordRuleEntity
import com.kronkollen.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        KeywordRuleEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun keywordRuleDao(): KeywordRuleDao

    companion object {
        // Reset previously-seeded category colours to 0 ("no override") so they pick up the
        // current palette by sort index. Transactions and keyword rules are untouched.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE categories SET colorArgb = 0")
            }
        }
    }
}

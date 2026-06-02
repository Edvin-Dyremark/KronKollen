package com.kronkollen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.KeywordRuleEntity
import com.kronkollen.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        KeywordRuleEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun keywordRuleDao(): KeywordRuleDao
}

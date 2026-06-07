package com.kronkollen.di

import android.content.Context
import androidx.room.Room
import com.kronkollen.data.BackupManager
import com.kronkollen.data.SampleDataSeeder
import com.kronkollen.data.db.AppDatabase
import com.kronkollen.data.prefs.SettingsDataStore
import com.kronkollen.data.repo.BudgetRepository
import com.kronkollen.data.repo.CategoryRepository
import com.kronkollen.data.repo.TransactionRepository

/**
 * Lightweight manual dependency container. Built once in [com.kronkollen.KronKollenApp]
 * and reached from ViewModels via the Application instance — keeps the app free of an
 * annotation-processing DI framework.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "kronkollen.db",
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()

    val categoryRepository = CategoryRepository(
        categoryDao = database.categoryDao(),
        keywordRuleDao = database.keywordRuleDao(),
    )

    val transactionRepository = TransactionRepository(
        transactionDao = database.transactionDao(),
        keywordRuleDao = database.keywordRuleDao(),
    )

    val budgetRepository = BudgetRepository(
        budgetDao = database.budgetDao(),
    )

    val settings = SettingsDataStore(context.applicationContext)

    val backupManager = BackupManager(
        categoryDao = database.categoryDao(),
        keywordRuleDao = database.keywordRuleDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
    )

    val sampleDataSeeder = SampleDataSeeder(
        categories = categoryRepository,
        transactions = transactionRepository,
        settings = settings,
    )
}

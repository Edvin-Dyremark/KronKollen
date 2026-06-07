package com.kronkollen.data

import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.TransactionEntity
import com.kronkollen.data.prefs.SettingsDataStore
import com.kronkollen.data.repo.CategoryRepository
import com.kronkollen.data.repo.TransactionRepository
import java.time.LocalDate

/**
 * Seeds demo categories, keyword rules and ~6 months of transactions on first launch so
 * the app looks alive before the user imports anything. The demo transactions are wiped
 * the first time a real import is committed (see ImportViewModel + the sampleDataPresent
 * flag).
 */
class SampleDataSeeder(
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val settings: SettingsDataStore,
) {
    suspend fun seedIfFirstRun() {
        categories.seedDefaultsIfEmpty()
        if (transactions.count() > 0) return

        val byName = categories.getCategories().associateBy { it.name }

        // Seed the canonical default keyword rules so auto-categorization works out of the box.
        DefaultKeywords.rules.forEach { (kw, cat) ->
            byName[cat]?.let { categories.addKeyword(kw, it.id) }
        }

        transactions.insertEntities(buildSampleTransactions(byName))
        settings.setSampleDataPresent(true)
    }

    private fun buildSampleTransactions(byName: Map<String, CategoryEntity>): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val rows = mutableListOf<TransactionEntity>()

        fun id(name: String): Long? = byName[name]?.id
        fun add(date: LocalDate, desc: String, kr: Double, category: String?) {
            rows += TransactionEntity(
                date = date,
                description = desc,
                amount = Math.round(kr * 100),
                categoryId = category?.let { id(it) },
                rawDescription = desc,
                importedAt = now,
            )
        }

        for (m in 0L..5L) {
            val base = today.minusMonths(m)
            fun day(d: Int) = base.withDayOfMonth(minOf(d, base.lengthOfMonth()))
            val jitter = (m % 3) * 17.0 // small per-month variation

            add(day(25), "Lön", 32000.0, "Inkomst")
            add(day(1), "Hyra Lägenhet", -8500.0, "Boende")
            add(day(3), "Vattenfall El", -445.50 - jitter, "Abonnemang")
            add(day(4), "Telia Mobil", -299.0, "Abonnemang")
            add(day(2), "SL Access månadskort", -930.0, "Transport")
            add(day(5), "ICA Maxi Stormarknad", -842.30 - jitter, "Mat")
            add(day(11), "Coop Konsum", -534.10 + jitter, "Mat")
            add(day(19), "ICA Nära Sabbatsberg", -276.80, "Mat")
            add(day(23), "Hemköp", -312.40 + jitter, "Mat")
            add(day(8), "Spotify", -119.0, "Abonnemang")
            add(day(15), "Netflix", -139.0, "Abonnemang")
            add(day(20), "Restaurang Pong", -468.0 - jitter, "Restaurang")
            add(day(6), "SATS Träning", -799.0, "Sport")
            add(day(17), "Apotek Hjärtat", -189.50, "Hälsa")
            add(day(22), "H&M", -399.0 + jitter, "Shopping")
            add(day(26), "Överföring Sparkonto", -2000.0, "Sparande")
            if (m % 2 == 0L) add(day(14), "SJ Biljett Stockholm-Göteborg", -645.0, "Transport")
            if (m % 2 == 1L) add(day(9), "Clas Ohlson", -248.0, "Shopping")
            add(day(10), "Swish Anna", -250.0, null)
        }
        return rows
    }
}

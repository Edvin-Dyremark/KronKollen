package com.kronkollen.data

import com.kronkollen.data.db.BudgetDao
import com.kronkollen.data.db.CategoryDao
import com.kronkollen.data.db.KeywordRuleDao
import com.kronkollen.data.db.TransactionDao
import com.kronkollen.data.entity.BudgetEntity
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.KeywordRuleEntity
import com.kronkollen.data.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class RestoreResult(
    val categories: Int,
    val keywordRules: Int,
    val transactions: Int,
    val budgets: Int,
)

/**
 * Full backup/restore of all app data to/from a single JSON document. Uses the built-in
 * org.json so there's no serialization dependency. Original row ids are preserved so the
 * category references in keyword rules and transactions stay intact.
 */
class BackupManager(
    private val categoryDao: CategoryDao,
    private val keywordRuleDao: KeywordRuleDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
) {
    companion object {
        // v2 added the "budgets" array. Restore stays backward-compatible with v1 files.
        private const val FORMAT_VERSION = 2
    }

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("format", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val cats = JSONArray()
        for (c in categoryDao.getAll()) {
            cats.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("colorArgb", c.colorArgb)
                    .put("sortOrder", c.sortOrder),
            )
        }
        root.put("categories", cats)

        val rules = JSONArray()
        for (r in keywordRuleDao.getAll()) {
            rules.put(
                JSONObject()
                    .put("id", r.id)
                    .put("keyword", r.keyword)
                    .put("categoryId", r.categoryId),
            )
        }
        root.put("keywordRules", rules)

        val txs = JSONArray()
        for (t in transactionDao.getAll()) {
            txs.put(
                JSONObject()
                    .put("id", t.id)
                    .put("date", t.date.toEpochDay())
                    .put("description", t.description)
                    .put("amount", t.amount)
                    .put("categoryId", t.categoryId ?: JSONObject.NULL)
                    .put("rawDescription", t.rawDescription)
                    .put("importedAt", t.importedAt),
            )
        }
        root.put("transactions", txs)

        val budgets = JSONArray()
        for (b in budgetDao.getAll()) {
            budgets.put(
                JSONObject()
                    .put("categoryId", b.categoryId)
                    .put("amountOre", b.amountOre),
            )
        }
        root.put("budgets", budgets)

        return root.toString(2)
    }

    /** Replaces ALL current data with the backup's contents. */
    suspend fun restoreJson(json: String): RestoreResult {
        val root = JSONObject(json)

        val categories = root.getJSONArray("categories").map { o ->
            CategoryEntity(
                id = o.getLong("id"),
                name = o.getString("name"),
                colorArgb = o.optInt("colorArgb", 0),
                sortOrder = o.optInt("sortOrder", 0),
            )
        }
        val rules = root.getJSONArray("keywordRules").map { o ->
            KeywordRuleEntity(
                id = o.getLong("id"),
                keyword = o.getString("keyword"),
                categoryId = o.getLong("categoryId"),
            )
        }
        val transactions = root.getJSONArray("transactions").map { o ->
            TransactionEntity(
                id = o.getLong("id"),
                date = LocalDate.ofEpochDay(o.getLong("date")),
                description = o.getString("description"),
                amount = o.getLong("amount"),
                categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                rawDescription = o.optString("rawDescription", o.getString("description")),
                importedAt = o.optLong("importedAt", System.currentTimeMillis()),
            )
        }
        // Optional: v1 backups have no "budgets" array.
        val budgets = (root.optJSONArray("budgets") ?: JSONArray()).map { o ->
            BudgetEntity(
                categoryId = o.getLong("categoryId"),
                amountOre = o.getLong("amountOre"),
            )
        }

        // Wipe in FK-safe order, then insert parents before children.
        budgetDao.deleteAll()
        keywordRuleDao.deleteAll()
        transactionDao.deleteAll()
        categoryDao.deleteAll()

        categories.forEach { categoryDao.insert(it) }
        rules.forEach { keywordRuleDao.insert(it) }
        if (transactions.isNotEmpty()) transactionDao.insertAll(transactions)
        budgets.forEach { budgetDao.upsert(it) }

        return RestoreResult(categories.size, rules.size, transactions.size, budgets.size)
    }

    private inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
}

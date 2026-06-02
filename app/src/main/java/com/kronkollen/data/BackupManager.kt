package com.kronkollen.data

import com.kronkollen.data.db.CategoryDao
import com.kronkollen.data.db.KeywordRuleDao
import com.kronkollen.data.db.TransactionDao
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
) {
    companion object {
        private const val FORMAT_VERSION = 1
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

        // Wipe in FK-safe order, then insert parents before children.
        keywordRuleDao.deleteAll()
        transactionDao.deleteAll()
        categoryDao.deleteAll()

        categories.forEach { categoryDao.insert(it) }
        rules.forEach { keywordRuleDao.insert(it) }
        if (transactions.isNotEmpty()) transactionDao.insertAll(transactions)

        return RestoreResult(categories.size, rules.size, transactions.size)
    }

    private inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
}

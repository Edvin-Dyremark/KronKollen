package com.kronkollen.data.repo

import com.kronkollen.data.db.CategoryDao
import com.kronkollen.data.db.KeywordRuleDao
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.KeywordRuleEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val keywordRuleDao: KeywordRuleDao,
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeKeywordRules(): Flow<List<KeywordRuleEntity>> = keywordRuleDao.observeAll()

    suspend fun getCategories(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun getKeywordRules(): List<KeywordRuleEntity> = keywordRuleDao.getAll()

    suspend fun addCategory(name: String): Long {
        val existing = categoryDao.count()
        // colorArgb = 0 => no manual override; colour comes from the palette by sort index.
        return categoryDao.insert(
            CategoryEntity(name = name.trim(), colorArgb = 0, sortOrder = existing),
        )
    }

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)

    /** Add a keyword rule, normalising the keyword to lower case. Ignores duplicates. */
    suspend fun addKeyword(keyword: String, categoryId: Long): Long {
        val normalized = keyword.trim().lowercase()
        if (normalized.isEmpty()) return -1
        return keywordRuleDao.insert(
            KeywordRuleEntity(keyword = normalized, categoryId = categoryId),
        )
    }

    suspend fun removeKeyword(rule: KeywordRuleEntity) = keywordRuleDao.delete(rule)

    /** Seed a starter set of categories the first time the app runs. */
    suspend fun seedDefaultsIfEmpty() {
        if (categoryDao.count() > 0) return
        listOf(
            "Mat", "Transport", "Boende", "Nöje", "Hälsa", "Shopping", "Räkningar",
        ).forEach { addCategory(it) }
    }
}

package com.kronkollen.data.repo

import com.kronkollen.data.db.BudgetDao
import com.kronkollen.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val budgetDao: BudgetDao,
) {
    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    /** Set (or clear) a category's monthly limit. A non-positive amount removes the budget. */
    suspend fun setBudget(categoryId: Long, amountOre: Long) {
        if (amountOre <= 0) {
            budgetDao.deleteByCategory(categoryId)
        } else {
            budgetDao.upsert(BudgetEntity(categoryId = categoryId, amountOre = amountOre))
        }
    }

    suspend fun removeBudget(categoryId: Long) = budgetDao.deleteByCategory(categoryId)
}

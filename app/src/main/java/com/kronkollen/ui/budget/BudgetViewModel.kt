package com.kronkollen.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kronkollen.KronKollenApp
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.repo.BudgetRepository
import com.kronkollen.data.repo.CategoryRepository
import com.kronkollen.data.repo.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import kotlin.math.abs

/** One category's budget vs. actual spend for the selected month. */
data class BudgetRow(
    val category: CategoryEntity,
    val budgetOre: Long,
    val spentOre: Long,
    /** spent / budget, clamped to [0,1] for the bar; check [overBudget] for the real state. */
    val fraction: Float,
    val overBudget: Boolean,
)

data class BudgetUiState(
    val month: YearMonth = YearMonth.now(),
    /** Categories that have a budget, sorted by how close they are to (or over) the limit. */
    val rows: List<BudgetRow> = emptyList(),
    /** Categories without a budget — offered in the "add budget" section. */
    val unbudgeted: List<CategoryEntity> = emptyList(),
    val totalBudget: Long = 0,
    val totalSpent: Long = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val budgets: BudgetRepository,
    private val categoriesRepo: CategoryRepository,
    private val transactions: TransactionRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<BudgetUiState> =
        month.flatMapLatest { ym ->
            combine(
                budgets.observeBudgets(),
                categoriesRepo.observeCategories(),
                transactions.observeCategoryTotals(ym.atDay(1), ym.atEndOfMonth()),
            ) { budgetList, cats, totals ->
                // Spend per category id (totals are negative expenses; take magnitude).
                val spentById = totals
                    .filter { it.categoryId != null }
                    .associate { it.categoryId!! to abs(it.total) }
                val budgetById = budgetList.associate { it.categoryId to it.amountOre }

                val rows = cats
                    .filter { budgetById.containsKey(it.id) }
                    .map { cat ->
                        val limit = budgetById.getValue(cat.id)
                        val spent = spentById[cat.id] ?: 0L
                        BudgetRow(
                            category = cat,
                            budgetOre = limit,
                            spentOre = spent,
                            fraction = if (limit == 0L) 0f else (spent.toFloat() / limit),
                            overBudget = spent > limit,
                        )
                    }
                    .sortedByDescending { it.fraction }

                BudgetUiState(
                    month = ym,
                    rows = rows,
                    unbudgeted = cats.filter { !budgetById.containsKey(it.id) },
                    totalBudget = rows.sumOf { it.budgetOre },
                    totalSpent = rows.sumOf { it.spentOre },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun prevMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun setBudget(categoryId: Long, amountOre: Long) {
        viewModelScope.launch { budgets.setBudget(categoryId, amountOre) }
    }

    fun removeBudget(categoryId: Long) {
        viewModelScope.launch { budgets.removeBudget(categoryId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KronKollenApp
                BudgetViewModel(
                    app.container.budgetRepository,
                    app.container.categoryRepository,
                    app.container.transactionRepository,
                )
            }
        }
    }
}

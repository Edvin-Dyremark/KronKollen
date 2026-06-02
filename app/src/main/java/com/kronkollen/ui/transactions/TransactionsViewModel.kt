package com.kronkollen.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kronkollen.KronKollenApp
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.TransactionEntity
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
import java.time.LocalDate

/** A transaction joined with its (optional) category, ready to render. */
data class UiTransaction(
    val id: Long,
    val date: LocalDate,
    val description: String,
    val amount: Long,
    val category: CategoryEntity?,
)

data class TransactionFilter(
    val categoryId: Long? = null,
    val onlyUncategorized: Boolean = false,
    val query: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactions: TransactionRepository,
    private val categoriesRepo: CategoryRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())
    val filterState: StateFlow<TransactionFilter> = filter

    val categories: StateFlow<List<CategoryEntity>> =
        categoriesRepo.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestDate: StateFlow<LocalDate?> =
        transactions.observeLatestDate()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<UiTransaction>> =
        filter.flatMapLatest { f ->
            combine(
                transactions.observeFiltered(
                    start = null,
                    end = null,
                    categoryId = f.categoryId,
                    onlyUncategorized = f.onlyUncategorized,
                    query = f.query.trim(),
                ),
                categoriesRepo.observeCategories(),
            ) { txs, cats ->
                val byId = cats.associateBy { it.id }
                txs.map { tx ->
                    UiTransaction(
                        id = tx.id,
                        date = tx.date,
                        description = tx.description,
                        amount = tx.amount,
                        category = tx.categoryId?.let { byId[it] },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) {
        filter.value = filter.value.copy(query = query)
    }

    fun selectCategoryFilter(categoryId: Long?, onlyUncategorized: Boolean) {
        filter.value = filter.value.copy(categoryId = categoryId, onlyUncategorized = onlyUncategorized)
    }

    fun setCategory(transactionId: Long, categoryId: Long?) {
        viewModelScope.launch { transactions.setCategory(transactionId, categoryId) }
    }

    /** Create a keyword rule from a manual edit and re-apply it to other uncategorized rows. */
    fun createKeywordRule(keyword: String, categoryId: Long, onApplied: (Int) -> Unit = {}) {
        viewModelScope.launch {
            categoriesRepo.addKeyword(keyword, categoryId)
            val applied = transactions.applyKeywordToUncategorized(keyword, categoryId)
            onApplied(applied)
        }
    }

    fun delete(transactionId: Long) {
        viewModelScope.launch { transactions.deleteTransaction(transactionId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KronKollenApp
                TransactionsViewModel(
                    app.container.transactionRepository,
                    app.container.categoryRepository,
                )
            }
        }
    }
}

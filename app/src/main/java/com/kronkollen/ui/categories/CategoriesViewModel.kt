package com.kronkollen.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kronkollen.KronKollenApp
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.entity.KeywordRuleEntity
import com.kronkollen.data.repo.CategoryRepository
import com.kronkollen.data.repo.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CategoryWithKeywords(
    val category: CategoryEntity,
    val keywords: List<KeywordRuleEntity>,
)

class CategoriesViewModel(
    private val repository: CategoryRepository,
    transactions: TransactionRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryWithKeywords>> =
        combine(
            repository.observeCategories(),
            repository.observeKeywordRules(),
        ) { cats, rules ->
            cats.map { c -> CategoryWithKeywords(c, rules.filter { it.categoryId == c.id }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestDate: StateFlow<LocalDate?> =
        transactions.observeLatestDate()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addCategory(name) }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.updateCategory(category.copy(name = newName.trim())) }
    }

    fun setCategoryColor(category: CategoryEntity, colorArgb: Int) {
        viewModelScope.launch { repository.updateCategory(category.copy(colorArgb = colorArgb)) }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun addKeyword(categoryId: Long, keyword: String) {
        viewModelScope.launch { repository.addKeyword(keyword, categoryId) }
    }

    fun removeKeyword(rule: KeywordRuleEntity) {
        viewModelScope.launch { repository.removeKeyword(rule) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KronKollenApp
                CategoriesViewModel(
                    app.container.categoryRepository,
                    app.container.transactionRepository,
                )
            }
        }
    }
}

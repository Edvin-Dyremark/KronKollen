package com.kronkollen.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kronkollen.KronKollenApp
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.data.repo.CategoryRepository
import com.kronkollen.data.repo.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.abs

enum class RangePreset(val label: String) {
    THIS_YEAR("I år"),
    THIS_MONTH("Denna månad"),
    LAST_12("Senaste 12 mån"),
    CUSTOM("Anpassat"),
}

data class DateRange(val start: LocalDate, val end: LocalDate)

/** One category's share of spending in the selected range (amount is a positive magnitude). */
data class CategorySlice(
    val category: CategoryEntity?,
    val amount: Long,
    val fraction: Float,
)

data class MonthBar(val label: String, val amount: Long)

data class OverviewUiState(
    val preset: RangePreset = RangePreset.THIS_YEAR,
    val range: DateRange = DateRange(LocalDate.now().withDayOfYear(1), LocalDate.now()),
    val totalSpent: Long = 0,
    val slices: List<CategorySlice> = emptyList(),
    val months: List<MonthBar> = emptyList(),
    val latestDate: LocalDate? = null,
    val transactionCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModel(
    private val transactions: TransactionRepository,
    private val categoriesRepo: CategoryRepository,
) : ViewModel() {

    private val preset = MutableStateFlow(RangePreset.THIS_YEAR)
    private val custom = MutableStateFlow<DateRange?>(null)

    val uiState: StateFlow<OverviewUiState> =
        combine(preset, custom) { p, c -> p to resolveRange(p, c) }
            .flatMapLatest { (p, range) ->
                combine(
                    transactions.observeCategoryTotals(range.start, range.end),
                    categoriesRepo.observeCategories(),
                    transactions.observeExpenseRows(range.start, range.end),
                    transactions.observeLatestDate(),
                    transactions.observeCount(),
                ) { totals, cats, rows, latest, count ->
                    val byId = cats.associateBy { it.id }
                    val magnitudes = totals
                        .map { it.categoryId to abs(it.total) }
                        .filter { it.second > 0 }
                    val totalSpent = magnitudes.sumOf { it.second }
                    val slices = magnitudes
                        .sortedByDescending { it.second }
                        .map { (catId, mag) ->
                            CategorySlice(
                                category = catId?.let { byId[it] },
                                amount = mag,
                                fraction = if (totalSpent == 0L) 0f else mag.toFloat() / totalSpent,
                            )
                        }
                    OverviewUiState(
                        preset = p,
                        range = range,
                        totalSpent = totalSpent,
                        slices = slices,
                        months = buildMonths(rows.map { it.date to abs(it.amount) }),
                        latestDate = latest,
                        transactionCount = count,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    fun selectPreset(p: RangePreset) {
        preset.value = p
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        custom.value = DateRange(start, end)
        preset.value = RangePreset.CUSTOM
    }

    private fun resolveRange(p: RangePreset, customRange: DateRange?): DateRange {
        val today = LocalDate.now()
        return when (p) {
            RangePreset.THIS_YEAR -> DateRange(today.withDayOfYear(1), today)
            RangePreset.THIS_MONTH ->
                DateRange(today.withDayOfMonth(1), today)
            RangePreset.LAST_12 ->
                DateRange(today.minusMonths(11).withDayOfMonth(1), today)
            RangePreset.CUSTOM -> customRange ?: DateRange(today.withDayOfYear(1), today)
        }
    }

    private fun buildMonths(rows: List<Pair<LocalDate, Long>>): List<MonthBar> {
        if (rows.isEmpty()) return emptyList()
        val byMonth = rows.groupBy { YearMonth.from(it.first) }
            .mapValues { entry -> entry.value.sumOf { it.second } }
        return byMonth.keys.sorted().map { ym ->
            val label = ym.month.getDisplayName(TextStyle.SHORT, com.kronkollen.util.Dates.SWEDISH)
                .replace(".", "")
                .replaceFirstChar { it.uppercase() }
            MonthBar(label = label, amount = byMonth.getValue(ym))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KronKollenApp
                OverviewViewModel(
                    app.container.transactionRepository,
                    app.container.categoryRepository,
                )
            }
        }
    }
}

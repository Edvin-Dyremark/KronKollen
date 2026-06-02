package com.kronkollen.ui.importflow

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kronkollen.KronKollenApp
import com.kronkollen.data.prefs.SettingsDataStore
import com.kronkollen.data.repo.CategoryRepository
import com.kronkollen.data.repo.ImportResult
import com.kronkollen.data.repo.ParsedTransaction
import com.kronkollen.data.repo.TransactionRepository
import com.kronkollen.importer.ColumnMapping
import com.kronkollen.importer.SheetData
import com.kronkollen.importer.TransactionParser
import com.kronkollen.importer.XlsxReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportStep { Idle, Loading, Mapping, Preview, Done, Error }

data class UiPreviewRow(
    val date: String,
    val description: String,
    val amount: Long,
    val categoryName: String,
    val categoryColorArgb: Int?,
)

data class ImportUiState(
    val step: ImportStep = ImportStep.Idle,
    val mapping: ColumnMapping = ColumnMapping(),
    val headers: List<String> = emptyList(),
    val columnCount: Int = 0,
    val sampleRows: List<List<String>> = emptyList(),
    val previewRows: List<UiPreviewRow> = emptyList(),
    val parsedCount: Int = 0,
    val skippedUnparseable: Int = 0,
    val result: ImportResult? = null,
    val error: String? = null,
)

class ImportViewModel(
    application: Application,
    private val transactions: TransactionRepository,
    private val categoriesRepo: CategoryRepository,
    private val settings: SettingsDataStore,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var sheet: SheetData? = null

    fun onFilePicked(uri: Uri) {
        _state.value = _state.value.copy(step = ImportStep.Loading, error = null)
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        XlsxReader.read(it)
                    } ?: throw IllegalStateException("Kunde inte öppna filen.")
                }
                if (data.rows.isEmpty()) {
                    _state.value = _state.value.copy(
                        step = ImportStep.Error,
                        error = "Filen verkar tom eller går inte att läsa.",
                    )
                    return@launch
                }
                sheet = data
                val savedMapping = settings.mapping.first()
                val headerRow = if (savedMapping.hasHeaderRow) data.rows.firstOrNull().orEmpty() else emptyList()
                _state.value = _state.value.copy(
                    step = ImportStep.Mapping,
                    mapping = clampMapping(savedMapping, data.columnCount),
                    headers = headerRow,
                    columnCount = data.columnCount,
                    sampleRows = data.rows.take(6),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    step = ImportStep.Error,
                    error = e.message ?: "Okänt fel vid inläsning.",
                )
            }
        }
    }

    fun updateMapping(mapping: ColumnMapping) {
        val data = sheet
        val headerRow = if (mapping.hasHeaderRow) data?.rows?.firstOrNull().orEmpty() else emptyList()
        _state.value = _state.value.copy(mapping = mapping, headers = headerRow)
    }

    fun buildPreview() {
        val data = sheet ?: return
        viewModelScope.launch {
            val mapping = _state.value.mapping
            val parsed = TransactionParser.parse(data, mapping)
            val categorized = transactions.categorize(parsed.parsed)
            val byId = categoriesRepo.getCategories().associateBy { it.id }
            val rows = categorized.take(50).map { p ->
                val cat = p.categoryId?.let { byId[it] }
                UiPreviewRow(
                    date = com.kronkollen.util.Dates.displayIso(p.date),
                    description = p.description,
                    amount = p.amount,
                    categoryName = cat?.name ?: "Okategoriserat",
                    categoryColorArgb = cat?.colorArgb,
                )
            }
            _state.value = _state.value.copy(
                step = ImportStep.Preview,
                previewRows = rows,
                parsedCount = categorized.size,
                skippedUnparseable = parsed.skippedUnparseable,
            )
            pendingCommit = categorized
        }
    }

    private var pendingCommit: List<ParsedTransaction> = emptyList()

    fun confirmImport() {
        if (sheet == null) return
        viewModelScope.launch {
            val result = transactions.commitImport(pendingCommit)
            settings.saveMapping(_state.value.mapping)
            _state.value = _state.value.copy(step = ImportStep.Done, result = result)
        }
    }

    fun backToMapping() {
        _state.value = _state.value.copy(step = ImportStep.Mapping)
    }

    fun reset() {
        sheet = null
        pendingCommit = emptyList()
        _state.value = ImportUiState()
    }

    private fun clampMapping(mapping: ColumnMapping, columnCount: Int): ColumnMapping {
        if (columnCount <= 0) return mapping
        val max = columnCount - 1
        fun c(i: Int) = i.coerceIn(0, max)
        return mapping.copy(
            dateColumn = c(mapping.dateColumn),
            descriptionColumn = c(mapping.descriptionColumn),
            amountColumn = c(mapping.amountColumn),
            outColumn = c(mapping.outColumn),
            inColumn = c(mapping.inColumn),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KronKollenApp
                ImportViewModel(
                    application = app,
                    transactions = app.container.transactionRepository,
                    categoriesRepo = app.container.categoryRepository,
                    settings = app.container.settings,
                )
            }
        }
    }
}

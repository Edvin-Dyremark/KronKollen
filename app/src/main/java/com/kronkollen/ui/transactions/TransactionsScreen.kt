package com.kronkollen.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.ui.categories.TextInputDialog
import com.kronkollen.ui.components.CategoryChip
import com.kronkollen.ui.components.UNCATEGORIZED_LABEL
import com.kronkollen.ui.components.UncategorizedColor
import com.kronkollen.ui.components.colorOf
import com.kronkollen.ui.components.nameOf
import com.kronkollen.util.Dates
import com.kronkollen.util.Money
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    initialCategoryId: Long? = null,
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filter by viewModel.filterState.collectAsStateWithLifecycle()
    val latestDate by viewModel.latestDate.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<UiTransaction?>(null) }
    var rulePrompt by remember { mutableStateOf<RulePrompt?>(null) }

    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null) viewModel.selectCategoryFilter(initialCategoryId, false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Transaktioner")
                        latestDate?.let {
                            Text(
                                "Senaste: ${Dates.displayIso(it)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::setQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Sök beskrivning") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = filter.categoryId == null && !filter.onlyUncategorized,
                        onClick = { viewModel.selectCategoryFilter(null, false) },
                        label = { Text("Alla") },
                    )
                }
                item {
                    FilterChip(
                        selected = filter.onlyUncategorized,
                        onClick = { viewModel.selectCategoryFilter(null, true) },
                        label = { Text(UNCATEGORIZED_LABEL) },
                    )
                }
                items(categories, key = { it.id }) { c ->
                    FilterChip(
                        selected = filter.categoryId == c.id,
                        onClick = { viewModel.selectCategoryFilter(c.id, false) },
                        label = { Text(c.name) },
                    )
                }
            }

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Inga transaktioner", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Importera en xlsx-fil från din bank för att komma igång.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { tx ->
                        TransactionRow(tx = tx, onClick = { editing = tx })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editing?.let { tx ->
        CategoryPickerDialog(
            categories = categories,
            current = tx.category?.id,
            onDismiss = { editing = null },
            onDelete = {
                viewModel.delete(tx.id)
                editing = null
            },
            onPick = { newCategoryId ->
                viewModel.setCategory(tx.id, newCategoryId)
                editing = null
                val picked = categories.firstOrNull { it.id == newCategoryId }
                if (picked != null && newCategoryId != tx.category?.id) {
                    rulePrompt = RulePrompt(
                        suggestion = suggestKeyword(tx.description),
                        category = picked,
                    )
                }
            },
        )
    }

    rulePrompt?.let { prompt ->
        TextInputDialog(
            title = "Skapa regel?",
            label = "Nyckelord → ${prompt.category.name}",
            initialValue = prompt.suggestion,
            confirmLabel = "Spara regel",
            onConfirm = { keyword ->
                viewModel.createKeywordRule(keyword, prompt.category.id) { applied ->
                    scope.launch {
                        snackbar.showSnackbar(
                            if (applied > 0) "Regel sparad. $applied transaktioner kategoriserade."
                            else "Regel sparad.",
                        )
                    }
                }
                rulePrompt = null
            },
            onDismiss = { rulePrompt = null },
        )
    }
}

private data class RulePrompt(val suggestion: String, val category: CategoryEntity)

/** Suggest the longest word in the description as a keyword (e.g. "ICA MAXI 1234" -> "maxi"). */
private fun suggestKeyword(description: String): String {
    return description
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 3 && it.any { ch -> ch.isLetter() } }
        .maxByOrNull { it.length }
        ?.lowercase()
        ?: description.trim().lowercase()
}

@Composable
private fun TransactionRow(tx: UiTransaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { "(ingen beskrivning)" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = Dates.displayIso(tx.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                CategoryChip(name = nameOf(tx.category), color = colorOf(tx.category))
            }
        }
        Text(
            text = Money.format(tx.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (tx.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    current: Long?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onPick: (Long?) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Välj kategori") },
        text = {
            LazyColumn {
                item {
                    PickerRow(
                        label = UNCATEGORIZED_LABEL,
                        color = UncategorizedColor,
                        selected = current == null,
                        onClick = { onPick(null) },
                    )
                }
                items(categories, key = { it.id }) { c ->
                    PickerRow(
                        label = c.name,
                        color = colorOf(c),
                        selected = current == c.id,
                        onClick = { onPick(c.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Ta bort", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@Composable
private fun PickerRow(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        com.kronkollen.ui.components.ColorDot(color, size = 14, modifier = Modifier.padding(end = 8.dp))
        Text(label)
    }
}

package com.kronkollen.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.ui.components.ColorDot
import com.kronkollen.ui.components.colorOf
import com.kronkollen.util.Dates
import com.kronkollen.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The category currently being edited in the amount dialog (null = no dialog).
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budget") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                MonthStepper(
                    label = Dates.monthYear(state.month.atDay(1)),
                    onPrev = viewModel::prevMonth,
                    onNext = viewModel::nextMonth,
                )
            }

            item {
                SummaryCard(spent = state.totalSpent, budget = state.totalBudget)
            }

            items(state.rows, key = { it.category.id }) { row ->
                BudgetRowCard(
                    row = row,
                    onClick = {
                        editing = EditTarget(row.category, row.budgetOre)
                    },
                )
            }

            if (state.unbudgeted.isNotEmpty()) {
                item {
                    Text(
                        "Lägg till budget",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(state.unbudgeted, key = { it.id }) { category ->
                    UnbudgetedRow(
                        category = category,
                        onClick = { editing = EditTarget(category, 0L) },
                    )
                }
            }

            if (state.rows.isEmpty() && state.unbudgeted.isEmpty()) {
                item { EmptyHint() }
            }
        }
    }

    editing?.let { target ->
        BudgetAmountDialog(
            categoryName = target.category.name,
            currentOre = target.currentOre,
            onSave = { ore ->
                viewModel.setBudget(target.category.id, ore)
                editing = null
            },
            onRemove = {
                viewModel.removeBudget(target.category.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

private data class EditTarget(val category: CategoryEntity, val currentOre: Long)

@Composable
private fun MonthStepper(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Föregående månad")
        }
        Text(
            text = label.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Nästa månad")
        }
    }
}

@Composable
private fun SummaryCard(spent: Long, budget: Long) {
    val over = spent > budget && budget > 0
    val fraction = if (budget == 0L) 0f else (spent.toFloat() / budget)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Denna månad", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    Money.format(spent),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "av ${Money.format(budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(8.dp),
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = remainingText(spent, budget),
                style = MaterialTheme.typography.labelMedium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun BudgetRowCard(row: BudgetRow, onClick: () -> Unit) {
    val color = colorOf(row.category)
    val barColor = if (row.overBudget) MaterialTheme.colorScheme.error else color
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(color, size = 14)
                Text(
                    text = row.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                )
                Text(
                    text = "${Money.format(row.spentOre, withSuffix = false)} / ${Money.format(row.budgetOre)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { row.fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(8.dp),
                color = barColor,
            )
            Text(
                text = remainingText(row.spentOre, row.budgetOre),
                style = MaterialTheme.typography.labelSmall,
                color = if (row.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun UnbudgetedRow(category: CategoryEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorDot(colorOf(category), size = 14)
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            Icon(
                Icons.Filled.Add,
                contentDescription = "Sätt budget",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Inga kategorier än", style = MaterialTheme.typography.titleMedium)
        Text(
            "Skapa kategorier under Kategorier för att sätta budget.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** "Kvar: 1 234 kr" or "234 kr över budget". */
private fun remainingText(spent: Long, budget: Long): String {
    if (budget <= 0) return ""
    val diff = budget - spent
    return if (diff >= 0) {
        "Kvar: ${Money.format(diff)}"
    } else {
        "${Money.format(-diff, withSuffix = true)} över budget"
    }
}

@Composable
private fun BudgetAmountDialog(
    categoryName: String,
    currentOre: Long,
    onSave: (Long) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = currentOre > 0
    var text by remember {
        mutableStateOf(if (isEditing) Money.format(currentOre, withSuffix = false) else "")
    }
    val parsed = Money.parseToOre(text)?.let { kotlin.math.abs(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Budget – $categoryName") },
        text = {
            Column {
                Text(
                    "Månadsbelopp i kronor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("t.ex. 5000") },
                    suffix = { Text("kr") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null && parsed > 0,
            ) { Text("Spara") }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = onRemove) {
                    Text("Ta bort", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Avbryt") }
            }
        },
    )
}

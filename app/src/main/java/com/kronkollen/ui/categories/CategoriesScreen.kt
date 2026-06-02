package com.kronkollen.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kronkollen.data.entity.CategoryEntity
import com.kronkollen.ui.components.ColorDot
import com.kronkollen.ui.components.colorOf
import com.kronkollen.ui.theme.CategoryPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kategorier") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ny kategori")
            }
        },
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                // Bottom inset so the last card clears the floating "+" button.
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.category.id }) { item ->
                    CategoryCard(
                        item = item,
                        onAddKeyword = { kw -> viewModel.addKeyword(item.category.id, kw) },
                        onRemoveKeyword = { viewModel.removeKeyword(it) },
                        onDelete = { pendingDelete = item.category },
                        onRename = { viewModel.renameCategory(item.category, it) },
                        onSetColor = { viewModel.setCategoryColor(item.category, it) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        TextInputDialog(
            title = "Ny kategori",
            label = "Namn",
            confirmLabel = "Lägg till",
            onConfirm = { viewModel.addCategory(it); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Ta bort kategori?") },
            text = {
                Text(
                    "“${category.name}” tas bort. Transaktioner i kategorin blir " +
                        "okategoriserade och dess nyckelord raderas.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    pendingDelete = null
                }) { Text("Ta bort") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Avbryt") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(
    item: CategoryWithKeywords,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (com.kronkollen.data.entity.KeywordRuleEntity) -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onSetColor: (Int) -> Unit,
) {
    var expanded by rememberSaveable(item.category.id) { mutableStateOf(false) }
    var newKeyword by rememberSaveable(item.category.id) { mutableStateOf("") }
    var showRename by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(
                    colorOf(item.category),
                    size = 20,
                    modifier = Modifier.clickable { showColorPicker = true },
                )
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                Text(
                    text = "${item.keywords.size} nyckelord",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Dölj" else "Visa",
                    )
                }
            }

            if (expanded) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.keywords.forEach { rule ->
                        KeywordChip(text = rule.keyword, onRemove = { onRemoveKeyword(rule) })
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        label = { Text("Nytt nyckelord (t.ex. ica)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        if (newKeyword.isNotBlank()) {
                            onAddKeyword(newKeyword)
                            newKeyword = ""
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Lägg till nyckelord")
                    }
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = { showRename = true }) { Text("Byt namn") }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Ta bort", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }

    if (showRename) {
        TextInputDialog(
            title = "Byt namn",
            label = "Namn",
            initialValue = item.category.name,
            confirmLabel = "Spara",
            onConfirm = { onRename(it); showRename = false },
            onDismiss = { showRename = false },
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            current = item.category.colorArgb,
            onDismiss = { showColorPicker = false },
            onPick = { onSetColor(it); showColorPicker = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Välj färg") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryPalette.forEach { color ->
                    val argb = color.toArgb()
                    val selected = argb == current
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onPick(argb) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@Composable
private fun KeywordChip(text: String, onRemove: () -> Unit) {
    androidx.compose.material3.AssistChip(
        onClick = onRemove,
        label = { Text(text) },
        trailingIcon = {
            Icon(Icons.Filled.Close, contentDescription = "Ta bort", modifier = Modifier.size(16.dp))
        },
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Inga kategorier än", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tryck på + för att skapa din första kategori.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

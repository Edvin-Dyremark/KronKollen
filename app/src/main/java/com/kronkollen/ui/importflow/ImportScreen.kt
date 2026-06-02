package com.kronkollen.ui.importflow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kronkollen.importer.AmountMode
import com.kronkollen.importer.ColumnMapping
import com.kronkollen.util.DateFormatOption
import com.kronkollen.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.onFilePicked(uri) }

    fun launchPicker() = picker.launch(
        arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream",
            "*/*",
        ),
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Importera") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state.step) {
                ImportStep.Idle -> IdleStep(::launchPicker)
                ImportStep.Loading -> LoadingStep()
                ImportStep.Mapping -> MappingStep(state, viewModel)
                ImportStep.Preview -> PreviewStep(state, viewModel)
                ImportStep.Done -> DoneStep(state, viewModel, ::launchPicker)
                ImportStep.Error -> ErrorStep(state, ::launchPicker)
            }
        }
    }
}

@Composable
private fun IdleStep(onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Importera transaktioner", style = MaterialTheme.typography.titleLarge)
        Text(
            "Välj en xlsx-fil exporterad från din bank. Du får mappa kolumnerna innan något sparas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onPick) { Text("Välj xlsx-fil") }
    }
}

@Composable
private fun LoadingStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text("Läser filen…", modifier = Modifier.padding(top = 12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappingStep(state: ImportUiState, viewModel: ImportViewModel) {
    val mapping = state.mapping
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Mappa kolumner", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = mapping.hasHeaderRow,
                    onCheckedChange = { viewModel.updateMapping(mapping.copy(hasHeaderRow = it)) },
                )
                Text("Första raden är rubriker", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            ColumnDropdown("Datum", mapping.dateColumn, state) {
                viewModel.updateMapping(mapping.copy(dateColumn = it))
            }
        }
        item {
            ColumnDropdown("Beskrivning", mapping.descriptionColumn, state) {
                viewModel.updateMapping(mapping.copy(descriptionColumn = it))
            }
        }
        item {
            Text("Belopp", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mapping.amountMode == AmountMode.SINGLE,
                    onClick = { viewModel.updateMapping(mapping.copy(amountMode = AmountMode.SINGLE)) },
                    label = { Text("En kolumn") },
                )
                FilterChip(
                    selected = mapping.amountMode == AmountMode.SPLIT,
                    onClick = { viewModel.updateMapping(mapping.copy(amountMode = AmountMode.SPLIT)) },
                    label = { Text("Separat in/ut") },
                )
            }
        }
        if (mapping.amountMode == AmountMode.SINGLE) {
            item {
                ColumnDropdown("Beloppskolumn", mapping.amountColumn, state) {
                    viewModel.updateMapping(mapping.copy(amountColumn = it))
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = mapping.flipSign,
                        onCheckedChange = { viewModel.updateMapping(mapping.copy(flipSign = it)) },
                    )
                    Text(
                        "Utgifter anges som positiva tal (vänd tecken)",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        } else {
            item {
                ColumnDropdown("Ut (utgift)", mapping.outColumn, state) {
                    viewModel.updateMapping(mapping.copy(outColumn = it))
                }
            }
            item {
                ColumnDropdown("In (inkomst)", mapping.inColumn, state) {
                    viewModel.updateMapping(mapping.copy(inColumn = it))
                }
            }
        }
        item { DateFormatDropdown(mapping.dateFormat) { viewModel.updateMapping(mapping.copy(dateFormat = it)) } }

        item { SamplePreview(state) }

        item {
            Button(
                onClick = { viewModel.buildPreview() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Förhandsgranska") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdown(
    label: String,
    selected: Int,
    state: ImportUiState,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = columnLabel(selected, state),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (i in 0 until state.columnCount) {
                DropdownMenuItem(
                    text = { Text(columnLabel(i, state)) },
                    onClick = { onSelect(i); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFormatDropdown(selected: DateFormatOption, onSelect: (DateFormatOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Datumformat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DateFormatOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SamplePreview(state: ImportUiState) {
    if (state.sampleRows.isEmpty()) return
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Filinnehåll (första raderna)", style = MaterialTheme.typography.labelMedium)
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                state.sampleRows.forEach { row ->
                    Row {
                        for (i in 0 until state.columnCount) {
                            Text(
                                text = row.getOrNull(i).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                modifier = Modifier
                                    .width(110.dp)
                                    .padding(end = 8.dp, top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewStep(state: ImportUiState, viewModel: ImportViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${state.parsedCount} transaktioner inlästa",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.skippedUnparseable > 0) {
                    Text(
                        "${state.skippedUnparseable} rader kunde inte tolkas och hoppas över.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "Dubbletter som redan finns hoppas över vid import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.previewRows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.description.ifBlank { "(ingen beskrivning)" }, maxLines = 1)
                        Text(
                            "${row.date} · ${row.categoryName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Text(
                        Money.format(row.amount),
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.amount < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { viewModel.backToMapping() }, modifier = Modifier.weight(1f)) {
                Text("Tillbaka")
            }
            Button(
                onClick = { viewModel.confirmImport() },
                modifier = Modifier.weight(1f),
                enabled = state.parsedCount > 0,
            ) { Text("Importera") }
        }
    }
}

@Composable
private fun DoneStep(state: ImportUiState, viewModel: ImportViewModel, onPickAnother: () -> Unit) {
    val result = state.result
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Import klar", style = MaterialTheme.typography.titleLarge)
        if (result != null) {
            Text(
                "${result.inserted} nya transaktioner sparade.",
                modifier = Modifier.padding(top = 8.dp),
            )
            if (result.skippedDuplicates > 0) {
                Text("${result.skippedDuplicates} dubbletter hoppades över.")
            }
            Text("${result.autoCategorized} kategoriserades automatiskt.")
            result.latestDate?.let {
                Text(
                    "Senaste transaktionen i importen: ${com.kronkollen.util.Dates.displayIso(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Button(onClick = { viewModel.reset(); onPickAnother() }, modifier = Modifier.padding(top = 20.dp)) {
            Text("Importera fler")
        }
        OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Klar")
        }
    }
}

@Composable
private fun ErrorStep(state: ImportUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Kunde inte läsa filen", style = MaterialTheme.typography.titleMedium)
        Text(
            state.error ?: "Okänt fel.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onRetry) { Text("Försök igen") }
    }
}

private fun columnLabel(index: Int, state: ImportUiState): String {
    val header = state.headers.getOrNull(index)?.takeIf { it.isNotBlank() }
    return header ?: "Kolumn ${index + 1}"
}

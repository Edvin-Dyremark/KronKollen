package com.kronkollen.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kronkollen.ui.components.BarChart
import com.kronkollen.ui.components.DonutChart
import com.kronkollen.ui.components.DonutSlice
import com.kronkollen.ui.components.LegendSwatch
import com.kronkollen.ui.components.colorOf
import com.kronkollen.ui.components.nameOf
import com.kronkollen.util.Dates
import com.kronkollen.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onCategoryClick: (Long) -> Unit,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Översikt") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RangePreset.entries) { preset ->
                        FilterChip(
                            selected = state.preset == preset,
                            onClick = { viewModel.selectPreset(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
            }

            item { SummaryCard(state) }

            if (state.totalSpent > 0) {
                item { SpendingByCategoryCard(state, onCategoryClick) }
                if (state.months.size >= 2) {
                    item { TrendCard(state) }
                }
            } else {
                item { EmptyCard() }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: OverviewUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Utgifter ${Dates.displayIso(state.range.start)} – ${Dates.displayIso(state.range.end)}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                Money.format(-state.totalSpent),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${state.transactionCount} transaktioner totalt",
                    style = MaterialTheme.typography.bodySmall,
                )
                state.latestDate?.let {
                    Text(
                        "Senaste: ${Dates.displayIso(it)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingByCategoryCard(state: OverviewUiState, onCategoryClick: (Long) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Per kategori", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                DonutChart(
                    slices = state.slices.map { DonutSlice(colorOf(it.category), it.amount) },
                    modifier = Modifier
                        .size(180.dp)
                        .aspectRatio(1f),
                )
            }
            state.slices.forEach { slice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { m ->
                            val id = slice.category?.id
                            if (id != null) m.clickable { onCategoryClick(id) } else m
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendSwatch(colorOf(slice.category))
                    Text(
                        nameOf(slice.category),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${(slice.fraction * 100).toInt()} %",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        Money.format(-slice.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCard(state: OverviewUiState) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Utgifter per månad", style = MaterialTheme.typography.titleMedium)
            BarChart(
                bars = state.months.map { it.label to it.amount },
                barColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    Card {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Inga utgifter i perioden", style = MaterialTheme.typography.titleMedium)
            Text(
                "Importera transaktioner eller välj en annan period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

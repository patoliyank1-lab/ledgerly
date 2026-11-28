package com.finledger.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.usecase.GetMonthlySummaryUseCase
import com.finledger.domain.usecase.PeriodComparison
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.screens.transactions.TransactionRowCard
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing
import java.util.Calendar

@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }
    var presetIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Reports", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Spending") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Cash Flow") })
            }

            when (tabIndex) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (state.spendingInsight.isNotBlank()) {
                            item {
                                SpendingInsightBanner(insight = state.spendingInsight)
                            }
                        }

                        item {
                            DateRangePresetRow(
                                selectedIndex = presetIndex,
                                onSelectPreset = { idx ->
                                    presetIndex = idx
                                    val now = System.currentTimeMillis()
                                    val (s, e) = when (idx) {
                                        0 -> GetMonthlySummaryUseCase.monthBounds()
                                        1 -> {
                                            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                                            GetMonthlySummaryUseCase.monthBounds(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                                        }
                                        else -> (now - 90L * 86_400_000L) to now
                                    }
                                    viewModel.setDateRange(s, e)
                                },
                            )
                        }

                        item {
                            PeriodComparisonCard(
                                comparison = state.periodComparison,
                                currency = state.primaryCurrency,
                            )
                        }

                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Text("Category Breakdown", style = MaterialTheme.typography.headlineMedium)
                                    if (state.categoryBreakdown.isEmpty()) {
                                        Text(
                                            text = "No personal expenses in this date range.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FinLedgerTheme.colors.inkMuted48,
                                            modifier = Modifier.padding(vertical = Spacing.md),
                                        )
                                    } else {
                                        SpendingPieChart(items = state.categoryBreakdown)
                                    }
                                }
                            }
                        }

                        items(state.categoryBreakdown, key = { it.categoryId ?: -1L }) { cat ->
                            val isSelected = state.hasSelectedCategory && state.selectedCategoryId == cat.categoryId
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectCategoryForDrilldown(cat.categoryId) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(cat.categoryName, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = "${cat.transactionCount} tx · ${"%.1f".format(cat.percent)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FinLedgerTheme.colors.inkMuted48,
                                        )
                                    }
                                    Text(
                                        text = MoneyFormatter.format(cat.totalPaise, state.primaryCurrency),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }

                        if (state.hasSelectedCategory && state.drilldownTransactions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Category Transactions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                            items(state.drilldownTransactions, key = { "dd_${it.transaction.id}" }) { item ->
                                TransactionRowCard(item = item, onClick = { onOpenTransaction(item.transaction.id) })
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Text("6-Month Cash Flow (Income vs Expense)", style = MaterialTheme.typography.headlineMedium)
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    MonthlyBarChart(points = state.cashFlowPoints)
                                }
                            }
                        }
                        items(state.cashFlowPoints, key = { it.label }) { pt ->
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(pt.label, style = MaterialTheme.typography.bodyLarge)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "In ${MoneyFormatter.format(pt.incomePaise, state.primaryCurrency)} · Out ${MoneyFormatter.format(pt.expensePaise, state.primaryCurrency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FinLedgerTheme.colors.inkMuted48,
                                        )
                                        Text(
                                            text = "Net: ${MoneyFormatter.format(pt.netSavingsPaise, state.primaryCurrency, showPlus = true)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (pt.netSavingsPaise >= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingInsightBanner(insight: String) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun DateRangePresetRow(
    selectedIndex: Int,
    onSelectPreset: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        listOf("This Month", "Last Month", "Last 90 Days").forEachIndexed { idx, label ->
            FilterChip(
                selected = selectedIndex == idx,
                onClick = { onSelectPreset(idx) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun PeriodComparisonCard(
    comparison: PeriodComparison,
    currency: String,
) {
    val expDelta = comparison.currentExpense - comparison.previousExpense
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("Month-over-Month Comparison", style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("This month spend", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                Text(MoneyFormatter.format(comparison.currentExpense, currency), style = MaterialTheme.typography.bodyLarge)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last month spend", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                Text(MoneyFormatter.format(comparison.previousExpense, currency), style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Difference", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                Text(
                    text = MoneyFormatter.format(expDelta, currency, showPlus = true),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (expDelta <= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
                )
            }
        }
    }
}


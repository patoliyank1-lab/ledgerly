package com.finledger.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun NetWorthScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nw = state.netWorth

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Net Worth", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Surface(
                    color = FinLedgerTheme.colors.tileDark1,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text("Total Net Worth", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                        Text(
                            text = MoneyFormatter.format(nw.netWorth, state.primaryCurrency),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Assets ${MoneyFormatter.format(nw.totalAssets, state.primaryCurrency)} − Liabilities ${MoneyFormatter.format(nw.totalLiabilities, state.primaryCurrency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        OutlinedButton(
                            onClick = { viewModel.recordNetWorthSnapshot(nw) },
                            shape = MaterialTheme.shapes.extraLarge,
                            border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                        ) {
                            Text("Save Snapshot Today", color = FinLedgerTheme.colors.skyLinkBlue)
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text("Net Worth History", style = MaterialTheme.typography.headlineMedium)
                            NetWorthChart(snapshots = state.netWorthSnapshots)
                        }
                    }

                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text("Assets", style = MaterialTheme.typography.headlineMedium)
                            BreakdownLine("Spendable Cash & Bank", nw.cashAndBankAssets, state.primaryCurrency)
                            BreakdownLine("Investments & FDs", nw.investmentAssets, state.primaryCurrency)
                            BreakdownLine("Receivables (Owed to you — not cash)", nw.receivablesAssets, state.primaryCurrency)
                            BreakdownLine("Total Assets", nw.totalAssets, state.primaryCurrency, bold = true)
                        }
                    }

                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text("Liabilities", style = MaterialTheme.typography.headlineMedium)
                            BreakdownLine("Credit Card Outstanding", nw.creditCardLiabilities, state.primaryCurrency)
                            BreakdownLine("Institutional Loans", nw.borrowedLoanLiabilities, state.primaryCurrency)
                            BreakdownLine("Payables (You owe others)", nw.payablesLiabilities, state.primaryCurrency)
                            BreakdownLine("Total Liabilities", nw.totalLiabilities, state.primaryCurrency, bold = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, amount: Long, currency: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            color = if (bold) MaterialTheme.colorScheme.onSurface else FinLedgerTheme.colors.inkMuted48,
        )
        Text(
            text = MoneyFormatter.format(amount, currency),
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        )
    }
}


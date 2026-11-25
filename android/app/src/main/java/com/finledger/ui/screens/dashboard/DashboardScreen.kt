package com.finledger.ui.screens.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.ui.theme.Spacing

@Composable
fun DashboardScreen(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBills: () -> Unit = {},
    onOpenBudgets: () -> Unit = {},
    onOpenBalances: () -> Unit = {},
    onOpenNetWorth: () -> Unit = {},
    onOpenInvestments: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("FinLedger", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            QuickActionFab(
                onAddExpense = onAddExpense,
                onAddIncome = onAddIncome,
                onAddTransfer = onAddTransfer,
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
                DashboardHeader(
                    userName = state.userName,
                    summary = state.accountSummary,
                    currency = state.primaryCurrency,
                )
            }

            item {
                MonthlySummaryCard(
                    summary = state.monthlySummary,
                    currency = state.primaryCurrency,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                NetWorthCard(
                    netWorth = state.netWorth,
                    currency = state.primaryCurrency,
                    onOpenNetWorth = onOpenNetWorth,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                InvestmentValueCard(
                    portfolio = state.portfolio,
                    currency = state.primaryCurrency,
                    onOpenInvestments = onOpenInvestments,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                FinancialHealthCard(
                    monthly = state.monthlySummary,
                    netWorth = state.netWorth,
                    currency = state.primaryCurrency,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                LendingBorrowingCard(
                    totalToReceive = state.totalToReceive,
                    totalToPay = state.totalToPay,
                    currency = state.primaryCurrency,
                    onOpenBalances = onOpenBalances,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                UpcomingBillsCard(
                    bills = state.upcomingBills,
                    currency = state.primaryCurrency,
                    onOpenBills = onOpenBills,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                BudgetProgressCard(
                    budgets = state.budgetProgress,
                    currency = state.primaryCurrency,
                    onOpenBudgets = onOpenBudgets,
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                RecentTransactionsList(
                    transactions = state.recentTransactions,
                    onSeeAll = onSeeAllTransactions,
                    onOpenTransaction = onOpenTransaction,
                )
            }

            item { Spacer(modifier = Modifier.height(Spacing.xl)) }
        }
    }
}

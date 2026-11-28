package com.finledger.ui.screens.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.model.SetupChecklistState
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun DashboardScreen(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMore: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenBills: () -> Unit = {},
    onOpenBudgets: () -> Unit = {},
    onOpenBackup: () -> Unit = onOpenSettings,
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
                    IconButton(onClick = onOpenMore) {
                        Icon(Icons.Outlined.GridView, contentDescription = "More tools")
                    }
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
                    onOpenAccounts = onOpenAccounts,
                )
            }

            item {
                MonthlySummaryCard(
                    summary = state.monthlySummary,
                    currency = state.primaryCurrency,
                    netWorth = state.netWorth,
                    showHealthNote = !state.isSimpleMode,
                )
            }

            if (state.setupChecklist.shouldShowFirstRunCta) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    FirstRunExpenseCard(onAddExpense = onAddExpense)
                }
            }

            if (state.setupChecklist.shouldShowChecklist) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    SetupChecklistCard(
                        checklist = state.setupChecklist,
                        onAddExpense = onAddExpense,
                        onOpenBudgets = onOpenBudgets,
                        onOpenBackup = onOpenBackup,
                        onDismiss = viewModel::dismissSetupChecklist,
                    )
                }
            }

            if (state.topAlert != null) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    TopAlertCard(
                        alert = state.topAlert,
                        onClick = {
                            when (state.topAlert?.kind) {
                                DashboardAlertKind.BUDGET_ALERT -> onOpenBudgets()
                                DashboardAlertKind.OVERDUE_BILLS,
                                DashboardAlertKind.UPCOMING_BILLS -> onOpenBills()
                                null -> Unit
                            }
                        },
                    )
                }
            }

            if (state.showDashboardNetWorth && (!state.isSimpleMode || state.trackInvestments)) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    NetWorthCard(
                        netWorth = state.netWorth,
                        currency = state.primaryCurrency,
                        onOpenNetWorth = onOpenNetWorth,
                    )
                }
            }

            if (state.showDashboardNetWorth && state.trackInvestments && state.portfolio.items.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    InvestmentValueCard(
                        portfolio = state.portfolio,
                        currency = state.primaryCurrency,
                        onOpenInvestments = onOpenInvestments,
                    )
                }
            }

            if (!state.isSimpleMode && state.trackSharedAndPeople) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    LendingBorrowingCard(
                        totalToReceive = state.totalToReceive,
                        totalToPay = state.totalToPay,
                        currency = state.primaryCurrency,
                        onOpenBalances = onOpenBalances,
                    )
                }
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

@Composable
private fun FirstRunExpenseCard(
    onAddExpense: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "Ready to track your spending",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Log your first expense to see live monthly totals and category insights.",
                style = MaterialTheme.typography.bodySmall,
                color = FinLedgerTheme.colors.inkMuted48,
            )
            Button(
                onClick = onAddExpense,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add your first expense")
            }
        }
    }
}

@Composable
private fun SetupChecklistCard(
    checklist: SetupChecklistState,
    onAddExpense: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenBackup: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Getting Started (${checklist.completedCount}/3)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 0.dp),
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                }
            }
            HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
            ChecklistStepRow(
                title = "1. Add first expense",
                completed = checklist.hasAnyTransaction,
                onClick = onAddExpense,
            )
            HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
            ChecklistStepRow(
                title = "2. Set a monthly budget",
                completed = checklist.hasAnyBudget,
                onClick = onOpenBudgets,
            )
            HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
            ChecklistStepRow(
                title = "3. Turn on backup",
                completed = checklist.hasBackupOrAutoBackupEnabled,
                onClick = onOpenBackup,
            )
        }
    }
}

@Composable
private fun ChecklistStepRow(
    title: String,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.inkMuted48,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = if (completed) FinLedgerTheme.colors.inkMuted48 else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!completed) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = FinLedgerTheme.colors.inkMuted48,
            )
        }
    }
}

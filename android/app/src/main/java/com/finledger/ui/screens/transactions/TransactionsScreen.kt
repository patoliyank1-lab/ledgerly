package com.finledger.ui.screens.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.TransactionType
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.components.IconMapper
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun TransactionsScreen(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onOpenTransactionDetail: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val hasActiveFilter = state.filter.type != null ||
        state.filter.accountId != null ||
        state.filter.categoryId != null ||
        state.filter.startDateMs != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Activity", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Filter transactions",
                            tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Expense") },
                        onClick = { showFabMenu = false; onAddExpense() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add Income") },
                        onClick = { showFabMenu = false; onAddIncome() },
                    )
                    DropdownMenuItem(
                        text = { Text("Transfer Between Accounts") },
                        onClick = { showFabMenu = false; onAddTransfer() },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TransactionSearchBar(
                query = state.filter.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )

            if (state.groupedTransactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "No transactions found",
                    subtitle = if (hasActiveFilter || state.filter.searchQuery.isNotBlank()) {
                        "Try clearing your search or filters."
                    } else {
                        "Tap + to record your first expense, income, or transfer."
                    },
                    actionLabel = if (hasActiveFilter || state.filter.searchQuery.isNotBlank()) "Clear Filters" else "Add Expense",
                    onAction = {
                        if (hasActiveFilter || state.filter.searchQuery.isNotBlank()) {
                            viewModel.clearFilter()
                        } else {
                            onAddExpense()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Spacing.xxl),
                ) {
                    state.groupedTransactions.forEach { (dateHeader, list) ->
                        item(key = "header_$dateHeader") {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                                modifier = Modifier.padding(
                                    start = Spacing.md,
                                    end = Spacing.md,
                                    top = Spacing.md,
                                    bottom = Spacing.xs,
                                ),
                            )
                        }
                        items(list, key = { it.transaction.id }) { item ->
                            TransactionRowCard(
                                item = item,
                                onClick = { onOpenTransactionDetail(item.transaction.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        TransactionFilterSheet(
            currentFilter = state.filter,
            accounts = state.accounts,
            categories = state.allCategories,
            onDismiss = { showFilterSheet = false },
            onApply = viewModel::updateFilter,
        )
    }
}

@Composable
fun TransactionRowCard(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tx = item.transaction
    val isPositive = tx.type == TransactionType.INCOME ||
        tx.type == TransactionType.REFUND ||
        tx.type == TransactionType.BORROWING ||
        tx.type == TransactionType.REPAYMENT_RECEIVED ||
        tx.type == TransactionType.INVESTMENT_SELL ||
        (tx.type == TransactionType.ACCOUNT_ADJUSTMENT && tx.adjustmentSign > 0)

    val amountLabel = when (tx.type) {
        TransactionType.TRANSFER -> MoneyFormatter.format(tx.amount, tx.currency)
        else -> "${if (isPositive) "+" else "-"}${MoneyFormatter.format(tx.amount, tx.currency)}"
    }

    val amountColor = when {
        tx.type == TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
        isPositive -> FinLedgerTheme.colors.positive
        else -> MaterialTheme.colorScheme.onSurface
    }

    val title = when (tx.type) {
        TransactionType.TRANSFER -> "${item.accountName} → ${item.toAccountName ?: "Account"}"
        else -> tx.merchantOrPayee.ifBlank {
            tx.description.ifBlank { item.category?.name ?: tx.type.displayName }
        }
    }

    val subtitle = buildList {
        item.category?.name?.let { add(it) }
        if (tx.type != TransactionType.TRANSFER) add(item.accountName)
        if (!tx.isPersonal) add("Shared (your share: ${MoneyFormatter.format(tx.personalAmount, tx.currency)})")
    }.joinToString(" · ")

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = FinLedgerTheme.colors.parchment,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (tx.type == TransactionType.TRANSFER) {
                                Icons.Outlined.SwapHoriz
                            } else {
                                IconMapper.categoryIcon(item.category?.iconName ?: "")
                            },
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.inkMuted48,
                        )
                    }
                }
            }
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor,
            )
        }
    }
}


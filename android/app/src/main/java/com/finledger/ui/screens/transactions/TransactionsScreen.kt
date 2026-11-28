package com.finledger.ui.screens.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.TransactionType
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.components.IconMapper
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun TransactionsScreen(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onOpenTransactionDetail: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit = { onOpenTransactionDetail(it) },
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undoCoordinator = LocalUndoCoordinator.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkCategoryDialog by remember { mutableStateOf(false) }

    val visibleItems = remember(state.groupedTransactions) {
        state.groupedTransactions.values.flatten()
    }
    val visibleIds = remember(visibleItems) {
        visibleItems.map { it.transaction.id }.toSet()
    }
    val isSelectionMode = selectedIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedIds = emptySet()
    }

    LaunchedEffect(visibleIds) {
        if (selectedIds.isNotEmpty()) {
            selectedIds = selectedIds.intersect(visibleIds)
        }
    }

    val activeMonth = remember(state.filter.startDateMs, state.filter.endDateMs) {
        TransactionsViewModel.resolveYearMonthFromBounds(state.filter.startDateMs, state.filter.endDateMs)
    }
    val currentMonthKey = remember { TransactionsViewModel.currentYearMonth() }
    val hasDateFilter = state.filter.startDateMs != null || state.filter.endDateMs != null
    val hasActiveFilter = state.filter.type != null ||
        state.filter.accountId != null ||
        state.filter.categoryId != null ||
        hasDateFilter ||
        state.filter.onlyShared
    val summaryStrip = remember(state) {
        TransactionsViewModel.formatFilteredSummaryText(state)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == visibleIds.size) emptySet() else visibleIds
                            },
                        ) {
                            Icon(Icons.Outlined.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = { showBulkCategoryDialog = true }) {
                            Icon(Icons.Outlined.Category, contentDescription = "Change category")
                        }
                        IconButton(
                            onClick = {
                                viewModel.bulkDelete(selectedIds) { deleted ->
                                    val msg = if (deleted.size == 1) "Deleted 1 item" else "Deleted ${deleted.size} items"
                                    undoCoordinator?.showUndo(message = msg) {
                                        viewModel.bulkRestoreDirect(deleted)
                                    }
                                    selectedIds = emptySet()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete selected",
                                tint = FinLedgerTheme.colors.negative,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
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
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
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

            ActivityMonthNavigator(
                startDateMs = state.filter.startDateMs,
                activeMonthSelected = activeMonth != null,
                hasCustomDateRange = hasDateFilter && activeMonth == null,
                onPreviousMonth = { viewModel.stepMonth(-1) },
                onNextMonth = { viewModel.stepMonth(1) },
                onToggleMonthOrAll = {
                    if (hasDateFilter) viewModel.clearDateRange() else viewModel.selectCurrentMonth()
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(
                    selected = activeMonth == currentMonthKey,
                    onClick = viewModel::toggleCurrentMonthFilter,
                    label = { Text("This Month") },
                )
                FilterChip(
                    selected = state.filter.type == TransactionType.EXPENSE,
                    onClick = { viewModel.toggleTypeFilter(TransactionType.EXPENSE) },
                    label = { Text("Expense") },
                )
                FilterChip(
                    selected = state.filter.type == TransactionType.INCOME,
                    onClick = { viewModel.toggleTypeFilter(TransactionType.INCOME) },
                    label = { Text("Income") },
                )
                FilterChip(
                    selected = state.filter.type == TransactionType.TRANSFER,
                    onClick = { viewModel.toggleTypeFilter(TransactionType.TRANSFER) },
                    label = { Text("Transfer") },
                )
                FilterChip(
                    selected = state.filter.onlyShared,
                    onClick = viewModel::toggleSharedFilter,
                    label = { Text("Shared") },
                )
            }

            if (summaryStrip != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = summaryStrip,
                        style = MaterialTheme.typography.labelMedium,
                        color = FinLedgerTheme.colors.inkMuted80,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasActiveFilter || state.filter.searchQuery.isNotBlank()) {
                        Text(
                            text = "Clear Filters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = viewModel::clearFilter)
                                .padding(start = Spacing.sm),
                        )
                    }
                }
                HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
            }

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
                            val txId = item.transaction.id
                            val isSelected = txId in selectedIds
                            SwipeableTransactionRow(
                                item = item,
                                swipeEnabled = !isSelectionMode,
                                selectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - txId else selectedIds + txId
                                    } else {
                                        onOpenTransactionDetail(txId)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + txId
                                },
                                onEdit = { onEditTransaction(txId) },
                                onDelete = {
                                    val tx = item.transaction
                                    val label = tx.merchantOrPayee.ifBlank {
                                        item.category?.name ?: tx.type.displayName
                                    }
                                    viewModel.deleteTransaction(tx.id) {
                                        undoCoordinator?.showDeleted(label) {
                                            viewModel.restoreTransactionDirect(tx.id)
                                        }
                                    }
                                },
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

    if (showBulkCategoryDialog && selectedIds.isNotEmpty()) {
        val selectedItems = visibleItems.filter { it.transaction.id in selectedIds }
        val hasExpense = selectedItems.any {
            it.transaction.type == TransactionType.EXPENSE || it.transaction.type == TransactionType.REFUND
        }
        val hasIncome = selectedItems.any { it.transaction.type == TransactionType.INCOME }
        val eligibleCategories = when {
            hasExpense && !hasIncome -> state.expenseCategories
            hasIncome && !hasExpense -> state.incomeCategories
            hasExpense || hasIncome -> state.allCategories.filter { !it.isDeleted }
            else -> emptyList()
        }
        var pickedCategoryId by remember(showBulkCategoryDialog, selectedIds) {
            mutableStateOf(eligibleCategories.firstOrNull()?.id)
        }

        AlertDialog(
            onDismissRequest = { showBulkCategoryDialog = false },
            title = { Text("Change Category (${selectedIds.size})") },
            text = {
                if (eligibleCategories.isEmpty()) {
                    Text(
                        text = "Selected transactions do not support categories (e.g., Transfers).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        CategoryPickerGrid(
                            categories = eligibleCategories,
                            selectedCategoryId = pickedCategoryId,
                            onSelectCategory = { pickedCategoryId = it.id },
                        )
                    }
                }
            },
            confirmButton = {
                if (eligibleCategories.isNotEmpty()) {
                    TextButton(
                        enabled = pickedCategoryId != null,
                        onClick = {
                            val targetCat = pickedCategoryId ?: return@TextButton
                            viewModel.bulkUpdateCategory(selectedIds, targetCat) {
                                selectedIds = emptySet()
                                showBulkCategoryDialog = false
                            }
                        },
                    ) { Text("Apply") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkCategoryDialog = false }) {
                    Text(if (eligibleCategories.isEmpty()) "Close" else "Cancel")
                }
            },
        )
    }
}

@Composable
private fun ActivityMonthNavigator(
    startDateMs: Long?,
    activeMonthSelected: Boolean,
    hasCustomDateRange: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleMonthOrAll: () -> Unit,
) {
    val centerLabel = when {
        activeMonthSelected && startDateMs != null -> DateFormatter.formatMonthYear(startDateMs)
        hasCustomDateRange -> "Custom Dates"
        else -> "All Time"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                )
            }
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable(onClick = onToggleMonthOrAll)
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                )
            }
        }
        TextButton(onClick = onToggleMonthOrAll) {
            Text(
                text = if (activeMonthSelected || hasCustomDateRange) "All Time" else "This Month",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SwipeableTransactionRow(
    item: TransactionWithDetails,
    swipeEnabled: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnDelete by rememberUpdatedState(onDelete)
    var deleteDispatched by remember(item.transaction.id) { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    currentOnEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!deleteDispatched) {
                        deleteDispatched = true
                        currentOnDelete()
                    }
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = swipeEnabled,
        enableDismissFromEndToStart = swipeEnabled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> FinLedgerTheme.colors.negative.copy(alpha = 0.14f)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
                    .clip(MaterialTheme.shapes.large)
                    .background(bgColor)
                    .padding(horizontal = Spacing.md),
                contentAlignment = alignment,
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit transaction",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge,
                            color = FinLedgerTheme.colors.negative,
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = FinLedgerTheme.colors.negative,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    ) {
        TransactionRowCard(
            item = item,
            onClick = onClick,
            selectionMode = selectionMode,
            isSelected = isSelected,
            onLongClick = onLongClick,
        )
    }
}

@Composable
fun TransactionRowCard(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
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
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
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
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                }
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


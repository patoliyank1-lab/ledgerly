package com.finledger.ui.screens.bills

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.BillEntity
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun BillsScreen(
    onBack: () -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (Long) -> Unit = {},
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bills & Subscriptions", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBill,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add bill")
            }
        },
    ) { padding ->
        BillsContent(
            onAddBill = onAddBill,
            onEditBill = onEditBill,
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            viewModel = viewModel,
        )
    }
}

@Composable
fun BillsContent(
    onAddBill: () -> Unit,
    onEditBill: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undoCoordinator = LocalUndoCoordinator.current
    var tabIndex by remember { mutableIntStateOf(0) }
    var payingBill by remember { mutableStateOf<BillEntity?>(null) }
    val now = System.currentTimeMillis()
    val overdue = state.bills.filter { it.nextDueDateMs < now }
    val upcoming = state.bills.filter { it.nextDueDateMs >= now }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Upcoming (${upcoming.size})") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Overdue (${overdue.size})") })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Paid (${state.payments.size})") })
        }

        when (tabIndex) {
            0, 1 -> {
                val list = if (tabIndex == 0) upcoming else overdue
                if (list.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Outlined.ReceiptLong,
                        title = if (tabIndex == 0) "No upcoming bills" else "No overdue bills",
                        subtitle = "Add recurring utility bills, rent, EMIs, or subscriptions.",
                        actionLabel = "Add Bill",
                        onAction = onAddBill,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        items(list, key = { it.id }) { bill ->
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditBill(bill.id) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bill.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = "Due ${DateFormatter.formatDate(bill.nextDueDateMs)} · ${bill.frequency.displayName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (bill.nextDueDateMs < now) FinLedgerTheme.colors.negative else FinLedgerTheme.colors.inkMuted48,
                                        )
                                        Text(
                                            text = MoneyFormatter.format(bill.expectedAmount, bill.currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    if (state.accounts.isNotEmpty()) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Button(
                                                onClick = {
                                                    viewModel.quickPayBill(
                                                        bill = bill,
                                                        onPaid = { receipt ->
                                                            undoCoordinator?.showUndo("Marked ${bill.name} paid") {
                                                                viewModel.undoBillPaymentDirect(receipt)
                                                            }
                                                        },
                                                        onNeedsDialog = { payingBill = bill },
                                                    )
                                                },
                                                shape = MaterialTheme.shapes.extraLarge,
                                            ) {
                                                Text("Mark Paid", style = MaterialTheme.typography.labelLarge)
                                            }
                                            TextButton(
                                                onClick = { payingBill = bill },
                                                contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 0.dp),
                                            ) {
                                                Text("Edit & Pay", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(state.payments, key = { it.id }) { payment ->
                        val billName = state.bills.firstOrNull { it.id == payment.billId }?.name ?: "Bill #${payment.billId}"
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
                                Column {
                                    Text(billName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Paid ${DateFormatter.formatDate(payment.paidDate ?: payment.dueDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FinLedgerTheme.colors.inkMuted48,
                                    )
                                }
                                Text(
                                    text = MoneyFormatter.format(payment.amount, state.primaryCurrency),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = FinLedgerTheme.colors.positive,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    payingBill?.let { bill ->
        var amountText by remember(bill.id) { mutableStateOf(MoneyFormatter.paiseToInputString(bill.expectedAmount)) }
        var selectedAccountId by remember(bill.id) {
            mutableStateOf(
                BillsViewModel.resolveBillPayAccountId(bill, state.accounts, state.lastUsedAccountId)
            )
        }
        var dialogError by remember(bill.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { payingBill = null },
            title = { Text("Edit & Pay Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(bill.name, style = MaterialTheme.typography.bodyLarge)
                    AmountTextField(
                        value = amountText,
                        onValueChange = { amountText = it; dialogError = null },
                        label = "Amount Paid",
                        currency = bill.currency,
                    )
                    AccountPickerDropdown(
                        accounts = state.accounts,
                        selectedAccountId = selectedAccountId,
                        onSelectAccount = { selectedAccountId = it.account.id },
                        label = "Pay From Account",
                    )
                    dialogError?.let {
                        Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = MoneyFormatter.parseToPaise(amountText) ?: 0L
                        val accId = selectedAccountId
                        if (amt <= 0L) {
                            dialogError = "Enter a valid positive amount"
                            return@Button
                        }
                        if (accId == null) {
                            dialogError = "Select a payment account"
                            return@Button
                        }
                        viewModel.markBillPaid(bill, amt, accId) { receipt ->
                            payingBill = null
                            undoCoordinator?.showUndo("Marked ${bill.name} paid") {
                                viewModel.undoBillPaymentDirect(receipt)
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { payingBill = null }) { Text("Cancel") }
            },
        )
    }
}


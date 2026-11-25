package com.finledger.ui.screens.accounts

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
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accountWithBalance by viewModel.observeAccount(accountId).collectAsStateWithLifecycle()
    val transactions by viewModel.observeTransactionsForAccount(accountId).collectAsStateWithLifecycle()
    var showEditSheet by remember { mutableStateOf(false) }
    var showReconcileDialog by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    val undoCoordinator = LocalUndoCoordinator.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val item = accountWithBalance

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(item?.account?.name ?: "Account", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit account")
                        }
                        IconButton(onClick = { showArchiveConfirm = true }) {
                            Icon(Icons.Outlined.Archive, contentDescription = "Archive account")
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteAccount(accountId)
                                undoCoordinator?.showDeleted(item.account.name) {
                                    viewModel.restoreAccountDirect(accountId)
                                }
                                onBack()
                            },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete account")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (item == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Account not found", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

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
                        Text(
                            text = item.account.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = MoneyFormatter.format(item.currentBalance, item.account.currency),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Opening: ${MoneyFormatter.format(item.account.openingBalance, item.account.currency)} · ${transactions.size} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        OutlinedButton(
                            onClick = { showReconcileDialog = true },
                            shape = MaterialTheme.shapes.extraLarge,
                            border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                        ) {
                            Text(
                                text = "Reconcile Balance",
                                color = FinLedgerTheme.colors.skyLinkBlue,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Account Ledger",
                    style = MaterialTheme.typography.titleSmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
            }

            items(transactions, key = { it.transaction.id }) { detail ->
                val tx = detail.transaction
                val isIncomingTransfer = tx.type == TransactionType.TRANSFER && tx.toAccountId == accountId
                val isPositiveAdjustment = tx.type == TransactionType.ACCOUNT_ADJUSTMENT && tx.adjustmentSign > 0
                val amountPrefix = when {
                    tx.type == TransactionType.INCOME || tx.type == TransactionType.REFUND ||
                        tx.type == TransactionType.BORROWING || tx.type == TransactionType.REPAYMENT_RECEIVED ||
                        tx.type == TransactionType.INVESTMENT_SELL || isPositiveAdjustment ||
                        isIncomingTransfer -> "+"
                    else -> "-"
                }
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
                        .clickable { onOpenTransaction(tx.id) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.merchantOrPayee.ifBlank { tx.description.ifBlank { detail.category?.name ?: tx.type.displayName } },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = DateFormatter.formatDate(tx.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        Text(
                            text = "$amountPrefix${MoneyFormatter.format(tx.amount, tx.currency)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (amountPrefix == "+") FinLedgerTheme.colors.positive else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet && item != null) {
        AddEditAccountSheet(
            existing = item.account,
            defaultCurrency = item.account.currency,
            onDismiss = { showEditSheet = false },
            onSave = { name, type, bank, masked, currency, opening, limit, billingDay, dueDay, notes ->
                viewModel.saveAccount(
                    existing = item.account,
                    name = name,
                    type = type,
                    bankOrProvider = bank,
                    maskedIdentifier = masked,
                    currency = currency,
                    openingBalancePaise = opening,
                    creditLimitPaise = limit,
                    billingCycleDay = billingDay,
                    dueDay = dueDay,
                    notes = notes,
                    onSaved = { showEditSheet = false },
                )
            },
        )
    }

    if (showReconcileDialog && item != null) {
        var targetText by remember { mutableStateOf(MoneyFormatter.paiseToInputString(item.currentBalance)) }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReconcileDialog = false },
            title = { Text("Reconcile Balance", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "Current calculated balance: ${MoneyFormatter.format(item.currentBalance, item.account.currency)}. Enter actual statement balance to create an audit-trailed adjustment.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    AmountTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = "Actual Balance",
                        currency = item.account.currency,
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason / Statement Ref") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = MoneyFormatter.parseToPaise(targetText) ?: item.currentBalance
                        viewModel.reconcileAccount(accountId, item.currentBalance, target, reason)
                        showReconcileDialog = false
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Adjust") }
            },
            dismissButton = {
                TextButton(onClick = { showReconcileDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showArchiveConfirm && item != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive ${item.account.name}?") },
            text = { Text("Archived accounts are hidden from active balances while keeping full transaction history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirm = false
                        viewModel.archiveAccount(accountId)
                        onBack()
                    },
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            },
        )
    }
}


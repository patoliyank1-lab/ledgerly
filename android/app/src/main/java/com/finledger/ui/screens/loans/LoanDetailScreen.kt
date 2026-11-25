package com.finledger.ui.screens.loans

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.LoanStatus
import com.finledger.data.local.entity.LoanType
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun LoanDetailScreen(
    loanId: Long,
    onBack: () -> Unit,
    onRecordRepayment: (Long) -> Unit,
    viewModel: LoansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val repayments by viewModel.observeRepayments(loanId).collectAsStateWithLifecycle()
    val item = state.loans.firstOrNull { it.loan.id == loanId }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(item?.loan?.personOrOrg ?: "Loan Detail", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        if (item.loan.status != LoanStatus.CLOSED) {
                            IconButton(onClick = { showCloseConfirm = true }) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = "Close loan")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete loan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (item == null) {
            Column(modifier = Modifier.padding(padding).padding(Spacing.lg)) {
                Text("Loan not found", style = MaterialTheme.typography.bodyMedium)
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
                            text = if (item.loan.type == LoanType.LENT) "Outstanding Receivable" else "Outstanding Payable",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Text(
                            text = MoneyFormatter.format(item.outstandingPrincipalPaise, item.loan.currency),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Principal: ${MoneyFormatter.format(item.loan.principalAmount, item.loan.currency)} · Repaid: ${MoneyFormatter.format(item.principalRepaidPaise, item.loan.currency)} · Interest: ${MoneyFormatter.format(item.interestPaidPaise, item.loan.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        if (item.loan.status != LoanStatus.CLOSED) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Button(
                                onClick = { onRecordRepayment(loanId) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Record Repayment") }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Repayment History",
                    style = MaterialTheme.typography.titleSmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
            }

            items(repayments, key = { it.id }) { rep ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(DateFormatter.formatDate(rep.date), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Principal: ${MoneyFormatter.format(rep.principalAmount, item.loan.currency)} · Interest/Fee: ${MoneyFormatter.format(rep.interestAmount + rep.feeAmount, item.loan.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(rep.principalAmount + rep.interestAmount + rep.feeAmount, item.loan.currency),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    if (showCloseConfirm && item != null) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("Close / Write-Off Loan?") },
            text = { Text("Marks this loan as Closed regardless of remaining balance.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.closeLoan(loanId)
                    showCloseConfirm = false
                }) { Text("Close Loan") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Loan?") },
            text = { Text("This loan record will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteLoan(loanId, onDone = onBack)
                }) { Text("Delete", color = FinLedgerTheme.colors.negative) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun RecordRepaymentScreen(
    loanId: Long,
    onBack: () -> Unit,
    viewModel: LoansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val item = state.loans.firstOrNull { it.loan.id == loanId }

    var principalText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts, item) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = item?.loan?.accountId ?: state.accounts.first().account.id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Record Repayment", style = MaterialTheme.typography.headlineMedium) },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (item != null) {
                Text(
                    text = "Outstanding Principal: ${MoneyFormatter.format(item.outstandingPrincipalPaise, item.loan.currency)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            AmountTextField(
                value = principalText,
                onValueChange = { principalText = it; errorText = null },
                label = "Principal Repayment",
                currency = item?.loan?.currency ?: state.primaryCurrency,
            )

            AmountTextField(
                value = interestText,
                onValueChange = { interestText = it },
                label = "Interest Component (Optional)",
                currency = item?.loan?.currency ?: state.primaryCurrency,
            )

            AmountTextField(
                value = feeText,
                onValueChange = { feeText = it },
                label = "Late Fee / Charges (Optional)",
                currency = item?.loan?.currency ?: state.primaryCurrency,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = "Account",
            )

            DatePickerField(dateMs = dateMs, onDateSelected = { dateMs = it })

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            (errorText ?: state.errorMessage)?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val principal = MoneyFormatter.parseToPaise(principalText) ?: 0L
                    val interest = MoneyFormatter.parseToPaise(interestText) ?: 0L
                    val fee = MoneyFormatter.parseToPaise(feeText) ?: 0L
                    if (principal <= 0L && interest <= 0L && fee <= 0L) {
                        errorText = "Enter a valid repayment amount"
                        return@Button
                    }
                    viewModel.recordRepayment(
                        loanId = loanId,
                        principalPaise = principal,
                        interestPaise = interest,
                        feePaise = fee,
                        dateMs = dateMs,
                        accountId = selectedAccountId,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Repayment")
            }
        }
    }
}


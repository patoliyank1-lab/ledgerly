package com.finledger.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.finledger.data.local.entity.TransactionType
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun EditTransactionScreen(
    transactionId: Long,
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tx by viewModel.observeTransaction(transactionId).collectAsStateWithLifecycle()

    var initialized by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedToAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var merchant by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tx) {
        val current = tx
        if (current != null && !initialized) {
            amountText = MoneyFormatter.paiseToInputString(current.amount)
            selectedAccountId = current.accountId
            selectedToAccountId = current.toAccountId
            selectedCategoryId = current.categoryId
            dateMs = current.date
            merchant = current.merchantOrPayee
            description = current.description
            notes = current.notes
            initialized = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val current = tx
        if (current == null) {
            Column(modifier = Modifier.padding(padding).padding(Spacing.lg)) {
                Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AmountTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                currency = current.currency,
                isError = amountError != null,
                supportingText = amountError,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = if (current.type == TransactionType.TRANSFER) "From Account" else "Account",
            )

            if (current.type == TransactionType.TRANSFER) {
                AccountPickerDropdown(
                    accounts = state.accounts,
                    selectedAccountId = selectedToAccountId,
                    onSelectAccount = { selectedToAccountId = it.account.id },
                    label = "To Account",
                )
            } else if (
                current.type == TransactionType.EXPENSE ||
                current.type == TransactionType.REFUND ||
                current.type == TransactionType.INCOME
            ) {
                val cats = if (current.type == TransactionType.INCOME) state.incomeCategories else state.expenseCategories
                CategoryPickerGrid(
                    categories = cats,
                    selectedCategoryId = selectedCategoryId,
                    onSelectCategory = { selectedCategoryId = it.id },
                )
            }

            DatePickerField(dateMs = dateMs, onDateSelected = { dateMs = it })

            if (current.type != TransactionType.TRANSFER) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Source") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val paise = MoneyFormatter.parseToPaise(amountText)
                    if (paise == null || paise <= 0L) {
                        amountError = "Enter a valid positive amount"
                        return@Button
                    }
                    viewModel.updateTransaction(
                        existing = current,
                        amountPaise = paise,
                        accountId = selectedAccountId ?: current.accountId,
                        toAccountId = selectedToAccountId,
                        categoryId = selectedCategoryId,
                        dateMs = dateMs,
                        merchantOrPayee = merchant,
                        description = description,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Changes", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


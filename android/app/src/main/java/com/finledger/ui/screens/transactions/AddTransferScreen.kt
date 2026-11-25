package com.finledger.ui.screens.transactions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun AddTransferScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf("") }
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts) {
        if (fromAccountId == null && state.accounts.isNotEmpty()) {
            fromAccountId = state.accounts.first().account.id
        }
        if (toAccountId == null && state.accounts.size > 1) {
            toAccountId = state.accounts[1].account.id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Account Transfer", style = MaterialTheme.typography.headlineMedium) },
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
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Transfers move money between your own accounts (or pay a credit card bill) without changing your income or expense totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted80,
                    modifier = Modifier.padding(Spacing.md),
                )
            }

            AmountTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorText = null
                },
                label = "Transfer Amount",
                currency = state.primaryCurrency,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = fromAccountId,
                onSelectAccount = {
                    fromAccountId = it.account.id
                    errorText = null
                },
                label = "From Account",
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = toAccountId,
                onSelectAccount = {
                    toAccountId = it.account.id
                    errorText = null
                },
                label = "To Account (or Credit Card)",
            )

            DatePickerField(
                dateMs = dateMs,
                onDateSelected = { dateMs = it },
            )

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
                    val paise = MoneyFormatter.parseToPaise(amountText)
                    if (paise == null || paise <= 0L) {
                        errorText = "Enter a valid positive amount"
                        return@Button
                    }
                    val from = fromAccountId
                    val to = toAccountId
                    if (from == null || to == null) {
                        errorText = "Select both source and destination accounts"
                        return@Button
                    }
                    if (from == to) {
                        errorText = "Source and destination accounts must differ"
                        return@Button
                    }
                    viewModel.addTransfer(
                        amountPaise = paise,
                        fromAccountId = from,
                        toAccountId = to,
                        dateMs = dateMs,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Record Transfer", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


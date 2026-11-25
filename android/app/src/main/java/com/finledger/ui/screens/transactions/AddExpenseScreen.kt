package com.finledger.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var merchant by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRefund by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var amountError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts, state.expenseCategories) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
        if (selectedCategoryId == null && state.expenseCategories.isNotEmpty()) {
            selectedCategoryId = state.expenseCategories.first().id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isRefund) "Add Refund" else "Add Expense", style = MaterialTheme.typography.headlineMedium) },
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
            AmountTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                currency = state.primaryCurrency,
                isError = amountError != null,
                supportingText = amountError,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = if (isRefund) "Refund To Account" else "Paid From Account",
            )

            CategoryPickerGrid(
                categories = state.expenseCategories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { selectedCategoryId = it.id },
            )

            DatePickerField(
                dateMs = dateMs,
                onDateSelected = { dateMs = it },
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Payee") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("Payment Method") },
                    placeholder = { Text("UPI, Card, Cash") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Tags", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        state.tags.forEach { tag ->
                            val selected = tag.id in selectedTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedTags = if (selected) selectedTags - tag.id else selectedTags + tag.id
                                },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = isRefund,
                    onCheckedChange = { isRefund = it },
                )
                Text(
                    text = "This is a refund (reduces category spending)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

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
                    val accountId = selectedAccountId ?: return@Button
                    viewModel.addExpense(
                        amountPaise = paise,
                        accountId = accountId,
                        categoryId = selectedCategoryId,
                        dateMs = dateMs,
                        merchant = merchant,
                        location = location,
                        paymentMethod = paymentMethod,
                        description = description,
                        notes = notes,
                        isRefund = isRefund,
                        tagIds = selectedTags.toList(),
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isRefund) "Save Refund" else "Save Expense", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


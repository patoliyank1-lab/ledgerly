package com.finledger.ui.screens.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.BillFrequency
import com.finledger.data.local.entity.BillType
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditBillScreen(
    billId: Long?,
    onBack: () -> Unit,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undoCoordinator = LocalUndoCoordinator.current
    val existing = remember(billId, state.bills) {
        billId?.let { id -> state.bills.firstOrNull { it.id == id } }
    }

    var name by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(BillType.ELECTRICITY) }
    var frequency by remember { mutableStateOf(BillFrequency.MONTHLY) }
    var amountText by remember { mutableStateOf("") }
    var dueDateMs by remember { mutableLongStateOf(System.currentTimeMillis() + 7L * 86_400_000L) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var populatedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(existing) {
        if (existing != null && populatedId != existing.id) {
            name = existing.name
            provider = existing.provider
            type = existing.type
            frequency = existing.frequency
            amountText = MoneyFormatter.paiseToInputString(existing.expectedAmount)
            dueDateMs = existing.nextDueDateMs
            selectedAccountId = existing.paymentAccountId
            selectedCategoryId = existing.categoryId
            notes = existing.notes
            populatedId = existing.id
        }
    }

    LaunchedEffect(state.accounts, state.categories) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
        if (selectedCategoryId == null && state.categories.isNotEmpty()) {
            selectedCategoryId = state.categories.first().id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit Bill" else "Add Bill / Subscription", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteBill(existing.id)
                                undoCoordinator?.showDeleted(existing.name) {
                                    viewModel.restoreBillDirect(existing.id)
                                }
                                onBack()
                            },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete bill")
                        }
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Bill Name") },
                placeholder = { Text("e.g., Fiber Internet, House Rent") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                label = { Text("Provider / Merchant (Optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountTextField(
                value = amountText,
                onValueChange = { amountText = it; errorText = null },
                label = "Expected Amount",
                currency = state.primaryCurrency,
            )

            Text("Bill Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                BillType.entries.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.displayName) })
                }
            }

            Text("Frequency", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                BillFrequency.entries.forEach { f ->
                    FilterChip(selected = frequency == f, onClick = { frequency = f }, label = { Text(f.displayName) })
                }
            }

            DatePickerField(dateMs = dueDateMs, onDateSelected = { dueDateMs = it }, label = "Next Due Date", showQuickChips = false)

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = "Payment Account",
            )

            CategoryPickerGrid(
                categories = state.categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { selectedCategoryId = it.id },
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
                    val paise = MoneyFormatter.parseToPaise(amountText) ?: 0L
                    if (paise <= 0L) {
                        errorText = "Enter a valid positive amount"
                        return@Button
                    }
                    viewModel.saveBill(
                        existing = existing,
                        name = name,
                        provider = provider,
                        type = type,
                        expectedAmountPaise = paise,
                        frequency = frequency,
                        nextDueDateMs = dueDateMs,
                        accountId = selectedAccountId,
                        categoryId = selectedCategoryId,
                        reminderDaysBefore = 3,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Bill") }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


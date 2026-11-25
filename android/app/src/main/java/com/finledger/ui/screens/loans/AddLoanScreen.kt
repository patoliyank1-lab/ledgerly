package com.finledger.ui.screens.loans

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.LoanType
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLoanScreen(
    onBack: () -> Unit,
    viewModel: LoansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var type by remember { mutableStateOf(LoanType.LENT) }
    var personOrOrg by remember { mutableStateOf("") }
    var selectedPersonId by remember { mutableStateOf<Long?>(null) }
    var principalText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dateGivenMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasExpectedDate by remember { mutableStateOf(true) }
    var expectedDateMs by remember { mutableLongStateOf(System.currentTimeMillis() + 30L * 86_400_000L) }
    var repaymentFrequency by remember { mutableStateOf("NONE") }
    var interestRateText by remember { mutableStateOf("") }
    var guarantor by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Record Loan", style = MaterialTheme.typography.headlineMedium) },
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = type == LoanType.LENT,
                    onClick = { type = LoanType.LENT },
                    label = { Text("Lent") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = type == LoanType.BORROWED,
                    onClick = { type = LoanType.BORROWED },
                    label = { Text("Borrowed") },
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.persons.isNotEmpty()) {
                Text("Link to Friend (Optional)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    state.persons.forEach { p ->
                        FilterChip(
                            selected = selectedPersonId == p.id,
                            onClick = {
                                if (selectedPersonId == p.id) {
                                    selectedPersonId = null
                                } else {
                                    selectedPersonId = p.id
                                    personOrOrg = p.name
                                }
                            },
                            label = { Text(p.name) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = personOrOrg,
                onValueChange = {
                    personOrOrg = it
                    val linked = state.persons.firstOrNull { p -> p.id == selectedPersonId }
                    if (linked != null && !linked.name.equals(it.trim(), ignoreCase = true)) {
                        selectedPersonId = null
                    }
                    errorText = null
                },
                label = { Text(if (type == LoanType.LENT) "Borrower Name" else "Lender / Bank / Platform") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            AmountTextField(
                value = principalText,
                onValueChange = { principalText = it; errorText = null },
                label = "Principal Amount",
                currency = state.primaryCurrency,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = if (type == LoanType.LENT) "Paid From Account" else "Deposited To Account",
            )

            DatePickerField(dateMs = dateGivenMs, onDateSelected = { dateGivenMs = it }, label = "Loan Date")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasExpectedDate, onCheckedChange = { hasExpectedDate = it })
                Text("Set Expected Repayment / Due Date", style = MaterialTheme.typography.bodySmall)
            }
            if (hasExpectedDate) {
                DatePickerField(
                    dateMs = expectedDateMs,
                    onDateSelected = { expectedDateMs = it },
                    label = "Expected Repayment / Due Date",
                    showQuickChips = false,
                )
            }

            Text("Repayment Frequency", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                listOf("NONE" to "Single / No schedule", "MONTHLY" to "Monthly", "QUARTERLY" to "Quarterly").forEach { (code, label) ->
                    FilterChip(
                        selected = repaymentFrequency == code || (code == "NONE" && repaymentFrequency == "ONE_TIME"),
                        onClick = { repaymentFrequency = code },
                        label = { Text(label) },
                    )
                }
            }

            OutlinedTextField(
                value = interestRateText,
                onValueChange = { interestRateText = it },
                label = { Text("Annual Interest Rate % (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = guarantor,
                onValueChange = { guarantor = it },
                label = { Text("Guarantor (Optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Terms / Notes") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            (errorText ?: state.errorMessage)?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val principal = MoneyFormatter.parseToPaise(principalText) ?: 0L
                    if (principal <= 0L) {
                        errorText = "Enter a valid positive principal amount"
                        return@Button
                    }
                    viewModel.createLoan(
                        type = type,
                        personOrOrg = personOrOrg,
                        personId = selectedPersonId,
                        principalPaise = principal,
                        dateGivenMs = dateGivenMs,
                        expectedRepaymentDateMs = if (hasExpectedDate) expectedDateMs else null,
                        interestRate = interestRateText.toDoubleOrNull() ?: 0.0,
                        repaymentFrequency = repaymentFrequency,
                        guarantorName = guarantor,
                        accountId = selectedAccountId,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Loan")
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


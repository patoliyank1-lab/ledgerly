package com.finledger.ui.screens.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AccountType
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditAccountSheet(
    existing: AccountEntity?,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: AccountType,
        bank: String,
        masked: String,
        currency: String,
        openingBalancePaise: Long,
        creditLimitPaise: Long?,
        billingDay: Int?,
        dueDay: Int?,
        notes: String,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: AccountType.SAVINGS) }
    var bank by remember { mutableStateOf(existing?.bankOrProvider ?: "") }
    var masked by remember { mutableStateOf(existing?.maskedIdentifier ?: "") }
    var openingText by remember {
        mutableStateOf(
            if (existing != null && existing.openingBalance != 0L) {
                MoneyFormatter.paiseToInputString(existing.openingBalance)
            } else ""
        )
    }
    var limitText by remember {
        mutableStateOf(
            existing?.creditLimit?.let { MoneyFormatter.paiseToInputString(it) } ?: ""
        )
    }
    var billingDayText by remember { mutableStateOf(existing?.billingCycleDay?.toString() ?: "") }
    var dueDayText by remember { mutableStateOf(existing?.dueDay?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = if (existing == null) "Add Account" else "Edit Account",
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Account Name") },
                placeholder = { Text("e.g., HDFC Savings, Cash Wallet") },
                isError = nameError,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Account Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                AccountType.entries.forEach { entry ->
                    FilterChip(
                        selected = type == entry,
                        onClick = { type = entry },
                        label = { Text(entry.displayName, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            AmountTextField(
                value = openingText,
                onValueChange = { openingText = it },
                label = if (type == AccountType.CREDIT_CARD) "Opening Outstanding" else "Opening Balance",
                currency = existing?.currency ?: defaultCurrency,
            )

            OutlinedTextField(
                value = bank,
                onValueChange = { bank = it },
                label = { Text("Bank / Institution / Provider") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = masked,
                onValueChange = { masked = it.take(6) },
                label = { Text("Last 4 Digits (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            if (type == AccountType.CREDIT_CARD) {
                AmountTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = "Credit Limit",
                    currency = existing?.currency ?: defaultCurrency,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = billingDayText,
                        onValueChange = { billingDayText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Statement Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Due Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val opening = MoneyFormatter.parseToPaise(openingText) ?: 0L
                        val limit = MoneyFormatter.parseToPaise(limitText)
                        val bDay = billingDayText.toIntOrNull()?.coerceIn(1, 31)
                        val dDay = dueDayText.toIntOrNull()?.coerceIn(1, 31)
                        onSave(name, type, bank, masked, existing?.currency ?: defaultCurrency, opening, limit, bDay, dDay, notes)
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


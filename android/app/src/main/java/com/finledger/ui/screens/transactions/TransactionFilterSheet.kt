package com.finledger.ui.screens.transactions

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.TransactionFilter
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFilterSheet(
    currentFilter: TransactionFilter,
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onApply: (TransactionFilter) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(currentFilter.type) }
    var selectedAccountId by remember { mutableStateOf(currentFilter.accountId) }
    var selectedCategoryId by remember { mutableStateOf(currentFilter.categoryId) }
    var startDateMs by remember { mutableStateOf(currentFilter.startDateMs) }
    var endDateMs by remember { mutableStateOf(currentFilter.endDateMs) }
    var dateError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("Filter Transactions", style = MaterialTheme.typography.headlineMedium)

            Text("Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All") },
                )
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = if (selectedType == type) null else type },
                        label = { Text(type.displayName) },
                    )
                }
            }

            Text("Account", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(
                    selected = selectedAccountId == null,
                    onClick = { selectedAccountId = null },
                    label = { Text("All Accounts") },
                )
                accounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.account.id,
                        onClick = {
                            selectedAccountId = if (selectedAccountId == acc.account.id) null else acc.account.id
                        },
                        label = { Text(acc.account.name) },
                    )
                }
            }

            Text("Category", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("All Categories") },
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                        },
                        label = { Text(cat.name) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Date Range", style = MaterialTheme.typography.titleSmall)
                if (startDateMs != null || endDateMs != null) {
                    TextButton(onClick = {
                        startDateMs = null
                        endDateMs = null
                        dateError = null
                    }) { Text("Clear") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DatePickerField(
                    dateMs = startDateMs ?: (System.currentTimeMillis() - 30L * 86_400_000L),
                    onDateSelected = {
                        startDateMs = startOfDay(it)
                        dateError = null
                    },
                    label = if (startDateMs == null) "From (Any)" else "From",
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    dateMs = endDateMs ?: System.currentTimeMillis(),
                    onDateSelected = {
                        endDateMs = endOfDay(it)
                        dateError = null
                    },
                    label = if (endDateMs == null) "To (Any)" else "To",
                    modifier = Modifier.weight(1f),
                )
            }
            dateError?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = {
                        onApply(TransactionFilter(searchQuery = currentFilter.searchQuery))
                        onDismiss()
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        val s = startDateMs
                        val e = endDateMs
                        if (s != null && e != null && s > e) {
                            dateError = "From date cannot be after To date"
                            return@Button
                        }
                        onApply(
                            currentFilter.copy(
                                type = selectedType,
                                accountId = selectedAccountId,
                                categoryId = selectedCategoryId,
                                startDateMs = s,
                                endDateMs = e,
                            )
                        )
                        onDismiss()
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

private fun startOfDay(epochMs: Long): Long = Calendar.getInstance().apply {
    timeInMillis = epochMs
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfDay(epochMs: Long): Long = Calendar.getInstance().apply {
    timeInMillis = epochMs
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis


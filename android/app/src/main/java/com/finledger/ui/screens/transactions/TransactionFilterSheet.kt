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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.finledger.ui.theme.Spacing

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
                listOf(
                    TransactionType.EXPENSE,
                    TransactionType.INCOME,
                    TransactionType.TRANSFER,
                    TransactionType.REFUND,
                    TransactionType.LENDING,
                    TransactionType.BORROWING,
                ).forEach { type ->
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

            Text("Date Range", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DatePickerField(
                    dateMs = startDateMs ?: (System.currentTimeMillis() - 30L * 86_400_000L),
                    onDateSelected = { startDateMs = it },
                    label = "From",
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    dateMs = endDateMs ?: System.currentTimeMillis(),
                    onDateSelected = { endDateMs = it },
                    label = "To",
                    modifier = Modifier.weight(1f),
                )
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
                        onApply(
                            currentFilter.copy(
                                type = selectedType,
                                accountId = selectedAccountId,
                                categoryId = selectedCategoryId,
                                startDateMs = startDateMs,
                                endDateMs = endDateMs,
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


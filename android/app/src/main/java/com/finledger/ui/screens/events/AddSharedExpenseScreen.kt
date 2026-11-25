package com.finledger.ui.screens.events

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.ExpensePayerEntity
import com.finledger.data.local.entity.ExpenseSplitEntity
import com.finledger.data.local.entity.PersonEntity
import com.finledger.data.local.entity.SharedExpenseItemEntity
import com.finledger.data.local.entity.SplitMethod
import com.finledger.domain.usecase.ItemSplitInput
import com.finledger.domain.usecase.SplitCalculator
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSharedExpenseScreen(
    eventId: Long?,
    groupId: Long?,
    onBack: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var splitMethod by remember { mutableStateOf(SplitMethod.EQUAL) }
    var multiPayerMode by remember { mutableStateOf(false) }
    var singlePayerId by remember { mutableLongStateOf(SplitCalculator.USER_PERSON_ID) }
    val payerAmounts = remember { mutableStateMapOf<Long, String>() }
    val customInputs = remember { mutableStateMapOf<Long, String>() }
    val includedParticipants = remember { mutableStateMapOf<Long, Boolean>(SplitCalculator.USER_PERSON_ID to true) }
    var items by remember { mutableStateOf(listOf(ItemSplitInput("Item 1", 0L, listOf(SplitCalculator.USER_PERSON_ID)))) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts, state.categories, state.persons) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
        if (selectedCategoryId == null && state.categories.isNotEmpty()) {
            selectedCategoryId = state.categories.first().id
        }
        state.persons.forEach { p ->
            if (!includedParticipants.containsKey(p.id)) {
                includedParticipants[p.id] = true
            }
        }
    }

    val allPeople = remember(state.persons) {
        listOf(PersonEntity(id = SplitCalculator.USER_PERSON_ID, name = "You")) + state.persons
    }
    val activeParticipantIds = allPeople.map { it.id }.filter { includedParticipants[it] == true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Add Shared Expense", style = MaterialTheme.typography.headlineMedium) },
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
                onValueChange = { amountText = it; errorText = null },
                label = "Total Expense Amount",
                currency = state.primaryCurrency,
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; errorText = null },
                label = { Text("What was this for?") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            DatePickerField(dateMs = dateMs, onDateSelected = { dateMs = it })

            CategoryPickerGrid(
                categories = state.categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { selectedCategoryId = it.id },
            )

            // Payer section
            Text("Who Paid?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = !multiPayerMode,
                    onClick = { multiPayerMode = false },
                    label = { Text("Single Payer") },
                )
                FilterChip(
                    selected = multiPayerMode,
                    onClick = { multiPayerMode = true },
                    label = { Text("Multiple Payers") },
                )
            }

            if (!multiPayerMode) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    allPeople.forEach { p ->
                        FilterChip(
                            selected = singlePayerId == p.id,
                            onClick = { singlePayerId = p.id },
                            label = { Text(p.name) },
                        )
                    }
                }
            } else {
                allPeople.forEach { p ->
                    AmountTextField(
                        value = payerAmounts[p.id] ?: "",
                        onValueChange = { payerAmounts[p.id] = it },
                        label = "${p.name} paid",
                        currency = state.primaryCurrency,
                    )
                }
            }

            val userIsPaying = (!multiPayerMode && singlePayerId == SplitCalculator.USER_PERSON_ID) ||
                (multiPayerMode && (MoneyFormatter.parseToPaise(payerAmounts[SplitCalculator.USER_PERSON_ID].orEmpty()) ?: 0L) > 0L)

            if (userIsPaying) {
                AccountPickerDropdown(
                    accounts = state.accounts,
                    selectedAccountId = selectedAccountId,
                    onSelectAccount = { selectedAccountId = it.account.id },
                    label = "Your Payment Account",
                )
            }

            // Participants (exclude toggle)
            Text("Participants in Split", style = MaterialTheme.typography.titleSmall)
            allPeople.forEach { p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = includedParticipants[p.id] == true,
                        onCheckedChange = { includedParticipants[p.id] = it },
                    )
                    Text(p.name, style = MaterialTheme.typography.bodyMedium)
                }
            }

            SplitMethodSelector(
                selected = splitMethod,
                onSelect = { splitMethod = it },
            )

            when (splitMethod) {
                SplitMethod.EQUAL -> {
                    val total = MoneyFormatter.parseToPaise(amountText) ?: 0L
                    val perPerson = if (activeParticipantIds.isNotEmpty()) total / activeParticipantIds.size else 0L
                    Text(
                        text = "Each of ${activeParticipantIds.size} people pays ${MoneyFormatter.format(perPerson, state.primaryCurrency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SplitMethod.CUSTOM_AMOUNT, SplitMethod.PERCENTAGE, SplitMethod.SHARES -> {
                    val unitLabel = when (splitMethod) {
                        SplitMethod.CUSTOM_AMOUNT -> "Amount"
                        SplitMethod.PERCENTAGE -> "Percent (%)"
                        else -> "Weight / Shares"
                    }
                    allPeople.filter { it.id in activeParticipantIds }.forEach { p ->
                        OutlinedTextField(
                            value = customInputs[p.id] ?: "",
                            onValueChange = { customInputs[p.id] = it },
                            label = { Text("${p.name} — $unitLabel") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                SplitMethod.ITEM -> {
                    ItemSplitEntry(
                        items = items,
                        people = allPeople.filter { it.id in activeParticipantIds },
                        currency = state.primaryCurrency,
                        onItemsChange = { items = it },
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

            (errorText ?: state.errorMessage)?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val totalPaise = if (splitMethod == SplitMethod.ITEM) {
                        items.sumOf { it.amountPaise }
                    } else {
                        MoneyFormatter.parseToPaise(amountText) ?: 0L
                    }
                    if (totalPaise <= 0L) {
                        errorText = "Enter a valid positive total amount"
                        return@Button
                    }
                    val payers = if (!multiPayerMode) {
                        listOf(ExpensePayerEntity(0L, singlePayerId, totalPaise))
                    } else {
                        allPeople.mapNotNull { p ->
                            val amt = MoneyFormatter.parseToPaise(payerAmounts[p.id].orEmpty()) ?: 0L
                            if (amt > 0L) ExpensePayerEntity(0L, p.id, amt) else null
                        }
                    }

                    val splitAmounts: Map<Long, Long> = when (splitMethod) {
                        SplitMethod.EQUAL -> SplitCalculator.splitEqually(totalPaise, activeParticipantIds)
                        SplitMethod.CUSTOM_AMOUNT -> activeParticipantIds.associateWith { id ->
                            MoneyFormatter.parseToPaise(customInputs[id].orEmpty()) ?: 0L
                        }
                        SplitMethod.PERCENTAGE -> SplitCalculator.splitByPercentages(
                            totalPaise,
                            activeParticipantIds.associateWith { id -> customInputs[id]?.toDoubleOrNull() ?: 0.0 },
                        )
                        SplitMethod.SHARES -> SplitCalculator.splitByWeights(
                            totalPaise,
                            activeParticipantIds.associateWith { id -> customInputs[id]?.toDoubleOrNull() ?: 1.0 },
                        )
                        SplitMethod.ITEM -> SplitCalculator.splitByItems(items)
                    }

                    val splitEntities = splitAmounts.map { (personId, share) ->
                        ExpenseSplitEntity(
                            sharedExpenseId = 0L,
                            personId = personId,
                            shareAmount = share,
                        )
                    }
                    val itemEntities = if (splitMethod == SplitMethod.ITEM) {
                        items.map { item ->
                            SharedExpenseItemEntity(
                                sharedExpenseId = 0L,
                                itemName = item.itemName,
                                amount = item.amountPaise,
                                assignedPersonIdsCsv = item.assignedPersonIds.joinToString(","),
                            )
                        }
                    } else emptyList()

                    viewModel.saveSharedExpense(
                        totalAmountPaise = totalPaise,
                        description = description,
                        dateMs = dateMs,
                        categoryId = selectedCategoryId,
                        groupId = groupId?.takeIf { it > 0L },
                        eventId = eventId?.takeIf { it > 0L },
                        splitMethod = splitMethod,
                        payers = payers,
                        userAccountIdIfUserPaid = selectedAccountId,
                        splits = splitEntities,
                        items = itemEntities,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Shared Expense")
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitMethodSelector(
    selected: SplitMethod,
    onSelect: (SplitMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Split Method", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SplitMethod.entries.forEach { method ->
                FilterChip(
                    selected = selected == method,
                    onClick = { onSelect(method) },
                    label = { Text(method.displayName) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemSplitEntry(
    items: List<ItemSplitInput>,
    people: List<PersonEntity>,
    currency: String,
    onItemsChange: (List<ItemSplitInput>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items.forEachIndexed { index, item ->
            var amtText by remember(index, items.size) {
                mutableStateOf(if (item.amountPaise > 0) MoneyFormatter.paiseToInputString(item.amountPaise) else "")
            }
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(
                        value = item.itemName,
                        onValueChange = { newName ->
                            onItemsChange(items.toMutableList().apply { this[index] = item.copy(itemName = newName) })
                        },
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AmountTextField(
                        value = amtText,
                        onValueChange = { newAmt ->
                            amtText = newAmt
                            val p = MoneyFormatter.parseToPaise(newAmt) ?: 0L
                            onItemsChange(items.toMutableList().apply { this[index] = item.copy(amountPaise = p) })
                        },
                        label = "Item Amount",
                        currency = currency,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        people.forEach { person ->
                            val assigned = person.id in item.assignedPersonIds
                            FilterChip(
                                selected = assigned,
                                onClick = {
                                    val updatedList = if (assigned) {
                                        item.assignedPersonIds - person.id
                                    } else {
                                        item.assignedPersonIds + person.id
                                    }
                                    onItemsChange(items.toMutableList().apply { this[index] = item.copy(assignedPersonIds = updatedList) })
                                },
                                label = { Text(person.name) },
                            )
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                onItemsChange(items + ItemSplitInput("Item ${items.size + 1}", 0L, people.map { it.id }))
            },
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text("+ Add Item")
        }
    }
}


package com.finledger.ui.screens.friends

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.finledger.data.local.entity.SettlementDirection
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordSettlementScreen(
    initialPersonId: Long?,
    initialGroupId: Long? = null,
    initialEventId: Long? = null,
    initialDirection: SettlementDirection? = null,
    initialAmountPaise: Long? = null,
    onBack: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPersonId by remember { mutableStateOf(initialPersonId?.takeIf { it > 0L }) }
    var selectedGroupId by remember { mutableStateOf(initialGroupId?.takeIf { it > 0L }) }
    var selectedEventId by remember { mutableStateOf(initialEventId?.takeIf { it > 0L }) }
    var direction by remember { mutableStateOf(initialDirection ?: SettlementDirection.RECEIVED) }
    var amountText by remember {
        mutableStateOf(
            if (initialAmountPaise != null && initialAmountPaise > 0L) {
                MoneyFormatter.paiseToInputString(initialAmountPaise)
            } else ""
        )
    }
    var userEditedAmountOrDirection by remember {
        mutableStateOf(initialDirection != null || (initialAmountPaise != null && initialAmountPaise > 0L))
    }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var paymentMethod by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var personExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.balances, state.accounts) {
        if (selectedPersonId == null && state.balances.isNotEmpty()) {
            selectedPersonId = state.balances.first().person.id
        }
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
    }

    val selectedPerson = state.balances.firstOrNull { it.person.id == selectedPersonId }

    LaunchedEffect(selectedPerson?.person?.id, selectedPerson?.netBalancePaise) {
        if (!userEditedAmountOrDirection && selectedPerson != null) {
            val net = selectedPerson.netBalancePaise
            if (net < 0L) {
                direction = SettlementDirection.PAID
                amountText = MoneyFormatter.paiseToInputString(-net)
            } else if (net > 0L) {
                direction = SettlementDirection.RECEIVED
                amountText = MoneyFormatter.paiseToInputString(net)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Record Settlement", style = MaterialTheme.typography.headlineMedium) },
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
            ExposedDropdownMenuBox(
                expanded = personExpanded,
                onExpandedChange = { personExpanded = !personExpanded },
            ) {
                OutlinedTextField(
                    value = selectedPerson?.person?.name ?: "Select Person",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Person") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = personExpanded) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = personExpanded,
                    onDismissRequest = { personExpanded = false },
                ) {
                    state.balances.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b.person.name) },
                            onClick = {
                                selectedPersonId = b.person.id
                                userEditedAmountOrDirection = false
                                personExpanded = false
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = direction == SettlementDirection.RECEIVED,
                    onClick = {
                        direction = SettlementDirection.RECEIVED
                        userEditedAmountOrDirection = true
                    },
                    label = { Text("They Paid You") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = direction == SettlementDirection.PAID,
                    onClick = {
                        direction = SettlementDirection.PAID
                        userEditedAmountOrDirection = true
                    },
                    label = { Text("You Paid Them") },
                    modifier = Modifier.weight(1f),
                )
            }

            AmountTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    userEditedAmountOrDirection = true
                    errorText = null
                },
                label = "Settlement Amount",
                currency = state.primaryCurrency,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = "Payment Account",
            )

            if (state.groups.isNotEmpty()) {
                Text("Group (Optional)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FilterChip(
                        selected = selectedGroupId == null,
                        onClick = { selectedGroupId = null },
                        label = { Text("None") },
                    )
                    state.groups.forEach { g ->
                        FilterChip(
                            selected = selectedGroupId == g.id,
                            onClick = { selectedGroupId = g.id },
                            label = { Text(g.name) },
                        )
                    }
                }
            }

            val scopedEvents = if (selectedGroupId != null) {
                state.events.filter { it.groupId == selectedGroupId }
            } else state.events
            if (scopedEvents.isNotEmpty()) {
                Text("Event (Optional)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FilterChip(
                        selected = selectedEventId == null,
                        onClick = { selectedEventId = null },
                        label = { Text("None") },
                    )
                    scopedEvents.forEach { ev ->
                        FilterChip(
                            selected = selectedEventId == ev.id,
                            onClick = {
                                selectedEventId = ev.id
                                if (ev.groupId != null) selectedGroupId = ev.groupId
                            },
                            label = { Text(ev.name) },
                        )
                    }
                }
            }

            DatePickerField(dateMs = dateMs, onDateSelected = { dateMs = it })

            Text("Payment Method", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                listOf("UPI", "Cash", "Bank Transfer", "Card").forEach { method ->
                    FilterChip(
                        selected = paymentMethod.equals(method, ignoreCase = true),
                        onClick = { paymentMethod = method },
                        label = { Text(method) },
                    )
                }
            }
            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment Method (UPI, Cash, Bank)") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference / UTR (Optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
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
                    val pid = selectedPersonId ?: run {
                        errorText = "Select a person first"
                        return@Button
                    }
                    val paise = MoneyFormatter.parseToPaise(amountText) ?: 0L
                    if (paise <= 0L) {
                        errorText = "Enter a valid positive amount"
                        return@Button
                    }
                    viewModel.recordSettlement(
                        personId = pid,
                        amountPaise = paise,
                        direction = direction,
                        dateMs = dateMs,
                        accountId = selectedAccountId,
                        paymentMethod = paymentMethod,
                        referenceNumber = reference,
                        notes = notes,
                        groupId = selectedGroupId,
                        eventId = selectedEventId,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Settlement")
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


package com.finledger.ui.screens.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.SettlementDirection
import com.finledger.data.local.entity.SettlementEntity
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

/**
 * Formats an explicit outstanding-balance warning for an unsettled contact, or null when settled (`0L`).
 */
fun formatPersonBalanceWarning(
    personName: String,
    netBalancePaise: Long,
    currency: String,
): String? {
    val label = personName.ifBlank { "Person" }
    return when {
        netBalancePaise > 0L -> "$label owes you ${MoneyFormatter.format(netBalancePaise, currency)}"
        netBalancePaise < 0L -> "You owe $label ${MoneyFormatter.format(-netBalancePaise, currency)}"
        else -> null
    }
}

@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onSettleUp: (Long) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val person by viewModel.observePerson(personId).collectAsStateWithLifecycle()
    val ledger by viewModel.observeLedger(personId).collectAsStateWithLifecycle()
    val settlements by viewModel.observeSettlements(personId).collectAsStateWithLifecycle()
    val summary = state.balances.firstOrNull { it.person.id == personId }
    var showEditSheet by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var inspectingSettlement by remember { mutableStateOf<SettlementEntity?>(null) }
    val undoCoordinator = LocalUndoCoordinator.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(person?.name ?: "Person Ledger", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (person != null) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit person")
                        }
                        IconButton(onClick = { showArchiveConfirm = true }) {
                            Icon(Icons.Outlined.Archive, contentDescription = "Archive person")
                        }
                        IconButton(
                            onClick = {
                                val currentPerson = person ?: return@IconButton
                                val netBalance = summary?.netBalancePaise ?: 0L
                                if (netBalance != 0L) {
                                    showDeleteConfirm = true
                                } else {
                                    viewModel.deletePerson(personId) {
                                        undoCoordinator?.showDeleted(currentPerson.name) {
                                            viewModel.restorePersonDirect(personId)
                                        }
                                        onBack()
                                    }
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete person")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                val net = summary?.netBalancePaise ?: 0L
                val statusText = formatPersonBalanceWarning(
                    personName = person?.name ?: "Person",
                    netBalancePaise = net,
                    currency = state.primaryCurrency,
                ) ?: "All settled up"
                Surface(
                    color = FinLedgerTheme.colors.tileDark1,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = person?.relationship ?: "Contact",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        if (net != 0L) {
                            val fullAmount = kotlin.math.abs(net)
                            val settleDirection = if (net > 0L) {
                                SettlementDirection.RECEIVED
                            } else {
                                SettlementDirection.PAID
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.quickSettle(
                                            personId = personId,
                                            amountPaise = fullAmount,
                                            direction = settleDirection,
                                            onRecorded = { settlementId ->
                                                undoCoordinator?.showUndo("Settlement recorded") {
                                                    viewModel.deleteSettlementByIdDirect(settlementId)
                                                }
                                            },
                                            onNoAccount = { onSettleUp(personId) },
                                        )
                                    },
                                    shape = MaterialTheme.shapes.extraLarge,
                                ) {
                                    Text("Settle full amount (${MoneyFormatter.format(fullAmount, state.primaryCurrency)})")
                                }
                                TextButton(onClick = { onSettleUp(personId) }) {
                                    Text("Edit / Partial", color = FinLedgerTheme.colors.skyLinkBlue)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onSettleUp(personId) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Text("Record Settlement")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Consolidated Ledger",
                    style = MaterialTheme.typography.titleSmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
            }

            if (ledger.isEmpty()) {
                item {
                    Text(
                        text = "No shared expenses or settlements with this person yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                }
            } else {
                items(ledger, key = { it.id }) { entry ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${DateFormatter.formatDate(entry.dateMs)} · ${entry.subtitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                            Text(
                                text = MoneyFormatter.format(entry.deltaPaise, state.primaryCurrency, showPlus = true),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (entry.deltaPaise >= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
                            )
                        }
                    }
                }
            }

            if (settlements.isNotEmpty()) {
                item {
                    Text(
                        text = "Settlement History",
                        style = MaterialTheme.typography.titleSmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                    )
                }
                items(settlements, key = { "sh_${it.id}" }) { s ->
                    SettlementHistoryRow(
                        settlement = s,
                        currency = state.primaryCurrency,
                        onClick = { inspectingSettlement = s },
                    )
                }
            }
        }
    }

    if (showEditSheet && person != null) {
        AddEditPersonSheet(
            existing = person,
            onDismiss = { showEditSheet = false },
            onSave = { name, phone, email, rel, notes ->
                viewModel.savePerson(person, name, phone, email, rel, notes) {
                    showEditSheet = false
                }
            },
        )
    }

    if (showArchiveConfirm && person != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive ${person!!.name}?") },
            text = { Text("Archived contacts are hidden from active lists while keeping all ledger history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirm = false
                        viewModel.archivePerson(personId, onDone = onBack)
                    },
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm && person != null) {
        val currentPerson = person!!
        val net = summary?.netBalancePaise ?: 0L
        val balanceText = formatPersonBalanceWarning(
            personName = currentPerson.name,
            netBalancePaise = net,
            currency = state.primaryCurrency,
        )
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${currentPerson.name}?") },
            text = {
                Text(
                    if (balanceText != null) {
                        "$balanceText. Deleting hides this contact and balance from active lists."
                    } else {
                        "This contact will be moved to Trash."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deletePerson(personId) {
                            undoCoordinator?.showDeleted(currentPerson.name) {
                                viewModel.restorePersonDirect(personId)
                            }
                            onBack()
                        }
                    },
                ) { Text("Delete", color = FinLedgerTheme.colors.negative) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    inspectingSettlement?.let { st ->
        AlertDialog(
            onDismissRequest = { inspectingSettlement = null },
            title = { Text(if (st.direction == SettlementDirection.RECEIVED) "Settlement Received" else "Settlement Paid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(MoneyFormatter.format(st.amount, state.primaryCurrency), style = MaterialTheme.typography.headlineMedium)
                    Text("Date: ${DateFormatter.formatDate(st.date)}", style = MaterialTheme.typography.bodySmall)
                    if (st.paymentMethod.isNotBlank()) Text("Method: ${st.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                    if (st.referenceNumber.isNotBlank()) Text("Ref: ${st.referenceNumber}", style = MaterialTheme.typography.bodySmall)
                    if (st.notes.isNotBlank()) Text("Notes: ${st.notes}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSettlement(st)
                        undoCoordinator?.showDeleted("settlement") {
                            viewModel.restoreSettlementDirect(st)
                        }
                        inspectingSettlement = null
                    },
                ) { Text("Delete", color = FinLedgerTheme.colors.negative) }
            },
            dismissButton = {
                TextButton(onClick = { inspectingSettlement = null }) { Text("Close") }
            },
        )
    }
}

@Composable
fun SettlementHistoryRow(
    settlement: SettlementEntity,
    currency: String,
    onClick: () -> Unit = {},
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (settlement.direction == SettlementDirection.RECEIVED) "Money Received" else "Money Paid",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${DateFormatter.formatDate(settlement.date)} · ${settlement.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
            Text(
                text = MoneyFormatter.format(settlement.amount, currency),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


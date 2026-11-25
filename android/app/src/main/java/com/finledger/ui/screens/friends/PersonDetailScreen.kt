package com.finledger.ui.screens.friends

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                val statusText = when {
                    net > 0 -> "${person?.name ?: "Person"} owes you ${MoneyFormatter.format(net, state.primaryCurrency)}"
                    net < 0 -> "You owe ${person?.name ?: "Person"} ${MoneyFormatter.format(-net, state.primaryCurrency)}"
                    else -> "All settled up"
                }
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
                        Button(
                            onClick = { onSettleUp(personId) },
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text("Record Settlement")
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
                    SettlementHistoryRow(s, state.primaryCurrency)
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
}

@Composable
fun SettlementHistoryRow(settlement: SettlementEntity, currency: String) {
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


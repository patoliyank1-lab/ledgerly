package com.finledger.ui.screens.events

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun EventDetailScreen(
    eventId: Long,
    onBack: () -> Unit,
    onAddSharedExpense: (eventId: Long, groupId: Long?) -> Unit,
    onOpenSummary: (Long) -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val detail by viewModel.observeEventDetail(eventId).collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(detail.event?.name ?: "Event", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddSharedExpense(eventId, detail.event?.groupId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add shared expense")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Surface(
                    color = FinLedgerTheme.colors.tileDark1,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = "Event Total Spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Text(
                            text = MoneyFormatter.format(detail.totalSpentPaise, detail.primaryCurrency),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = "Your personal share: ${MoneyFormatter.format(detail.userPersonalSpendPaise, detail.primaryCurrency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.skyLinkBlue,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Button(
                                onClick = { onAddSharedExpense(eventId, detail.event?.groupId) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Add Expense") }
                            OutlinedButton(
                                onClick = { onOpenSummary(eventId) },
                                shape = MaterialTheme.shapes.extraLarge,
                                border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                            ) { Text("Summary & Settle", color = FinLedgerTheme.colors.skyLinkBlue) }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Shared Expenses",
                    style = MaterialTheme.typography.titleSmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
            }

            items(detail.expenses, key = { it.id }) { exp ->
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
                            Text(exp.description, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${DateFormatter.formatDate(exp.date)} · ${exp.splitMethod.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(exp.totalAmount, exp.currency),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventSummaryScreen(
    eventId: Long,
    onBack: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val detail by viewModel.observeEventDetail(eventId).collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Event Summary", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(detail.event?.name ?: "Event", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Event Total: ${MoneyFormatter.format(detail.totalSpentPaise, detail.primaryCurrency)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Your Personal Share: ${MoneyFormatter.format(detail.userPersonalSpendPaise, detail.primaryCurrency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (detail.event?.status == com.finledger.data.local.entity.EventStatus.OPEN) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            OutlinedButton(
                                onClick = { viewModel.closeEvent(eventId) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Close Event") }
                        }
                    }
                }
            }

            item {
                Text("Participant Balances", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
            }

            items(detail.memberNets.entries.toList(), key = { it.key }) { (personId, net) ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(viewModel.personName(personId, detail.participants), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = when {
                                net > 0 -> "gets back ${MoneyFormatter.format(net, detail.primaryCurrency)}"
                                net < 0 -> "owes ${MoneyFormatter.format(-net, detail.primaryCurrency)}"
                                else -> "settled"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (net >= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
                        )
                    }
                }
            }

            if (detail.suggestions.isNotEmpty()) {
                item {
                    Text("Suggested Settlement Plan", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                }
                items(detail.suggestions) { s ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${viewModel.personName(s.fromPersonId, detail.participants)} → ${viewModel.personName(s.toPersonId, detail.participants)}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = MoneyFormatter.format(s.amountPaise, detail.primaryCurrency),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}


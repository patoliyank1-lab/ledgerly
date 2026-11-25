package com.finledger.ui.screens.groups

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun GroupDetailScreen(
    groupId: Long,
    onBack: () -> Unit,
    onEditGroup: (Long) -> Unit,
    onAddEvent: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onAddSharedExpense: (Long) -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val detail by viewModel.observeGroupDetail(groupId).collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(detail.group?.name ?: "Group", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditGroup(groupId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit group")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddSharedExpense(groupId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add group expense")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Surface(
                color = FinLedgerTheme.colors.tileDark1,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text("Total Group Spend", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                    Text(
                        text = MoneyFormatter.format(detail.totalSpent, detail.primaryCurrency),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Button(
                            onClick = { onAddSharedExpense(groupId) },
                            shape = MaterialTheme.shapes.extraLarge,
                        ) { Text("Add Expense") }
                        OutlinedButton(
                            onClick = { onAddEvent(groupId) },
                            shape = MaterialTheme.shapes.extraLarge,
                            border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                        ) { Text("New Event", color = FinLedgerTheme.colors.skyLinkBlue) }
                    }
                }
            }

            TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Expenses (${detail.expenses.size})") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Balances") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Events (${detail.events.size})") })
            }

            when (tabIndex) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        items(detail.expenses, key = { it.id }) { exp ->
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

                1 -> {
                    GroupMemberBalancesTab(
                        detail = detail,
                        personName = { id -> viewModel.personName(id, detail.members) },
                        modifier = Modifier.weight(1f),
                    )
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        items(detail.events, key = { it.id }) { ev ->
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenEvent(ev.id) },
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Text(ev.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${DateFormatter.formatDate(ev.startDate)} · ${ev.status.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FinLedgerTheme.colors.inkMuted48,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupMemberBalancesTab(
    detail: GroupDetailUiState,
    personName: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        item {
            Text(
                text = "Net Group Positions",
                style = MaterialTheme.typography.titleSmall,
                color = FinLedgerTheme.colors.inkMuted48,
            )
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
                    Text(personName(personId), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = when {
                            net > 0 -> "gets back ${MoneyFormatter.format(net, detail.primaryCurrency)}"
                            net < 0 -> "owes ${MoneyFormatter.format(-net, detail.primaryCurrency)}"
                            else -> "settled"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            net > 0 -> FinLedgerTheme.colors.positive
                            net < 0 -> FinLedgerTheme.colors.negative
                            else -> FinLedgerTheme.colors.inkMuted48
                        },
                    )
                }
            }
        }

        if (detail.suggestions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Suggested Settlement Plan",
                    style = MaterialTheme.typography.titleSmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
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
                            text = "${personName(s.fromPersonId)} → ${personName(s.toPersonId)}",
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


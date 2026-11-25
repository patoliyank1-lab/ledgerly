package com.finledger.ui.screens.data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Trash & Recovery", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.deletedTransactions.isEmpty() && state.deletedAccounts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.DeleteOutline,
                title = "Trash is empty",
                subtitle = "Deleted transactions and accounts appear here so you can recover them safely.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(state.deletedTransactions, key = { "tx_${it.id}" }) { tx ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.merchantOrPayee.ifBlank { tx.description.ifBlank { tx.type.displayName } }, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${DateFormatter.formatDate(tx.date)} · ${MoneyFormatter.format(tx.amount, tx.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.restoreTransaction(tx.id) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Restore") }
                        }
                    }
                }
                items(state.deletedAccounts, key = { "acc_${it.id}" }) { acc ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, style = MaterialTheme.typography.bodyLarge)
                                Text(acc.type.displayName, style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                            }
                            OutlinedButton(
                                onClick = { viewModel.restoreAccount(acc.id) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Restore") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditHistoryScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Activity History", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.auditLogs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.History,
                title = "No audit events yet",
                subtitle = "Every create, edit, delete, and restore action is logged for full transparency.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(state.auditLogs, key = { it.id }) { log ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
                            Text("${log.action} · ${log.entityType} #${log.entityId}", style = MaterialTheme.typography.bodyLarge)
                            if (log.fieldChanged.isNotBlank() || log.newValue.isNotBlank()) {
                                Text(
                                    text = listOf(log.fieldChanged, log.newValue).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted80,
                                )
                            }
                            Text(
                                text = "${DateFormatter.formatDate(log.performedAt)} ${DateFormatter.formatTime(log.performedAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Archived Records", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.archivedAccounts.isEmpty() && state.archivedGroups.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Archive,
                title = "No archived records",
                subtitle = "Archived accounts and closed groups appear here.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(state.archivedAccounts, key = { "a_${it.id}" }) { acc ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${acc.name} (${acc.type.displayName})", style = MaterialTheme.typography.bodyLarge)
                            OutlinedButton(
                                onClick = { viewModel.unarchiveAccount(acc.id) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Unarchive") }
                        }
                    }
                }
                items(state.archivedGroups, key = { "g_${it.id}" }) { grp ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(grp.name, style = MaterialTheme.typography.bodyLarge)
                            OutlinedButton(
                                onClick = { viewModel.unarchiveGroup(grp.id) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Reopen") }
                        }
                    }
                }
            }
        }
    }
}


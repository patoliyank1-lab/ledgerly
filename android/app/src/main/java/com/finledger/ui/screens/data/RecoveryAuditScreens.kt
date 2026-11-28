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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun RecoveryScreen(
    initialTab: Int = 0,
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    val trashCount = state.deletedTransactions.size + state.deletedAccounts.size
    val archiveCount = state.archivedAccounts.size + state.archivedGroups.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Recovery", style = MaterialTheme.typography.headlineMedium) },
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
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (trashCount > 0) "Trash ($trashCount)" else "Trash") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (archiveCount > 0) "Archive ($archiveCount)" else "Archive") },
                )
            }
            if (selectedTab == 0) {
                TrashContent(
                    state = state,
                    onRestoreTransaction = viewModel::restoreTransaction,
                    onRestoreAccount = viewModel::restoreAccount,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ArchiveContent(
                    state = state,
                    onUnarchiveAccount = viewModel::unarchiveAccount,
                    onReopenGroup = viewModel::unarchiveGroup,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    RecoveryScreen(initialTab = 0, onBack = onBack, viewModel = viewModel)
}

@Composable
private fun TrashContent(
    state: DataManagementUiState,
    onRestoreTransaction: (Long) -> Unit,
    onRestoreAccount: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.deletedTransactions.isEmpty() && state.deletedAccounts.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.DeleteOutline,
            title = "Trash is empty",
            subtitle = "Deleted transactions and accounts appear here so you can recover them safely.",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            items(state.deletedTransactions, key = { "tx_${it.id}" }) { tx ->
                RecoveryRowCard(
                    title = tx.merchantOrPayee.ifBlank { tx.description.ifBlank { tx.type.displayName } },
                    subtitle = "${DateFormatter.formatDate(tx.date)} · ${MoneyFormatter.format(tx.amount, tx.currency)}",
                    actionLabel = "Restore",
                    onAction = { onRestoreTransaction(tx.id) },
                )
            }
            items(state.deletedAccounts, key = { "acc_${it.id}" }) { acc ->
                RecoveryRowCard(
                    title = acc.name,
                    subtitle = acc.type.displayName,
                    actionLabel = "Restore",
                    onAction = { onRestoreAccount(acc.id) },
                )
            }
        }
    }
}

@Composable
private fun RecoveryRowCard(
    title: String,
    subtitle: String?,
    actionLabel: String,
    onAction: () -> Unit,
) {
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
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                    )
                }
            }
            OutlinedButton(
                onClick = onAction,
                shape = MaterialTheme.shapes.extraLarge,
            ) { Text(actionLabel) }
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
                title = { Text("Change Log", style = MaterialTheme.typography.headlineMedium) },
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
    RecoveryScreen(initialTab = 1, onBack = onBack, viewModel = viewModel)
}

@Composable
private fun ArchiveContent(
    state: DataManagementUiState,
    onUnarchiveAccount: (Long) -> Unit,
    onReopenGroup: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.archivedAccounts.isEmpty() && state.archivedGroups.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.Archive,
            title = "No archived records",
            subtitle = "Archived accounts and closed groups appear here.",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            items(state.archivedAccounts, key = { "a_${it.id}" }) { acc ->
                RecoveryRowCard(
                    title = acc.name,
                    subtitle = acc.type.displayName,
                    actionLabel = "Unarchive",
                    onAction = { onUnarchiveAccount(acc.id) },
                )
            }
            items(state.archivedGroups, key = { "g_${it.id}" }) { grp ->
                RecoveryRowCard(
                    title = grp.name,
                    subtitle = "Closed group",
                    actionLabel = "Reopen",
                    onAction = { onReopenGroup(grp.id) },
                )
            }
        }
    }
}


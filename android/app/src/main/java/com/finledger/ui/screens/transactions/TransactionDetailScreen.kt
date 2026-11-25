package com.finledger.ui.screens.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tx by viewModel.observeTransaction(transactionId).collectAsStateWithLifecycle()
    val auditLogs by viewModel.observeAuditHistory(transactionId).collectAsStateWithLifecycle()
    val undoCoordinator = LocalUndoCoordinator.current

    val accountName = tx?.let { t ->
        state.accounts.firstOrNull { it.account.id == t.accountId }?.account?.name ?: "Account #${t.accountId}"
    }.orEmpty()
    val toAccountName = tx?.toAccountId?.let { id ->
        state.accounts.firstOrNull { it.account.id == id }?.account?.name ?: "Account #$id"
    }
    val categoryName = tx?.categoryId?.let { id ->
        state.allCategories.firstOrNull { it.id == id }?.name
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tx != null) {
                        IconButton(onClick = { onEdit(transactionId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                        IconButton(
                            onClick = {
                                val currentTx = tx ?: return@IconButton
                                val label = currentTx.merchantOrPayee.ifBlank {
                                    categoryName ?: currentTx.type.displayName
                                }
                                viewModel.deleteTransaction(transactionId) {
                                    undoCoordinator?.showDeleted(label) {
                                        viewModel.restoreTransactionDirect(transactionId)
                                    }
                                    onBack()
                                }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete",
                                tint = FinLedgerTheme.colors.negative,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val current = tx
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.lg),
            ) {
                Text("Transaction not found", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Surface(
                color = FinLedgerTheme.colors.tileDark1,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = current.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.bodyMuted,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = MoneyFormatter.formatExact(current.amount, current.currency),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                    if (!current.isPersonal) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Your personal share: ${MoneyFormatter.formatExact(current.personalAmount, current.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.skyLinkBlue,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        DetailRow("Date", DateFormatter.formatDate(current.date))
                        DetailRow("Account", accountName)
                        if (toAccountName != null) DetailRow("To Account", toAccountName)
                        if (categoryName != null) DetailRow("Category", categoryName)
                        if (current.merchantOrPayee.isNotBlank()) DetailRow("Merchant / Source", current.merchantOrPayee)
                        if (current.paymentMethod.isNotBlank()) DetailRow("Payment Method", current.paymentMethod)
                        if (current.location.isNotBlank()) DetailRow("Location", current.location)
                        if (current.description.isNotBlank()) DetailRow("Description", current.description)
                        if (current.notes.isNotBlank()) DetailRow("Notes", current.notes)
                        DetailRow("Status", current.status.name)
                    }
                }

                if (auditLogs.isNotEmpty()) {
                    Text(
                        text = "Change History",
                        style = MaterialTheme.typography.titleSmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                    )
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            auditLogs.forEachIndexed { idx, log ->
                                Column(modifier = Modifier.padding(vertical = Spacing.xxs)) {
                                    Text(
                                        text = "${log.action}${if (log.fieldChanged.isNotBlank()) " · ${log.fieldChanged}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = "${DateFormatter.formatDate(log.performedAt)} ${DateFormatter.formatTime(log.performedAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FinLedgerTheme.colors.inkMuted48,
                                    )
                                }
                                if (idx < auditLogs.lastIndex) {
                                    HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}


package com.finledger.ui.screens.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.DocumentEntity
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.screens.security.LocalSuppressAppLock
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenDocument: (Long) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tx by viewModel.observeTransaction(transactionId).collectAsStateWithLifecycle()
    val auditLogs by viewModel.observeAuditHistory(transactionId).collectAsStateWithLifecycle()
    val receipts by viewModel.observeTransactionReceipts(transactionId).collectAsStateWithLifecycle()
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

                TransactionAttachedReceiptsSection(
                    receipts = receipts,
                    vaultDocuments = state.vaultDocuments,
                    onOpenDocument = onOpenDocument,
                    onPickFileUri = { uri ->
                        val title = current.merchantOrPayee.ifBlank { current.description.ifBlank { "Receipt" } }
                        viewModel.attachReceiptUri(transactionId, uri, name = title)
                    },
                    onPickVaultDocument = { doc ->
                        viewModel.attachExistingVaultDocument(transactionId, doc.id)
                    },
                    onDetach = viewModel::detachReceipt,
                )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionReceiptAttachRow(
    vaultDocuments: List<DocumentEntity>,
    onPickFileUri: (Uri) -> Unit,
    onPickVaultDocument: (DocumentEntity) -> Unit,
    stagedLabel: String? = null,
    onClearStaged: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val suppressAppLock = LocalSuppressAppLock.current
    var showVaultDialog by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPickFileUri)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text("Attach receipt (Photo / PDF / Vault)", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OutlinedButton(
                onClick = {
                    suppressAppLock()
                    picker.launch(arrayOf("image/*", "application/pdf"))
                },
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.xxs))
                Text("Photo / PDF")
            }
            OutlinedButton(
                onClick = { showVaultDialog = true },
                enabled = vaultDocuments.isNotEmpty(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(if (vaultDocuments.isEmpty()) "Vault empty" else "From Vault (${vaultDocuments.size})")
            }
        }
        if (stagedLabel != null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Attached: $stagedLabel",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (onClearStaged != null) {
                        IconButton(onClick = onClearStaged) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove receipt")
                        }
                    }
                }
            }
        }
    }

    if (showVaultDialog) {
        AlertDialog(
            onDismissRequest = { showVaultDialog = false },
            title = { Text("Link Vault Document") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(vaultDocuments, key = { it.id }) { doc ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPickVaultDocument(doc)
                                    showVaultDialog = false
                                },
                        ) {
                            Column(modifier = Modifier.padding(Spacing.sm)) {
                                Text(doc.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${doc.type.displayName} · ${DateFormatter.formatDate(doc.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVaultDialog = false }) { Text("Close") }
            },
        )
    }
}

@Composable
fun TransactionAttachedReceiptsSection(
    receipts: List<DocumentEntity>,
    vaultDocuments: List<DocumentEntity>,
    onOpenDocument: ((Long) -> Unit)? = null,
    onPickFileUri: (Uri) -> Unit,
    onPickVaultDocument: (DocumentEntity) -> Unit,
    onDetach: ((Long) -> Unit)? = null,
) {
    Text(
        text = "Receipts",
        style = MaterialTheme.typography.titleSmall,
        color = FinLedgerTheme.colors.inkMuted48,
    )
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (receipts.isEmpty()) {
                Text(
                    text = "No receipts attached yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            } else {
                receipts.forEachIndexed { idx, doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onOpenDocument != null) Modifier.clickable { onOpenDocument(doc.id) } else Modifier)
                            .padding(vertical = Spacing.xxs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${doc.type.displayName} · ${DateFormatter.formatDate(doc.createdAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        if (onDetach != null) {
                            IconButton(onClick = { onDetach(doc.id) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Detach receipt")
                            }
                        }
                    }
                    if (idx < receipts.lastIndex) {
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                    }
                }
            }
            TransactionReceiptAttachRow(
                vaultDocuments = vaultDocuments,
                onPickFileUri = onPickFileUri,
                onPickVaultDocument = onPickVaultDocument,
            )
        }
    }
}


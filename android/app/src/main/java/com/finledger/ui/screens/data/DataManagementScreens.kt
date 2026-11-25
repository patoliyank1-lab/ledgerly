package com.finledger.ui.screens.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.usecase.ColumnMapping
import com.finledger.domain.usecase.ImportRowStatus
import com.finledger.domain.util.DateFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun ImportExportScreen(
    onBack: () -> Unit,
    onOpenColumnMapper: () -> Unit,
    onOpenImportHistory: () -> Unit,
    onOpenRestore: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportCsvToUri) }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let(viewModel::exportPdfToUri) }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportJsonBackupToUri) }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.loadCsvFromUri(it, "transactions_import.csv", onLoaded = onOpenColumnMapper)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import, Export & Backup", style = MaterialTheme.typography.headlineMedium) },
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.freeStorageMb < 100L) {
                item { StorageWarningBanner(freeMb = state.freeStorageMb) }
            }

            state.statusMessage?.let { msg ->
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(Spacing.md))
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Import Transactions", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Select a CSV file, map columns, validate dates/amounts, and detect duplicates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.inkMuted48,
                        )
                        Button(
                            onClick = { csvImportLauncher.launch(arrayOf("text/*", "text/csv")) },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Import CSV File") }
                        OutlinedButton(
                            onClick = onOpenImportHistory,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Import History (${state.importHistory.size})") }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Export Reports", style = MaterialTheme.typography.headlineMedium)
                        Button(
                            onClick = { csvExportLauncher.launch("finledger_transactions.csv") },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Export CSV Statement") }
                        OutlinedButton(
                            onClick = { pdfExportLauncher.launch("finledger_report.pdf") },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Export PDF Statement") }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Full JSON Backup & Restore", style = MaterialTheme.typography.headlineMedium)
                        Button(
                            onClick = { jsonBackupLauncher.launch("finledger_backup.json") },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Create Full JSON Backup") }
                        OutlinedButton(
                            onClick = onOpenRestore,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Restore From Backup") }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageWarningBanner(freeMb: Long) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = FinLedgerTheme.colors.warning.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, FinLedgerTheme.colors.warning),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = "Low storage", tint = FinLedgerTheme.colors.warning)
            Text("Low device storage (${freeMb} MB free). Free up space before large backups.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnMapperScreen(
    onBack: () -> Unit,
    onPreviewReady: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val headers = state.csvTable?.headers.orEmpty()
    var dateIdx by remember { mutableIntStateOf(state.columnMapping.dateIndex) }
    var amountIdx by remember { mutableIntStateOf(state.columnMapping.amountIndex) }
    var merchantIdx by remember { mutableStateOf(state.columnMapping.merchantIndex) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.accounts) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Map CSV Columns", style = MaterialTheme.typography.headlineMedium) },
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
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { selectedAccountId = it.account.id },
                label = "Import Into Account",
            )
            Text("Date Column", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                headers.forEachIndexed { idx, h ->
                    FilterChip(selected = dateIdx == idx, onClick = { dateIdx = idx }, label = { Text(h) })
                }
            }
            Text("Amount Column", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                headers.forEachIndexed { idx, h ->
                    FilterChip(selected = amountIdx == idx, onClick = { amountIdx = idx }, label = { Text(h) })
                }
            }
            Text("Merchant / Description Column", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                headers.forEachIndexed { idx, h ->
                    FilterChip(selected = merchantIdx == idx, onClick = { merchantIdx = idx }, label = { Text(h) })
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val accId = selectedAccountId ?: return@Button
                    viewModel.updateMappingAndAnalyze(
                        mapping = ColumnMapping(dateIndex = dateIdx, amountIndex = amountIdx, merchantIndex = merchantIdx),
                        targetAccountId = accId,
                        currency = state.primaryCurrency,
                        onReady = onPreviewReady,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Validate & Preview (${state.csvTable?.rows?.size ?: 0} rows)") }
        }
    }
}

@Composable
fun ImportPreviewScreen(
    onBack: () -> Unit,
    onCommitted: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var skipDuplicates by remember { mutableStateOf(true) }
    var allowPartial by remember { mutableStateOf(true) }

    val validCount = state.analyzedRows.count { it.status == ImportRowStatus.VALID }
    val invalidCount = state.analyzedRows.count { it.status == ImportRowStatus.INVALID }
    val dupCount = state.analyzedRows.count { it.status == ImportRowStatus.DUPLICATE }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import Preview", style = MaterialTheme.typography.headlineMedium) },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Valid: $validCount", color = FinLedgerTheme.colors.positive, style = MaterialTheme.typography.bodyLarge)
                Text("Duplicates: $dupCount", color = FinLedgerTheme.colors.warning, style = MaterialTheme.typography.bodyLarge)
                Text("Invalid: $invalidCount", color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Skip detected duplicates", style = MaterialTheme.typography.bodySmall)
                Switch(checked = skipDuplicates, onCheckedChange = { skipDuplicates = it })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Import valid rows even if errors exist", style = MaterialTheme.typography.bodySmall)
                Switch(checked = allowPartial, onCheckedChange = { allowPartial = it })
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(state.analyzedRows, key = { it.rowNumber }) { row ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text("Row ${row.rowNumber} · ${row.status.name}", style = MaterialTheme.typography.bodyLarge)
                            Text(row.rawColumns.joinToString(" | "), style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                            row.errorReason?.let { err ->
                                Text(err, style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.negative)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.commitImport(skipDuplicates, allowPartial, onDone = onCommitted) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
            ) { Text("Run Import") }
        }
    }
}

@Composable
fun ImportResultScreen(
    onDone: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val res = state.lastImportResult

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import Report", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (res != null) {
                Text("Batch #${res.batchId} Completed", style = MaterialTheme.typography.headlineMedium)
                Text("Imported: ${res.importedCount} of ${res.totalRows}", style = MaterialTheme.typography.bodyLarge, color = FinLedgerTheme.colors.positive)
                Text("Duplicates skipped: ${res.duplicateCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Failed rows: ${res.failedCount}", style = MaterialTheme.typography.bodyMedium, color = FinLedgerTheme.colors.negative)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onDone,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Done") }
        }
    }
}

@Composable
fun ImportHistoryScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import History", style = MaterialTheme.typography.headlineMedium) },
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
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            items(state.importHistory, key = { it.id }) { batch ->
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
                            Text("${batch.fileName} (${batch.status.name})", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${DateFormatter.formatDate(batch.importedAt)} · ${batch.successRows}/${batch.totalRows} imported",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        if (batch.canRollback) {
                            OutlinedButton(
                                onClick = { viewModel.rollbackImport(batch.id) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("Rollback") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::loadBackupForPreview)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Restore Backup", style = MaterialTheme.typography.headlineMedium) },
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
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Button(
                onClick = { picker.launch(arrayOf("application/json", "*/*")) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Select Backup JSON File") }

            state.restorePreview?.let { preview ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text("Backup Preview", style = MaterialTheme.typography.headlineMedium)
                        Text("Created: ${DateFormatter.formatDate(preview.exportedAtMs)}", style = MaterialTheme.typography.bodySmall)
                        Text("Accounts: ${preview.accountsCount}", style = MaterialTheme.typography.bodyMedium)
                        Text("Transactions: ${preview.transactionsCount}", style = MaterialTheme.typography.bodyMedium)
                        Text("People: ${preview.personsCount} · Loans: ${preview.loansCount}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Button(
                            onClick = { viewModel.confirmRestore(onDone = onBack) },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Restore Now (Replaces Current Data)") }
                    }
                }
            }
        }
    }
}


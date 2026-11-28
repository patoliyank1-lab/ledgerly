package com.finledger.ui.screens.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.screens.security.LocalSuppressAppLock
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun ImportExportScreen(
    onBack: () -> Unit,
    onOpenColumnMapper: () -> Unit,
    onOpenImportPreview: () -> Unit = onOpenColumnMapper,
    onOpenImportHistory: () -> Unit,
    onOpenRestore: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suppressAppLock = LocalSuppressAppLock.current

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
            viewModel.loadCsvFromUri(
                uri = it,
                onNeedsMapping = onOpenColumnMapper,
                onPreviewReady = onOpenImportPreview,
            )
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = viewModel::clearStatus) {
                                Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            if (state.showBackupReminder) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = FinLedgerTheme.colors.warning.copy(alpha = 0.10f)),
                        border = BorderStroke(1.dp, FinLedgerTheme.colors.warning),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text("Back up your data", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "You have ${state.activeTransactionCount} transactions saved locally. Create a JSON backup or turn on weekly auto-backup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted80,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = {
                                        suppressAppLock()
                                        jsonBackupLauncher.launch("finledger_backup.json")
                                    },
                                    shape = MaterialTheme.shapes.extraLarge,
                                ) { Text("Backup Now") }
                                TextButton(onClick = viewModel::snoozeBackupReminder) {
                                    Text("Remind me later")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Import", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Import Transactions", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Select a CSV file, map columns when needed, validate dates/amounts, and detect duplicates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.inkMuted48,
                        )
                        Button(
                            onClick = {
                                suppressAppLock()
                                csvImportLauncher.launch(arrayOf("text/*", "text/csv"))
                            },
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
                Text("Export", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Export Reports", style = MaterialTheme.typography.headlineMedium)
                        Button(
                            onClick = {
                                suppressAppLock()
                                csvExportLauncher.launch("finledger_transactions.csv")
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Export CSV Statement") }
                        OutlinedButton(
                            onClick = {
                                suppressAppLock()
                                pdfExportLauncher.launch("finledger_report.pdf")
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Export PDF Statement") }
                    }
                }
            }

            item {
                Text("Backup & Restore", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Full JSON Backup & Restore", style = MaterialTheme.typography.headlineMedium)
                        state.lastBackupAtMs?.let { ts ->
                            Text(
                                text = "Last backup: ${DateFormatter.formatDate(ts)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAutoBackupEnabled(!state.autoBackupEnabled) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                                Text("Weekly Scheduled Local Backup", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Saves an offline JSON snapshot every week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                            Switch(
                                checked = state.autoBackupEnabled,
                                onCheckedChange = viewModel::setAutoBackupEnabled,
                            )
                        }
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        Button(
                            onClick = {
                                suppressAppLock()
                                jsonBackupLauncher.launch("finledger_backup.json")
                            },
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
    var typeIdx by remember { mutableStateOf(state.columnMapping.typeIndex) }
    var merchantIdx by remember { mutableStateOf(state.columnMapping.merchantIndex) }
    var descriptionIdx by remember { mutableStateOf(state.columnMapping.descriptionIndex) }
    var notesIdx by remember { mutableStateOf(state.columnMapping.notesIndex) }
    var positiveIsIncome by remember { mutableStateOf(state.columnMapping.positiveIsIncome) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.csvTable, state.columnMapping) {
        dateIdx = state.columnMapping.dateIndex
        amountIdx = state.columnMapping.amountIndex
        typeIdx = state.columnMapping.typeIndex
        merchantIdx = state.columnMapping.merchantIndex
        descriptionIdx = state.columnMapping.descriptionIndex
        notesIdx = state.columnMapping.notesIndex
        positiveIsIncome = state.columnMapping.positiveIsIncome
    }

    LaunchedEffect(state.accounts, state.importAccountId) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.importAccountId
                ?.takeIf { id -> state.accounts.any { it.account.id == id } }
                ?: state.accounts.first().account.id
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
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.csvFileName.isNotBlank()) {
                Text(
                    text = "File: ${state.csvFileName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = {
                    selectedAccountId = it.account.id
                    accountError = null
                },
                label = "Import Into Account",
            )
            accountError?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.negative)
            }
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
            OptionalColumnChips("Type Column (Credit/Debit)", headers, typeIdx) { typeIdx = it }
            OptionalColumnChips("Merchant / Payee Column", headers, merchantIdx) { merchantIdx = it }
            OptionalColumnChips("Description Column", headers, descriptionIdx) { descriptionIdx = it }
            OptionalColumnChips("Notes Column", headers, notesIdx) { notesIdx = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Treat positive amounts as Income", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "When no Type column matches, negative values stay Expenses and positive values import as Income.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                    )
                }
                Switch(checked = positiveIsIncome, onCheckedChange = { positiveIsIncome = it })
            }

            Button(
                onClick = {
                    val accId = selectedAccountId
                    if (accId == null) {
                        accountError = "Select a target account first"
                        return@Button
                    }
                    viewModel.updateMappingAndAnalyze(
                        mapping = ColumnMapping(
                            dateIndex = dateIdx,
                            amountIndex = amountIdx,
                            typeIndex = typeIdx,
                            merchantIndex = merchantIdx,
                            descriptionIndex = descriptionIdx,
                            notesIndex = notesIdx,
                            positiveIsIncome = positiveIsIncome,
                        ),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionalColumnChips(
    title: String,
    headers: List<String>,
    selectedIdx: Int?,
    onSelect: (Int?) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilterChip(selected = selectedIdx == null, onClick = { onSelect(null) }, label = { Text("None") })
        headers.forEachIndexed { idx, h ->
            FilterChip(selected = selectedIdx == idx, onClick = { onSelect(idx) }, label = { Text(h) })
        }
    }
}

@Composable
fun ImportPreviewScreen(
    onBack: () -> Unit,
    onChangeColumns: () -> Unit = onBack,
    onCommitted: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var skipDuplicates by remember { mutableStateOf(true) }
    var allowPartial by remember { mutableStateOf(true) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatus()
        }
    }

    val validCount = state.analyzedRows.count { it.status == ImportRowStatus.VALID }
    val invalidCount = state.analyzedRows.count { it.status == ImportRowStatus.INVALID }
    val dupCount = state.analyzedRows.count { it.status == ImportRowStatus.DUPLICATE }
    val targetAccountName = state.accounts.firstOrNull { it.account.id == state.importAccountId }?.account?.name

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Import Preview", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onChangeColumns) {
                        Text("Change columns")
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
            if (targetAccountName != null) {
                Text(
                    text = "Importing to: $targetAccountName",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xxs),
                )
            }
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
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRollbackId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        if (state.importHistory.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.UploadFile,
                title = "No CSV imports yet",
                subtitle = "Imported CSV batches will appear here with rollback support.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
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
                                    onClick = { pendingRollbackId = batch.id },
                                    shape = MaterialTheme.shapes.extraLarge,
                                ) { Text("Rollback") }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRollbackId?.let { batchId ->
        AlertDialog(
            onDismissRequest = { pendingRollbackId = null },
            title = { Text("Rollback Import #$batchId?") },
            text = { Text("All transactions imported in this batch will be soft-deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rollbackImport(batchId)
                        pendingRollbackId = null
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Rollback") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRollbackId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun RestoreScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val suppressAppLock = LocalSuppressAppLock.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::loadBackupForPreview)
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                onClick = {
                    suppressAppLock()
                    picker.launch(arrayOf("application/json", "*/*"))
                },
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


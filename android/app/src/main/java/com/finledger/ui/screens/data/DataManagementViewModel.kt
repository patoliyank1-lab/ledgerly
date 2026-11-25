package com.finledger.ui.screens.data

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.dao.BackupHistoryDao
import com.finledger.data.local.dao.ImportHistoryDao
import com.finledger.data.local.entity.*
import com.finledger.data.repository.*
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class DataManagementUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val importHistory: List<ImportHistoryEntity> = emptyList(),
    val backupHistory: List<BackupHistoryEntity> = emptyList(),
    val documents: List<DocumentEntity> = emptyList(),
    val deletedAccounts: List<AccountEntity> = emptyList(),
    val deletedTransactions: List<TransactionEntity> = emptyList(),
    val archivedAccounts: List<AccountEntity> = emptyList(),
    val archivedGroups: List<GroupEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val freeStorageMb: Long = 1024L,
    val statusMessage: String? = null,
    val csvFileName: String = "",
    val csvTable: CsvTable? = null,
    val columnMapping: ColumnMapping = ColumnMapping(),
    val analyzedRows: List<AnalyzedImportRow> = emptyList(),
    val lastImportResult: ImportExecutionResult? = null,
    val restorePreview: RestorePreview? = null,
)

@Singleton
class ImportSessionHolder @Inject constructor() {
    val csvState = MutableStateFlow<Pair<String, CsvTable?>>("" to null)
    val mapping = MutableStateFlow(ColumnMapping())
    val analyzed = MutableStateFlow<List<AnalyzedImportRow>>(emptyList())
    val importResult = MutableStateFlow<ImportExecutionResult?>(null)
    val restorePreview = MutableStateFlow<RestorePreview?>(null)
}

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
    auditLogRepository: AuditLogRepository,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val exportPdfUseCase: ExportPdfUseCase,
    private val backupUseCase: BackupUseCase,
    private val restoreUseCase: RestoreUseCase,
    private val executeImportUseCase: ExecuteImportUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val session: ImportSessionHolder,
    importHistoryDao: ImportHistoryDao,
    backupHistoryDao: BackupHistoryDao,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    private val _csvState = session.csvState
    private val _mapping = session.mapping
    private val _analyzed = session.analyzed
    private val _importResult = session.importResult
    private val _restorePreview = session.restorePreview

    val uiState: StateFlow<DataManagementUiState> = combine<Any?, DataManagementUiState>(
        getAccountsUseCase(),
        importHistoryDao.observeAll(),
        backupHistoryDao.observeAll(),
        saveDocumentUseCase.observeAll(),
        accountRepository.observeDeletedAccounts(),
        transactionRepository.observeDeletedTransactions(),
        accountRepository.observeArchivedAccounts(),
        socialRepository.observeArchivedGroups(),
        auditLogRepository.observeRecent(150),
        userPreferencesRepository.preferences,
        _status,
        _csvState,
        _mapping,
        _analyzed,
        _importResult,
        _restorePreview,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val accounts = values[0] as List<AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val imports = values[1] as List<ImportHistoryEntity>
        @Suppress("UNCHECKED_CAST")
        val backups = values[2] as List<BackupHistoryEntity>
        @Suppress("UNCHECKED_CAST")
        val docs = values[3] as List<DocumentEntity>
        @Suppress("UNCHECKED_CAST")
        val delAccounts = values[4] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val delTxs = values[5] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val archAccounts = values[6] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val archGroups = values[7] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val logs = values[8] as List<AuditLogEntity>
        val prefs = values[9] as com.finledger.domain.model.UserPreferences
        val status = values[10] as String?
        @Suppress("UNCHECKED_CAST")
        val csvPair = values[11] as Pair<String, CsvTable?>
        val mapping = values[12] as ColumnMapping
        @Suppress("UNCHECKED_CAST")
        val analyzed = values[13] as List<AnalyzedImportRow>
        val importRes = values[14] as ImportExecutionResult?
        val restorePrev = values[15] as RestorePreview?

        DataManagementUiState(
            accounts = accounts,
            importHistory = imports,
            backupHistory = backups,
            documents = docs,
            deletedAccounts = delAccounts,
            deletedTransactions = delTxs,
            archivedAccounts = archAccounts,
            archivedGroups = archGroups,
            auditLogs = logs,
            primaryCurrency = prefs.primaryCurrency,
            freeStorageMb = availableStorageMb(),
            statusMessage = status,
            csvFileName = csvPair.first,
            csvTable = csvPair.second,
            columnMapping = mapping,
            analyzedRows = analyzed,
            lastImportResult = importRes,
            restorePreview = restorePrev,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataManagementUiState())

    fun exportCsvToUri(uri: Uri) {
        viewModelScope.launch {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                exportCsvUseCase(stream)
                    .onSuccess { count -> _status.value = "Exported $count transactions to CSV" }
                    .onFailure { _status.value = "CSV export failed: ${it.message}" }
            }
        }
    }

    fun exportPdfToUri(uri: Uri) {
        viewModelScope.launch {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                exportPdfUseCase(stream)
                    .onSuccess { count -> _status.value = "Exported $count transactions to PDF" }
                    .onFailure { _status.value = "PDF export failed: ${it.message}" }
            }
        }
    }

    fun exportJsonBackupToUri(uri: Uri) {
        viewModelScope.launch {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                backupUseCase.writeBackupToStream(stream, "finledger_backup.json", isScheduled = false)
                    .onSuccess { bytes -> _status.value = "Backup saved (${bytes / 1024} KB)" }
                    .onFailure { _status.value = "Backup failed: ${it.message}" }
            }
        }
    }

    fun loadBackupForPreview(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Cannot read backup file")
            }.onSuccess { raw ->
                restoreUseCase.preview(raw)
                    .onSuccess { _restorePreview.value = it }
                    .onFailure { _status.value = "Invalid backup file: ${it.message}" }
            }.onFailure { _status.value = it.message }
        }
    }

    fun confirmRestore(onDone: () -> Unit) {
        val preview = _restorePreview.value ?: return
        viewModelScope.launch {
            restoreUseCase.executeRestore(preview.backup)
                .onSuccess {
                    _restorePreview.value = null
                    _status.value = "Backup restored (${preview.transactionsCount} transactions)"
                    onDone()
                }
                .onFailure { _status.value = "Restore failed: ${it.message}" }
        }
    }

    fun loadCsvFromUri(uri: Uri, fileName: String, onLoaded: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Cannot read CSV file")
            }.onSuccess { raw ->
                val table = CsvParser.parse(raw)
                _csvState.value = fileName to table
                _mapping.value = ColumnMapping(
                    dateIndex = 0,
                    amountIndex = 1.coerceAtMost((table.headers.size - 1).coerceAtLeast(0)),
                    merchantIndex = if (table.headers.size > 2) 2 else null,
                    descriptionIndex = if (table.headers.size > 3) 3 else null,
                )
                onLoaded()
            }.onFailure { _status.value = "CSV parse error: ${it.message}" }
        }
    }

    fun updateMappingAndAnalyze(mapping: ColumnMapping, targetAccountId: Long, currency: String, onReady: () -> Unit) {
        val table = _csvState.value.second ?: return
        _mapping.value = mapping
        viewModelScope.launch {
            _analyzed.value = executeImportUseCase.analyze(table, mapping, targetAccountId, currency)
            onReady()
        }
    }

    fun commitImport(skipDuplicates: Boolean, allowPartial: Boolean, onDone: () -> Unit) {
        val name = _csvState.value.first.ifBlank { "import.csv" }
        val rows = _analyzed.value
        viewModelScope.launch {
            executeImportUseCase.commitImport(name, rows, skipDuplicates, allowPartial)
                .onSuccess { res ->
                    _importResult.value = res
                    onDone()
                }
                .onFailure { _status.value = it.message }
        }
    }

    fun rollbackImport(batchId: Long) {
        viewModelScope.launch {
            executeImportUseCase.rollbackBatch(batchId)
            _status.value = "Import batch #$batchId rolled back"
        }
    }

    fun saveDocument(uri: Uri, name: String, type: DocumentType, notes: String) {
        viewModelScope.launch {
            saveDocumentUseCase.importFromUri(uri, name, type, notes = notes)
                .onSuccess { _status.value = "Document saved to vault" }
                .onFailure { _status.value = it.message }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch { saveDocumentUseCase.softDelete(id) }
    }

    fun observeDocument(id: Long): StateFlow<DocumentEntity?> =
        saveDocumentUseCase.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restoreTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.restore(id) }
    }

    fun restoreAccount(id: Long) {
        viewModelScope.launch { accountRepository.restore(id) }
    }

    fun unarchiveAccount(id: Long) {
        viewModelScope.launch { accountRepository.setArchived(id, false) }
    }

    fun unarchiveGroup(id: Long) {
        viewModelScope.launch { socialRepository.updateGroupStatus(id, GroupStatus.ACTIVE) }
    }

    fun clearStatus() {
        _status.value = null
    }

    private fun availableStorageMb(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024L)
        } catch (_: Exception) {
            1024L
        }
    }
}

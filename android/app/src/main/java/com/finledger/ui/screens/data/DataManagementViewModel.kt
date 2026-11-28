package com.finledger.ui.screens.data

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.dao.BackupHistoryDao
import com.finledger.data.local.dao.ImportHistoryDao
import com.finledger.data.local.entity.*
import com.finledger.data.repository.*
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.*
import com.finledger.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
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
    val autoBackupEnabled: Boolean = false,
    val activeTransactionCount: Int = 0,
    val lastBackupAtMs: Long? = null,
    val showBackupReminder: Boolean = false,
    val freeStorageMb: Long = 1024L,
    val statusMessage: String? = null,
    val csvFileName: String = "",
    val csvTable: CsvTable? = null,
    val columnMapping: ColumnMapping = ColumnMapping(),
    val importAccountId: Long? = null,
    val analyzedRows: List<AnalyzedImportRow> = emptyList(),
    val lastImportResult: ImportExecutionResult? = null,
    val restorePreview: RestorePreview? = null,
)

@Singleton
class ImportSessionHolder @Inject constructor() {
    val csvState = MutableStateFlow<Pair<String, CsvTable?>>("" to null)
    val mapping = MutableStateFlow(ColumnMapping())
    val importAccountId = MutableStateFlow<Long?>(null)
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
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    private val _csvState = session.csvState
    private val _mapping = session.mapping
    private val _importAccountId = session.importAccountId
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
        transactionRepository.observeAllTransactionsWithDetails(),
        _status,
        _csvState,
        _mapping,
        _importAccountId,
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
        @Suppress("UNCHECKED_CAST")
        val allTxs = values[10] as List<TransactionWithDetails>
        val status = values[11] as String?
        @Suppress("UNCHECKED_CAST")
        val csvPair = values[12] as Pair<String, CsvTable?>
        val mapping = values[13] as ColumnMapping
        val importAccId = values[14] as Long?
        @Suppress("UNCHECKED_CAST")
        val analyzed = values[15] as List<AnalyzedImportRow>
        val importRes = values[16] as ImportExecutionResult?
        val restorePrev = values[17] as RestorePreview?

        val activeTxCount = allTxs.count { !it.transaction.isDeleted }
        val lastBackupAt = backups.firstOrNull()?.createdAt

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
            autoBackupEnabled = prefs.autoBackupEnabled,
            activeTransactionCount = activeTxCount,
            lastBackupAtMs = lastBackupAt,
            showBackupReminder = shouldShowBackupReminder(
                activeTransactionCount = activeTxCount,
                lastBackupAtMs = lastBackupAt,
                autoBackupEnabled = prefs.autoBackupEnabled,
                snoozedUntilMs = prefs.backupReminderSnoozedUntilMs,
            ),
            freeStorageMb = availableStorageMb(),
            statusMessage = status,
            csvFileName = csvPair.first,
            csvTable = csvPair.second,
            columnMapping = mapping,
            importAccountId = importAccId,
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

    fun loadCsvFromUri(
        uri: Uri,
        fileName: String? = null,
        onNeedsMapping: () -> Unit,
        onPreviewReady: () -> Unit = onNeedsMapping,
    ) {
        viewModelScope.launch {
            val resolvedName = fileName ?: resolveDisplayName(uri)
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Cannot read CSV file")
            }.onSuccess { raw ->
                val table = CsvParser.parse(raw)
                if (table.headers.isEmpty()) {
                    _status.value = "CSV file has no headers or rows"
                    return@onSuccess
                }
                val detected = ImportValidator.autoDetectMapping(table.headers)
                val prefs = userPreferencesRepository.preferences.first()
                val activeAccounts = uiState.value.accounts.ifEmpty {
                    accountRepository.observeActiveAccountsWithBalances().first()
                }
                val targetAccountId = prefs.lastUsedAccountId
                    ?.takeIf { id -> activeAccounts.any { it.account.id == id } }
                    ?: activeAccounts.firstOrNull()?.account?.id

                _csvState.value = resolvedName to table
                _analyzed.value = emptyList()
                _importResult.value = null
                _mapping.value = detected
                _importAccountId.value = targetAccountId

                if (targetAccountId != null && ImportValidator.isMappingConfident(table, detected)) {
                    _analyzed.value = executeImportUseCase.analyze(table, detected, targetAccountId, prefs.primaryCurrency)
                    onPreviewReady()
                } else {
                    onNeedsMapping()
                }
            }.onFailure { _status.value = "CSV parse error: ${it.message}" }
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else {
                    null
                }
            }
        }.getOrNull() ?: "transactions_import.csv"
    }

    fun updateMappingAndAnalyze(mapping: ColumnMapping, targetAccountId: Long, currency: String, onReady: () -> Unit) {
        val table = _csvState.value.second ?: return
        _mapping.value = mapping
        _importAccountId.value = targetAccountId
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

    suspend fun restoreDocumentDirect(id: Long) {
        saveDocumentUseCase.restore(id)
    }

    private val documentFlows = ConcurrentHashMap<Long, StateFlow<DocumentEntity?>>()

    fun observeDocument(id: Long): StateFlow<DocumentEntity?> = documentFlows.getOrPut(id) {
        saveDocumentUseCase.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

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

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(enabled)
            ReminderScheduler.syncWithPreferences(context, userPreferencesRepository.preferences.first())
        }
    }

    fun snoozeBackupReminder() {
        viewModelScope.launch {
            userPreferencesRepository.snoozeBackupReminder(System.currentTimeMillis() + BACKUP_SNOOZE_MS)
        }
    }

    private fun availableStorageMb(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024L)
        } catch (_: Exception) {
            1024L
        }
    }

    companion object {
        const val BACKUP_REMINDER_TX_THRESHOLD = 20
        const val BACKUP_SNOOZE_MS = 7L * 86_400_000L

        fun shouldShowBackupReminder(
            activeTransactionCount: Int,
            lastBackupAtMs: Long?,
            autoBackupEnabled: Boolean,
            snoozedUntilMs: Long,
            nowMs: Long = System.currentTimeMillis(),
        ): Boolean {
            if (activeTransactionCount < BACKUP_REMINDER_TX_THRESHOLD) return false
            if (nowMs < snoozedUntilMs) return false
            if (lastBackupAtMs != null && (nowMs - lastBackupAtMs) in 0L..BACKUP_SNOOZE_MS) return false
            return lastBackupAtMs == null || !autoBackupEnabled
        }
    }
}

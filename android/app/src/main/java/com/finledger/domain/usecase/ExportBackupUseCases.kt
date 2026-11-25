package com.finledger.domain.usecase

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.finledger.data.local.AppDatabase
import com.finledger.data.local.dao.*
import com.finledger.data.local.entity.*
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

@Serializable
data class FinLedgerBackup(
    val schemaVersion: Int = 1,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val sharedExpenses: List<SharedExpenseEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanRepayments: List<LoanRepaymentEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val investmentTransactions: List<InvestmentTransactionEntity> = emptyList(),
)

data class RestorePreview(
    val exportedAtMs: Long,
    val accountsCount: Int,
    val transactionsCount: Int,
    val personsCount: Int,
    val loansCount: Int,
    val backup: FinLedgerBackup,
)

class ExportCsvUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {
    suspend operator fun invoke(outputStream: OutputStream): Result<Int> = runCatching {
        val txs = transactionDao.getAllOnce()
        val accounts = accountDao.getAllAccountsOnce().associateBy { it.id }
        val categories = categoryDao.getAllOnce().associateBy { it.id }

        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("Date,Type,Amount,PersonalShare,Currency,Account,ToAccount,Category,Merchant,Description,Notes")
            for (tx in txs) {
                val row = listOf(
                    DateFormatter.formatDate(tx.date),
                    tx.type.name,
                    MoneyFormatter.paiseToInputString(tx.amount),
                    MoneyFormatter.paiseToInputString(tx.personalAmount),
                    tx.currency,
                    accounts[tx.accountId]?.name.orEmpty(),
                    tx.toAccountId?.let { accounts[it]?.name }.orEmpty(),
                    tx.categoryId?.let { categories[it]?.name }.orEmpty(),
                    tx.merchantOrPayee,
                    tx.description,
                    tx.notes,
                ).joinToString(",") { escapeCsv(it) }
                writer.appendLine(row)
            }
        }
        txs.size
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}

class ExportPdfUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {
    suspend operator fun invoke(outputStream: OutputStream): Result<Int> = runCatching {
        val txs = transactionDao.getAllOnce()
        val accounts = accountDao.getAllAccountsOnce().associateBy { it.id }
        val categories = categoryDao.getAllOnce().associateBy { it.id }

        val pdf = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val mutedPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.DKGRAY }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
        var canvas = page.canvas
        var y = 48f

        canvas.drawText("FinLedger Financial Statement", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Generated: ${DateFormatter.formatDate(System.currentTimeMillis())} · ${txs.size} transactions", 40f, y, mutedPaint)
        y += 28f

        for (tx in txs) {
            if (y > 790f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                y = 48f
            }
            val acc = accounts[tx.accountId]?.name.orEmpty()
            val cat = tx.categoryId?.let { categories[it]?.name } ?: tx.type.displayName
            val label = tx.merchantOrPayee.ifBlank { tx.description.ifBlank { cat } }.take(28)
            val amt = MoneyFormatter.format(tx.amount, tx.currency)
            canvas.drawText("${DateFormatter.formatDate(tx.date)}  |  $label  |  $acc  |  $amt", 40f, y, bodyPaint)
            y += 18f
        }
        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
        txs.size
    }
}

class BackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val transactionDao: TransactionDao,
    private val personDao: PersonDao,
    private val groupDao: GroupDao,
    private val eventDao: EventDao,
    private val sharedExpenseDao: SharedExpenseDao,
    private val settlementDao: SettlementDao,
    private val loanDao: LoanDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val investmentDao: InvestmentDao,
    private val backupHistoryDao: BackupHistoryDao,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun createBackupSnapshotJson(): String {
        val backup = FinLedgerBackup(
            accounts = accountDao.getAllAccountsOnce(),
            categories = categoryDao.getAllOnce(),
            tags = tagDao.getAllOnce(),
            transactions = transactionDao.getAllOnce(),
            persons = personDao.getAllOnce(),
            groups = groupDao.getAllOnce(),
            events = eventDao.getAllOnce(),
            sharedExpenses = sharedExpenseDao.getAllOnce(),
            settlements = settlementDao.getAllOnce(),
            loans = loanDao.getAllOnce(),
            loanRepayments = loanDao.getAllRepaymentsOnce(),
            bills = billDao.getAllOnce(),
            budgets = budgetDao.getAllOnce(),
            savingsGoals = savingsGoalDao.getAllOnce(),
            investments = investmentDao.getAllOnce(),
            investmentTransactions = investmentDao.getAllTransactionsOnce(),
        )
        return json.encodeToString(backup)
    }

    suspend fun writeBackupToStream(outputStream: OutputStream, fileName: String, isScheduled: Boolean): Result<Long> =
        runCatching {
            val content = createBackupSnapshotJson()
            val bytes = content.toByteArray(Charsets.UTF_8)
            outputStream.use { it.write(bytes) }
            backupHistoryDao.insert(
                BackupHistoryEntity(
                    fileName = fileName,
                    fileUri = fileName,
                    fileSizeBytes = bytes.size.toLong(),
                    isScheduled = isScheduled,
                )
            )
            bytes.size.toLong()
        }

    suspend fun writeLocalAutoBackup(): Result<File> = runCatching {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val fileName = "finledger_backup_${System.currentTimeMillis()}.json"
        val file = File(dir, fileName)
        file.outputStream().use { stream ->
            writeBackupToStream(stream, fileName, isScheduled = true).getOrThrow()
        }
        file
    }
}

class RestoreUseCase @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val transactionDao: TransactionDao,
    private val personDao: PersonDao,
    private val groupDao: GroupDao,
    private val eventDao: EventDao,
    private val settlementDao: SettlementDao,
    private val loanDao: LoanDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val investmentDao: InvestmentDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun preview(rawJson: String): Result<RestorePreview> = runCatching {
        val backup = json.decodeFromString<FinLedgerBackup>(rawJson)
        RestorePreview(
            exportedAtMs = backup.exportedAtMs,
            accountsCount = backup.accounts.size,
            transactionsCount = backup.transactions.size,
            personsCount = backup.persons.size,
            loansCount = backup.loans.size,
            backup = backup,
        )
    }

    suspend fun executeRestore(backup: FinLedgerBackup): Result<Unit> = runCatching {
        database.clearAllTables()
        if (backup.categories.isNotEmpty()) categoryDao.insertAll(backup.categories)
        if (backup.tags.isNotEmpty()) tagDao.insertAll(backup.tags)
        if (backup.accounts.isNotEmpty()) accountDao.insertAll(backup.accounts)
        if (backup.persons.isNotEmpty()) personDao.insertAll(backup.persons)
        if (backup.groups.isNotEmpty()) groupDao.insertAll(backup.groups)
        if (backup.events.isNotEmpty()) eventDao.insertAll(backup.events)
        if (backup.transactions.isNotEmpty()) transactionDao.insertAll(backup.transactions)
        if (backup.settlements.isNotEmpty()) settlementDao.insertAll(backup.settlements)
        if (backup.loans.isNotEmpty()) loanDao.insertLoans(backup.loans)
        if (backup.loanRepayments.isNotEmpty()) loanDao.insertRepayments(backup.loanRepayments)
        if (backup.bills.isNotEmpty()) billDao.insertBills(backup.bills)
        if (backup.budgets.isNotEmpty()) budgetDao.insertAll(backup.budgets)
        if (backup.savingsGoals.isNotEmpty()) savingsGoalDao.insertAll(backup.savingsGoals)
        if (backup.investments.isNotEmpty()) investmentDao.insertInvestments(backup.investments)
        if (backup.investmentTransactions.isNotEmpty()) investmentDao.insertTransactions(backup.investmentTransactions)
        Unit
    }
}


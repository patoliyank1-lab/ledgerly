package com.finledger.domain.usecase

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
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
    val schemaVersion: Int = 2,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val transactionTags: List<TransactionTagCrossRef> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val groupMembers: List<GroupMemberEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val eventParticipants: List<EventParticipantEntity> = emptyList(),
    val sharedExpenses: List<SharedExpenseEntity> = emptyList(),
    val expensePayers: List<ExpensePayerEntity> = emptyList(),
    val expenseSplits: List<ExpenseSplitEntity> = emptyList(),
    val sharedExpenseItems: List<SharedExpenseItemEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanRepayments: List<LoanRepaymentEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val billPayments: List<BillPaymentEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val investmentTransactions: List<InvestmentTransactionEntity> = emptyList(),
    val netWorthSnapshots: List<NetWorthSnapshotEntity> = emptyList(),
    val documents: List<DocumentEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
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
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = android.graphics.Color.DKGRAY }
        val bodyPaint = Paint().apply { textSize = 10f }
        val mutedPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.DKGRAY }
        val dividerPaint = Paint().apply { strokeWidth = 0.8f; color = android.graphics.Color.LTGRAY }

        fun drawTableHeader(canvas: android.graphics.Canvas, startY: Float): Float {
            canvas.drawText("Date", 40f, startY, headerPaint)
            canvas.drawText("Merchant / Description", 118f, startY, headerPaint)
            canvas.drawText("Account", 315f, startY, headerPaint)
            canvas.drawText("Amount", 438f, startY, headerPaint)
            canvas.drawLine(40f, startY + 5f, 555f, startY + 5f, dividerPaint)
            return startY + 20f
        }

        fun fitText(text: String, maxWidth: Float, paint: Paint): String {
            if (paint.measureText(text) <= maxWidth) return text
            var end = text.length
            while (end > 1 && paint.measureText("${text.substring(0, end)}…") > maxWidth) {
                end--
            }
            return if (end <= 1) text.take(1) else "${text.substring(0, end)}…"
        }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
        var canvas = page.canvas
        var y = 48f

        canvas.drawText("FinLedger Financial Statement", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Generated: ${DateFormatter.formatDate(System.currentTimeMillis())} · ${txs.size} transactions", 40f, y, mutedPaint)
        y += 24f
        y = drawTableHeader(canvas, y)

        for (tx in txs) {
            if (y > 790f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                y = drawTableHeader(canvas, 48f)
            }
            val acc = accounts[tx.accountId]?.name.orEmpty()
            val cat = tx.categoryId?.let { categories[it]?.name } ?: tx.type.displayName
            val label = tx.merchantOrPayee.ifBlank { tx.description.ifBlank { cat } }
            val amt = MoneyFormatter.format(tx.amount, tx.currency)
            canvas.drawText(fitText(DateFormatter.formatDate(tx.date), 72f, bodyPaint), 40f, y, bodyPaint)
            canvas.drawText(fitText(label, 190f, bodyPaint), 118f, y, bodyPaint)
            canvas.drawText(fitText(acc, 115f, bodyPaint), 315f, y, bodyPaint)
            canvas.drawText(fitText(amt, 115f, bodyPaint), 438f, y, bodyPaint)
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
    private val netWorthDao: NetWorthDao,
    private val documentDao: DocumentDao,
    private val auditLogDao: AuditLogDao,
    private val backupHistoryDao: BackupHistoryDao,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun createBackupSnapshotJson(): String {
        val backup = FinLedgerBackup(
            accounts = accountDao.getAllRawOnce(),
            categories = categoryDao.getAllRawOnce(),
            tags = tagDao.getAllRawOnce(),
            transactionTags = tagDao.getAllCrossRefsOnce(),
            transactions = transactionDao.getAllRawOnce(),
            persons = personDao.getAllRawOnce(),
            groups = groupDao.getAllRawOnce(),
            groupMembers = groupDao.getAllMembersOnce(),
            events = eventDao.getAllRawOnce(),
            eventParticipants = eventDao.getAllParticipantsOnce(),
            sharedExpenses = sharedExpenseDao.getAllRawOnce(),
            expensePayers = sharedExpenseDao.getAllPayersOnce(),
            expenseSplits = sharedExpenseDao.getAllSplitsOnce(),
            sharedExpenseItems = sharedExpenseDao.getAllItemsOnce(),
            settlements = settlementDao.getAllRawOnce(),
            loans = loanDao.getAllRawOnce(),
            loanRepayments = loanDao.getAllRepaymentsRawOnce(),
            bills = billDao.getAllRawOnce(),
            billPayments = billDao.getAllPaymentsRawOnce(),
            budgets = budgetDao.getAllRawOnce(),
            savingsGoals = savingsGoalDao.getAllRawOnce(),
            investments = investmentDao.getAllRawOnce(),
            investmentTransactions = investmentDao.getAllTransactionsRawOnce(),
            netWorthSnapshots = netWorthDao.getAllOnce(),
            documents = documentDao.getAllRawOnce(),
            auditLogs = auditLogDao.getAllOnce(),
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
    private val sharedExpenseDao: SharedExpenseDao,
    private val settlementDao: SettlementDao,
    private val loanDao: LoanDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val investmentDao: InvestmentDao,
    private val netWorthDao: NetWorthDao,
    private val documentDao: DocumentDao,
    private val auditLogDao: AuditLogDao,
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
        database.withTransaction {
            database.clearAllTables()
            if (backup.categories.isNotEmpty()) categoryDao.insertAll(backup.categories)
            if (backup.tags.isNotEmpty()) tagDao.insertAll(backup.tags)
            if (backup.accounts.isNotEmpty()) accountDao.insertAll(backup.accounts)
            if (backup.persons.isNotEmpty()) personDao.insertAll(backup.persons)
            if (backup.groups.isNotEmpty()) groupDao.insertAll(backup.groups)
            if (backup.groupMembers.isNotEmpty()) groupDao.insertMembers(backup.groupMembers)
            if (backup.events.isNotEmpty()) eventDao.insertAll(backup.events)
            if (backup.eventParticipants.isNotEmpty()) eventDao.insertParticipants(backup.eventParticipants)
            if (backup.sharedExpenses.isNotEmpty()) sharedExpenseDao.insertExpenses(backup.sharedExpenses)
            if (backup.expensePayers.isNotEmpty()) sharedExpenseDao.insertPayers(backup.expensePayers)
            if (backup.expenseSplits.isNotEmpty()) sharedExpenseDao.insertSplits(backup.expenseSplits)
            if (backup.sharedExpenseItems.isNotEmpty()) sharedExpenseDao.insertItems(backup.sharedExpenseItems)
            if (backup.transactions.isNotEmpty()) transactionDao.insertAll(backup.transactions)
            if (backup.transactionTags.isNotEmpty()) tagDao.insertCrossRefs(backup.transactionTags)
            if (backup.settlements.isNotEmpty()) settlementDao.insertAll(backup.settlements)
            if (backup.loans.isNotEmpty()) loanDao.insertLoans(backup.loans)
            if (backup.loanRepayments.isNotEmpty()) loanDao.insertRepayments(backup.loanRepayments)
            if (backup.bills.isNotEmpty()) billDao.insertBills(backup.bills)
            if (backup.billPayments.isNotEmpty()) billDao.insertPayments(backup.billPayments)
            if (backup.budgets.isNotEmpty()) budgetDao.insertAll(backup.budgets)
            if (backup.savingsGoals.isNotEmpty()) savingsGoalDao.insertAll(backup.savingsGoals)
            if (backup.investments.isNotEmpty()) investmentDao.insertInvestments(backup.investments)
            if (backup.investmentTransactions.isNotEmpty()) investmentDao.insertTransactions(backup.investmentTransactions)
            if (backup.netWorthSnapshots.isNotEmpty()) netWorthDao.insertAll(backup.netWorthSnapshots)
            if (backup.documents.isNotEmpty()) documentDao.insertAll(backup.documents)
            if (backup.auditLogs.isNotEmpty()) auditLogDao.insertAll(backup.auditLogs)
        }
    }
}


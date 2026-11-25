package com.finledger.domain.usecase

import com.finledger.data.local.dao.ImportHistoryDao
import com.finledger.data.local.dao.TransactionDao
import com.finledger.data.local.entity.ImportHistoryEntity
import com.finledger.data.local.entity.ImportStatus
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.domain.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class CsvTable(
    val headers: List<String>,
    val rows: List<List<String>>,
)

data class ColumnMapping(
    val dateIndex: Int = 0,
    val amountIndex: Int = 1,
    val typeIndex: Int? = null,
    val merchantIndex: Int? = null,
    val descriptionIndex: Int? = null,
    val notesIndex: Int? = null,
)

enum class ImportRowStatus { VALID, INVALID, DUPLICATE }

data class AnalyzedImportRow(
    val rowNumber: Int,
    val rawColumns: List<String>,
    val status: ImportRowStatus,
    val errorReason: String? = null,
    val parsedTransaction: TransactionEntity? = null,
)

data class ImportExecutionResult(
    val batchId: Long,
    val totalRows: Int,
    val importedCount: Int,
    val failedCount: Int,
    val duplicateCount: Int,
    val failedCsvText: String,
)

object CsvParser {
    fun parse(rawText: String): CsvTable {
        val lines = rawText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvTable(emptyList(), emptyList())
        val parsedLines = lines.map { parseLine(it) }
        return CsvTable(
            headers = parsedLines.first(),
            rows = parsedLines.drop(1),
        )
    }

    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}

object ImportValidator {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
    ).onEach { it.isLenient = false }

    fun parseDate(raw: String): Long? {
        val trimmed = raw.trim()
        for (fmt in dateFormats) {
            try {
                return fmt.parse(trimmed)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun validateRow(
        rowNumber: Int,
        cols: List<String>,
        mapping: ColumnMapping,
        targetAccountId: Long,
        currency: String,
    ): AnalyzedImportRow {
        val rawDate = cols.getOrNull(mapping.dateIndex).orEmpty()
        val parsedDate = parseDate(rawDate)
            ?: return AnalyzedImportRow(rowNumber, cols, ImportRowStatus.INVALID, "Invalid date '$rawDate'")

        val rawAmount = cols.getOrNull(mapping.amountIndex).orEmpty()
        val rawSignedPaise = MoneyFormatter.parseToPaise(rawAmount)
            ?: return AnalyzedImportRow(rowNumber, cols, ImportRowStatus.INVALID, "Invalid amount '$rawAmount'")

        if (rawSignedPaise == 0L) {
            return AnalyzedImportRow(rowNumber, cols, ImportRowStatus.INVALID, "Amount cannot be zero")
        }

        val rawType = mapping.typeIndex?.let { cols.getOrNull(it) }.orEmpty()
        val txType = when {
            rawType.equals("INCOME", ignoreCase = true) || rawType.equals("CREDIT", ignoreCase = true) -> TransactionType.INCOME
            rawType.equals("EXPENSE", ignoreCase = true) || rawType.equals("DEBIT", ignoreCase = true) -> TransactionType.EXPENSE
            rawType.equals("REFUND", ignoreCase = true) -> TransactionType.REFUND
            rawSignedPaise < 0 -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }

        val absAmount = kotlin.math.abs(rawSignedPaise)
        val merchant = mapping.merchantIndex?.let { cols.getOrNull(it) }.orEmpty()
        val desc = mapping.descriptionIndex?.let { cols.getOrNull(it) }.orEmpty()
        val notes = mapping.notesIndex?.let { cols.getOrNull(it) }.orEmpty()

        val entity = TransactionEntity(
            type = txType,
            amount = absAmount,
            personalAmount = absAmount,
            amountInPrimary = absAmount,
            currency = currency,
            date = parsedDate,
            accountId = targetAccountId,
            merchantOrPayee = merchant,
            description = desc,
            notes = notes,
        )
        return AnalyzedImportRow(rowNumber, cols, ImportRowStatus.VALID, null, entity)
    }
}

class DuplicateDetector @Inject constructor(
    private val transactionDao: TransactionDao,
) {
    suspend fun isDuplicate(candidate: TransactionEntity): Boolean {
        val windowMs = 86_400_000L // +/- 1 day
        val matches = transactionDao.findPotentialDuplicates(
            amount = candidate.amount,
            accountId = candidate.accountId,
            startMs = candidate.date - windowMs,
            endMs = candidate.date + windowMs,
        )
        return matches.any { existing ->
            existing.type == candidate.type &&
                (existing.merchantOrPayee.equals(candidate.merchantOrPayee, ignoreCase = true) ||
                    existing.description.equals(candidate.description, ignoreCase = true))
        }
    }
}

class ExecuteImportUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val importHistoryDao: ImportHistoryDao,
    private val duplicateDetector: DuplicateDetector,
) {
    suspend fun analyze(
        table: CsvTable,
        mapping: ColumnMapping,
        targetAccountId: Long,
        currency: String,
    ): List<AnalyzedImportRow> {
        return table.rows.mapIndexed { idx, cols ->
            val validated = ImportValidator.validateRow(idx + 1, cols, mapping, targetAccountId, currency)
            if (validated.status == ImportRowStatus.VALID && validated.parsedTransaction != null) {
                if (duplicateDetector.isDuplicate(validated.parsedTransaction)) {
                    validated.copy(status = ImportRowStatus.DUPLICATE, errorReason = "Possible duplicate transaction")
                } else {
                    validated
                }
            } else {
                validated
            }
        }
    }

    suspend fun commitImport(
        fileName: String,
        analyzedRows: List<AnalyzedImportRow>,
        skipDuplicates: Boolean = true,
        allowPartialImport: Boolean = true,
    ): Result<ImportExecutionResult> {
        val invalidRows = analyzedRows.filter { it.status == ImportRowStatus.INVALID }
        val dupRows = analyzedRows.filter { it.status == ImportRowStatus.DUPLICATE }

        if (!allowPartialImport && invalidRows.isNotEmpty()) {
            return Result.failure(IllegalStateException("${invalidRows.size} invalid rows found (strict mode)"))
        }

        val toInsert = analyzedRows.filter {
            it.status == ImportRowStatus.VALID || (!skipDuplicates && it.status == ImportRowStatus.DUPLICATE)
        }.mapNotNull { it.parsedTransaction }

        return runCatching {
            val status = when {
                toInsert.isEmpty() -> ImportStatus.FAILED
                invalidRows.isNotEmpty() -> ImportStatus.PARTIAL
                else -> ImportStatus.COMPLETED
            }
            val batchId = importHistoryDao.insert(
                ImportHistoryEntity(
                    fileName = fileName,
                    totalRows = analyzedRows.size,
                    successRows = toInsert.size,
                    failedRows = invalidRows.size,
                    duplicateRows = dupRows.size,
                    status = status,
                    canRollback = toInsert.isNotEmpty(),
                )
            )
            if (toInsert.isNotEmpty()) {
                transactionDao.insertAll(toInsert.map { it.copy(importBatchId = batchId) })
            }
            val errorCsv = buildString {
                appendLine("Row,Error,RawData")
                invalidRows.forEach { r ->
                    appendLine("${r.rowNumber},\"${r.errorReason.orEmpty()}\",\"${r.rawColumns.joinToString("|")}\"")
                }
            }
            ImportExecutionResult(
                batchId = batchId,
                totalRows = analyzedRows.size,
                importedCount = toInsert.size,
                failedCount = invalidRows.size,
                duplicateCount = dupRows.size,
                failedCsvText = errorCsv,
            )
        }
    }

    suspend fun rollbackBatch(batchId: Long) {
        transactionDao.softDeleteByImportBatch(batchId)
        importHistoryDao.markRolledBack(batchId)
    }
}


package com.finledger.data.repository

import com.finledger.data.local.dao.AccountDao
import com.finledger.data.local.dao.AuditLogDao
import com.finledger.data.local.dao.CategoryDao
import com.finledger.data.local.dao.SettlementDao
import com.finledger.data.local.dao.TagDao
import com.finledger.data.local.dao.TransactionDao
import com.finledger.data.local.entity.AuditLogEntity
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.TagEntity
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionTagCrossRef
import com.finledger.domain.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    fun observeAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>
    fun observeRecentTransactions(limit: Int = 10): Flow<List<TransactionWithDetails>>
    fun observeTransactionsByAccount(accountId: Long): Flow<List<TransactionWithDetails>>
    fun observeDeletedTransactions(): Flow<List<TransactionEntity>>
    fun observeById(id: Long): Flow<TransactionEntity?>
    suspend fun getById(id: Long): TransactionEntity?
    suspend fun insert(transaction: TransactionEntity, tagIds: List<Long> = emptyList()): Long
    suspend fun update(old: TransactionEntity, updated: TransactionEntity, tagIds: List<Long>? = null)
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long>
}

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>
    suspend fun insert(category: CategoryEntity): Long
    suspend fun update(category: CategoryEntity)
    suspend fun softDeleteCustom(id: Long)
}

interface TagRepository {
    fun observeAll(): Flow<List<TagEntity>>
    suspend fun insert(tag: TagEntity): Long
    suspend fun softDelete(id: Long)
}

interface AuditLogRepository {
    fun observeRecent(limit: Int = 200): Flow<List<AuditLogEntity>>
    fun observeForEntity(entityType: String, entityId: Long): Flow<List<AuditLogEntity>>
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val settlementDao: SettlementDao,
    private val auditLogDao: AuditLogDao,
) : TransactionRepository {

    override fun observeAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>> =
        combineDetails(transactionDao.observeAllTransactions())

    override fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithDetails>> =
        combineDetails(transactionDao.observeRecentTransactions(limit))

    override fun observeTransactionsByAccount(accountId: Long): Flow<List<TransactionWithDetails>> =
        combineDetails(transactionDao.observeByAccount(accountId))

    private fun combineDetails(txFlow: Flow<List<TransactionEntity>>): Flow<List<TransactionWithDetails>> {
        return combine(
            txFlow,
            accountDao.observeActiveAccounts(),
            accountDao.observeArchivedAccounts(),
            categoryDao.observeAll(),
        ) { txs, activeAccounts, archivedAccounts, categories ->
            val accountMap = (activeAccounts + archivedAccounts).associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            txs.map { tx ->
                TransactionWithDetails(
                    transaction = tx,
                    accountName = accountMap[tx.accountId]?.name ?: "Account #${tx.accountId}",
                    toAccountName = tx.toAccountId?.let { accountMap[it]?.name ?: "Account #$it" },
                    category = tx.categoryId?.let { categoryMap[it] },
                )
            }
        }
    }

    override fun observeDeletedTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeDeletedTransactions()

    override fun observeById(id: Long): Flow<TransactionEntity?> =
        transactionDao.observeById(id)

    override suspend fun getById(id: Long): TransactionEntity? =
        transactionDao.getById(id)

    override suspend fun insert(transaction: TransactionEntity, tagIds: List<Long>): Long {
        val id = transactionDao.insert(transaction)
        tagIds.forEach { tagId ->
            tagDao.insertCrossRef(TransactionTagCrossRef(transactionId = id, tagId = tagId))
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "transaction",
                entityId = id,
                action = "CREATE",
                newValue = "${transaction.type.name} ${transaction.amount}",
            )
        )
        return id
    }

    override suspend fun update(old: TransactionEntity, updated: TransactionEntity, tagIds: List<Long>?) {
        val stamped = updated.copy(updatedAt = System.currentTimeMillis())
        transactionDao.update(stamped)
        if (tagIds != null) {
            tagDao.clearTagsForTransaction(updated.id)
            tagIds.forEach { tagId ->
                tagDao.insertCrossRef(TransactionTagCrossRef(transactionId = updated.id, tagId = tagId))
            }
        }
        val changes = buildList {
            if (old.amount != updated.amount) add("amount: ${old.amount} -> ${updated.amount}")
            if (old.accountId != updated.accountId) add("account: ${old.accountId} -> ${updated.accountId}")
            if (old.categoryId != updated.categoryId) add("category: ${old.categoryId} -> ${updated.categoryId}")
            if (old.date != updated.date) add("date changed")
            if (old.merchantOrPayee != updated.merchantOrPayee) add("merchant: ${old.merchantOrPayee} -> ${updated.merchantOrPayee}")
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "transaction",
                entityId = updated.id,
                action = "UPDATE",
                fieldChanged = changes.joinToString("; ").ifEmpty { "details" },
                oldValue = "${old.type.name} ${old.amount}",
                newValue = "${updated.type.name} ${updated.amount}",
            )
        )
    }

    override suspend fun softDelete(id: Long) {
        transactionDao.setDeleted(id, true)
        settlementDao.softDeleteByLinkedTransactionId(id)
        auditLogDao.insert(
            AuditLogEntity(entityType = "transaction", entityId = id, action = "DELETE")
        )
    }

    override suspend fun restore(id: Long) {
        transactionDao.setDeleted(id, false)
        settlementDao.restoreByLinkedTransactionId(id)
        auditLogDao.insert(
            AuditLogEntity(entityType = "transaction", entityId = id, action = "RESTORE")
        )
    }

    override suspend fun getTagIdsForTransaction(transactionId: Long): List<Long> =
        tagDao.getTagIdsForTransaction(transactionId)
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    override fun observeByType(type: CategoryType): Flow<List<CategoryEntity>> = categoryDao.observeByType(type)
    override suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)
    override suspend fun update(category: CategoryEntity) = categoryDao.update(category)
    override suspend fun softDeleteCustom(id: Long) = categoryDao.softDeleteCustom(id)
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {
    override fun observeAll(): Flow<List<TagEntity>> = tagDao.observeAll()
    override suspend fun insert(tag: TagEntity): Long = tagDao.insert(tag)
    override suspend fun softDelete(id: Long) = tagDao.softDelete(id)
}

@Singleton
class AuditLogRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao,
) : AuditLogRepository {
    override fun observeRecent(limit: Int): Flow<List<AuditLogEntity>> = auditLogDao.observeRecent(limit)
    override fun observeForEntity(entityType: String, entityId: Long): Flow<List<AuditLogEntity>> =
        auditLogDao.observeForEntity(entityType, entityId)
}


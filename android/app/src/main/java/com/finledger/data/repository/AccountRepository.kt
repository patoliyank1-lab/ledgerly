package com.finledger.data.repository

import com.finledger.data.local.dao.AccountDao
import com.finledger.data.local.dao.AuditLogDao
import com.finledger.data.local.dao.TransactionDao
import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AuditLogEntity
import com.finledger.domain.model.AccountBalanceCalculator
import com.finledger.domain.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRepository {
    fun observeActiveAccountsWithBalances(): Flow<List<AccountWithBalance>>
    fun observeArchivedAccounts(): Flow<List<AccountEntity>>
    fun observeDeletedAccounts(): Flow<List<AccountEntity>>
    fun observeAccountWithBalance(accountId: Long): Flow<AccountWithBalance?>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun saveAccount(account: AccountEntity): Long
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val auditLogDao: AuditLogDao,
) : AccountRepository {

    override fun observeActiveAccountsWithBalances(): Flow<List<AccountWithBalance>> {
        return combine(
            accountDao.observeActiveAccounts(),
            transactionDao.observeAllTransactions(),
        ) { accounts, transactions ->
            accounts.map { account ->
                AccountWithBalance(
                    account = account,
                    currentBalance = AccountBalanceCalculator.computeBalance(account, transactions),
                )
            }
        }
    }

    override fun observeArchivedAccounts(): Flow<List<AccountEntity>> =
        accountDao.observeArchivedAccounts()

    override fun observeDeletedAccounts(): Flow<List<AccountEntity>> =
        accountDao.observeDeletedAccounts()

    override fun observeAccountWithBalance(accountId: Long): Flow<AccountWithBalance?> {
        return combine(
            accountDao.observeAccountById(accountId),
            transactionDao.observeByAccount(accountId),
        ) { account, transactions ->
            account?.let {
                AccountWithBalance(
                    account = it,
                    currentBalance = AccountBalanceCalculator.computeBalance(it, transactions),
                )
            }
        }
    }

    override suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    override suspend fun saveAccount(account: AccountEntity): Long {
        val isNew = account.id == 0L
        val updated = account.copy(updatedAt = System.currentTimeMillis())
        val id = accountDao.insert(updated)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "account",
                entityId = if (isNew) id else account.id,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = account.name,
            )
        )
        return if (isNew) id else account.id
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        accountDao.setArchived(id, archived)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "account",
                entityId = id,
                action = if (archived) "ARCHIVE" else "UNARCHIVE",
            )
        )
    }

    override suspend fun softDelete(id: Long) {
        accountDao.setDeleted(id, true)
        auditLogDao.insert(
            AuditLogEntity(entityType = "account", entityId = id, action = "DELETE")
        )
    }

    override suspend fun restore(id: Long) {
        accountDao.setDeleted(id, false)
        auditLogDao.insert(
            AuditLogEntity(entityType = "account", entityId = id, action = "RESTORE")
        )
    }
}


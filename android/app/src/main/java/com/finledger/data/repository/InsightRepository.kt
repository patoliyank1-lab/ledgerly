package com.finledger.data.repository

import com.finledger.data.local.dao.AuditLogDao
import com.finledger.data.local.dao.InvestmentDao
import com.finledger.data.local.dao.NetWorthDao
import com.finledger.data.local.entity.AuditLogEntity
import com.finledger.data.local.entity.InvestmentEntity
import com.finledger.data.local.entity.InvestmentTransactionEntity
import com.finledger.data.local.entity.NetWorthSnapshotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val netWorthDao: NetWorthDao,
    private val auditLogDao: AuditLogDao,
) {
    fun observeInvestments(): Flow<List<InvestmentEntity>> = investmentDao.observeAll()
    fun observeInvestment(id: Long): Flow<InvestmentEntity?> = investmentDao.observeById(id)
    fun observeInvestmentTransactions(id: Long): Flow<List<InvestmentTransactionEntity>> =
        investmentDao.observeTransactionsForInvestment(id)

    suspend fun getInvestmentById(id: Long): InvestmentEntity? = investmentDao.getById(id)

    suspend fun saveInvestment(investment: InvestmentEntity): Long {
        val id = investmentDao.insertInvestment(investment)
        auditLogDao.insert(
            AuditLogEntity(entityType = "investment", entityId = id, action = "SAVE", newValue = investment.name)
        )
        return id
    }

    suspend fun insertInvestmentTransaction(tx: InvestmentTransactionEntity): Long =
        investmentDao.insertTransaction(tx)

    suspend fun deleteInvestment(id: Long) = investmentDao.softDelete(id)

    fun observeNetWorthSnapshots(): Flow<List<NetWorthSnapshotEntity>> = netWorthDao.observeSnapshots()
    suspend fun saveSnapshot(snapshot: NetWorthSnapshotEntity): Long = netWorthDao.insertSnapshot(snapshot)
}


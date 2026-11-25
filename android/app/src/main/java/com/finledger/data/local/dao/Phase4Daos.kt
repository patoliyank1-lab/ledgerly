package com.finledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finledger.data.local.entity.InvestmentEntity
import com.finledger.data.local.entity.InvestmentTransactionEntity
import com.finledger.data.local.entity.NetWorthSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<InvestmentEntity?>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getById(id: Long): InvestmentEntity?

    @Query("SELECT * FROM investments WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<InvestmentEntity>

    @Query("SELECT * FROM investments")
    suspend fun getAllRawOnce(): List<InvestmentEntity>

    @Query("SELECT * FROM investment_transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeAllTransactions(): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE isDeleted = 0 AND investmentId = :investmentId ORDER BY date DESC")
    fun observeTransactionsForInvestment(investmentId: Long): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE isDeleted = 0")
    suspend fun getAllTransactionsOnce(): List<InvestmentTransactionEntity>

    @Query("SELECT * FROM investment_transactions")
    suspend fun getAllTransactionsRawOnce(): List<InvestmentTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestments(investments: List<InvestmentEntity>)

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: InvestmentTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<InvestmentTransactionEntity>)

    @Query("UPDATE investments SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE investments SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)
}

@Dao
interface NetWorthDao {
    @Query("SELECT * FROM net_worth_snapshots ORDER BY snapshotDateMs ASC")
    fun observeSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots")
    suspend fun getAllOnce(): List<NetWorthSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: NetWorthSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<NetWorthSnapshotEntity>)
}


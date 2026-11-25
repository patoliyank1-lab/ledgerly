package com.finledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeletedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND (accountId = :accountId OR toAccountId = :accountId) ORDER BY date DESC, createdAt DESC")
    fun observeByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND personId = :personId ORDER BY date DESC, createdAt DESC")
    fun observeByPerson(personId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND date BETWEEN :startMs AND :endMs ORDER BY date DESC, createdAt DESC")
    fun observeInDateRange(startMs: Long, endMs: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllRawOnce(): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE isDeleted = 0
          AND amount = :amount
          AND accountId = :accountId
          AND date BETWEEN :startMs AND :endMs
        LIMIT 5
        """
    )
    suspend fun findPotentialDuplicates(
        amount: Long,
        accountId: Long,
        startMs: Long,
        endMs: Long,
    ): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = :deleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDeleted(id: Long, deleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE importBatchId = :batchId")
    suspend fun softDeleteByImportBatch(batchId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE sharedExpenseId = :sharedExpenseId")
    suspend fun softDeleteBySharedExpenseId(sharedExpenseId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND type = :type AND date BETWEEN :startMs AND :endMs ORDER BY date DESC")
    suspend fun getByTypeInRange(type: TransactionType, startMs: Long, endMs: Long): List<TransactionEntity>
}


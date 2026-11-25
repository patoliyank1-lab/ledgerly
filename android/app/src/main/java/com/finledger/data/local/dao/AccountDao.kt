package com.finledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finledger.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isDeleted = 0 AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 AND isArchived = 1 ORDER BY name ASC")
    fun observeArchivedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeletedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts")
    suspend fun getAllRawOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND isDeleted = 0")
    fun observeAccountById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE isDeleted = 0")
    suspend fun countActive(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET isDeleted = :deleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDeleted(id: Long, deleted: Boolean, updatedAt: Long = System.currentTimeMillis())
}


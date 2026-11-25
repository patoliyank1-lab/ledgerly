package com.finledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finledger.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons WHERE isDeleted = 0 AND isArchived = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllOnce(): List<PersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonEntity>)

    @Update
    suspend fun update(person: PersonEntity)

    @Query("UPDATE persons SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE persons SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE isDeleted = 0 AND status != 'ARCHIVED' ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isDeleted = 0 AND status = 'ARCHIVED' ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): GroupEntity?

    @Query("SELECT * FROM groups WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<GroupEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND isActive = 1")
    fun observeMembers(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE isActive = 1")
    fun observeAllMembers(): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Update
    suspend fun update(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearMembers(groupId: Long)

    @Query("UPDATE groups SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: GroupStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE groups SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isDeleted = 0 AND groupId = :groupId ORDER BY startDate DESC")
    fun observeByGroup(groupId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<EventEntity>

    @Query("SELECT * FROM event_participants WHERE eventId = :eventId AND isActive = 1")
    fun observeParticipants(eventId: Long): Flow<List<EventParticipantEntity>>

    @Query("SELECT * FROM event_participants WHERE isActive = 1")
    fun observeAllParticipants(): Flow<List<EventParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: EventParticipantEntity)

    @Query("DELETE FROM event_participants WHERE eventId = :eventId")
    suspend fun clearParticipants(eventId: Long)

    @Query("UPDATE events SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: EventStatus)

    @Query("UPDATE events SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface SharedExpenseDao {
    @Query("SELECT * FROM shared_expenses WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<SharedExpenseEntity>>

    @Query("SELECT * FROM shared_expenses WHERE isDeleted = 0 AND groupId = :groupId ORDER BY date DESC")
    fun observeByGroup(groupId: Long): Flow<List<SharedExpenseEntity>>

    @Query("SELECT * FROM shared_expenses WHERE isDeleted = 0 AND eventId = :eventId ORDER BY date DESC")
    fun observeByEvent(eventId: Long): Flow<List<SharedExpenseEntity>>

    @Query("SELECT * FROM expense_payers")
    fun observeAllPayers(): Flow<List<ExpensePayerEntity>>

    @Query("SELECT * FROM expense_splits")
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_payers WHERE sharedExpenseId = :expenseId")
    suspend fun getPayersForExpense(expenseId: Long): List<ExpensePayerEntity>

    @Query("SELECT * FROM expense_splits WHERE sharedExpenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: Long): List<ExpenseSplitEntity>

    @Query("SELECT * FROM shared_expenses WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<SharedExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SharedExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayers(payers: List<ExpensePayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SharedExpenseItemEntity>)

    @Query("UPDATE shared_expenses SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE isDeleted = 0 AND personId = :personId ORDER BY date DESC")
    fun observeByPerson(personId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settlement: SettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settlements: List<SettlementEntity>)

    @Query("UPDATE settlements SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SettlementStatus)

    @Query("UPDATE settlements SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}


package com.finledger.data.repository

import com.finledger.data.local.dao.*
import com.finledger.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val personDao: PersonDao,
    private val groupDao: GroupDao,
    private val eventDao: EventDao,
    private val sharedExpenseDao: SharedExpenseDao,
    private val settlementDao: SettlementDao,
    private val transactionDao: TransactionDao,
    private val auditLogDao: AuditLogDao,
) {
    fun observeActivePersons(): Flow<List<PersonEntity>> = personDao.observeActive()
    fun observeAllPersons(): Flow<List<PersonEntity>> = personDao.observeAll()
    fun observePerson(id: Long): Flow<PersonEntity?> = personDao.observeById(id)

    suspend fun savePerson(person: PersonEntity): Long {
        val isNew = person.id == 0L
        val id = if (isNew) {
            personDao.insert(person)
        } else {
            personDao.update(person)
            person.id
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "person",
                entityId = id,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = person.name,
            )
        )
        return id
    }

    suspend fun archivePerson(id: Long, archived: Boolean) = personDao.setArchived(id, archived)
    suspend fun deletePerson(id: Long) = personDao.softDelete(id)

    // Groups
    fun observeActiveGroups(): Flow<List<GroupEntity>> = groupDao.observeActive()
    fun observeArchivedGroups(): Flow<List<GroupEntity>> = groupDao.observeArchived()
    fun observeGroup(id: Long): Flow<GroupEntity?> = groupDao.observeById(id)
    fun observeGroupMembers(groupId: Long): Flow<List<GroupMemberEntity>> = groupDao.observeMembers(groupId)
    fun observeAllGroupMembers(): Flow<List<GroupMemberEntity>> = groupDao.observeAllMembers()

    suspend fun saveGroup(group: GroupEntity, memberPersonIds: List<Long>): Long {
        val isNew = group.id == 0L
        val stamped = group.copy(updatedAt = System.currentTimeMillis())
        val groupId = if (isNew) groupDao.insert(stamped) else {
            groupDao.update(stamped)
            group.id
        }
        groupDao.clearMembers(groupId)
        memberPersonIds.distinct().forEach { personId ->
            groupDao.insertMember(GroupMemberEntity(groupId = groupId, personId = personId))
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "group",
                entityId = groupId,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = group.name,
            )
        )
        return groupId
    }

    suspend fun updateGroupStatus(id: Long, status: GroupStatus) = groupDao.updateStatus(id, status)

    // Events
    fun observeAllEvents(): Flow<List<EventEntity>> = eventDao.observeAll()
    fun observeEventsByGroup(groupId: Long): Flow<List<EventEntity>> = eventDao.observeByGroup(groupId)
    fun observeEvent(id: Long): Flow<EventEntity?> = eventDao.observeById(id)
    fun observeEventParticipants(eventId: Long): Flow<List<EventParticipantEntity>> = eventDao.observeParticipants(eventId)
    fun observeAllEventParticipants(): Flow<List<EventParticipantEntity>> = eventDao.observeAllParticipants()

    suspend fun saveEvent(event: EventEntity, participantPersonIds: List<Long>): Long {
        val isNew = event.id == 0L
        val eventId = if (isNew) eventDao.insert(event) else {
            eventDao.update(event)
            event.id
        }
        eventDao.clearParticipants(eventId)
        participantPersonIds.distinct().forEach { personId ->
            eventDao.insertParticipant(EventParticipantEntity(eventId = eventId, personId = personId))
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "event",
                entityId = eventId,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = event.name,
            )
        )
        return eventId
    }

    suspend fun updateEventStatus(id: Long, status: EventStatus) = eventDao.updateStatus(id, status)

    // Shared expenses & settlements
    fun observeAllSharedExpenses(): Flow<List<SharedExpenseEntity>> = sharedExpenseDao.observeAll()
    fun observeSharedExpensesByGroup(groupId: Long): Flow<List<SharedExpenseEntity>> = sharedExpenseDao.observeByGroup(groupId)
    fun observeSharedExpensesByEvent(eventId: Long): Flow<List<SharedExpenseEntity>> = sharedExpenseDao.observeByEvent(eventId)
    fun observeAllPayers(): Flow<List<ExpensePayerEntity>> = sharedExpenseDao.observeAllPayers()
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>> = sharedExpenseDao.observeAllSplits()

    suspend fun insertSharedExpense(
        expense: SharedExpenseEntity,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
        items: List<SharedExpenseItemEntity>,
    ): Long {
        val expenseId = sharedExpenseDao.insertExpense(expense)
        sharedExpenseDao.insertPayers(payers.map { it.copy(sharedExpenseId = expenseId) })
        sharedExpenseDao.insertSplits(splits.map { it.copy(sharedExpenseId = expenseId) })
        if (items.isNotEmpty()) {
            sharedExpenseDao.insertItems(items.map { it.copy(sharedExpenseId = expenseId) })
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "shared_expense",
                entityId = expenseId,
                action = "CREATE",
                newValue = "${expense.description} (${expense.totalAmount})",
            )
        )
        return expenseId
    }

    suspend fun deleteSharedExpense(id: Long) {
        sharedExpenseDao.softDelete(id)
        transactionDao.softDeleteBySharedExpenseId(id)
        auditLogDao.insert(
            AuditLogEntity(entityType = "shared_expense", entityId = id, action = "DELETE")
        )
    }

    fun observeAllSettlements(): Flow<List<SettlementEntity>> = settlementDao.observeAll()
    fun observeSettlementsByPerson(personId: Long): Flow<List<SettlementEntity>> = settlementDao.observeByPerson(personId)

    suspend fun insertSettlement(settlement: SettlementEntity): Long {
        val id = settlementDao.insert(settlement)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "settlement",
                entityId = id,
                action = "CREATE",
                newValue = "${settlement.direction.name} ${settlement.amount}",
            )
        )
        return id
    }

    suspend fun deleteSettlement(settlement: SettlementEntity) {
        settlementDao.softDelete(settlement.id)
        settlement.linkedTransactionId?.let { txId ->
            transactionDao.setDeleted(txId, true)
        }
        auditLogDao.insert(
            AuditLogEntity(entityType = "settlement", entityId = settlement.id, action = "DELETE")
        )
    }
}


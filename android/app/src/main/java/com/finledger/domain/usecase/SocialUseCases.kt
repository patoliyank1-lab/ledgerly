package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.domain.model.TransactionWithDetails
import javax.inject.Inject

data class FriendPaidShareEntry(
    val expense: SharedExpenseEntity,
    val userSharePaise: Long,
)

object FriendPaidSharedExpenseHelper {
    fun resolveFriendPaidShares(
        transactions: List<TransactionWithDetails>,
        sharedExpenses: List<SharedExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
    ): List<FriendPaidShareEntry> {
        val recordedSharedIds = transactions.mapNotNull {
            val tx = it.transaction
            if (!tx.isDeleted && tx.status != TransactionStatus.CANCELLED) tx.sharedExpenseId else null
        }.toSet()

        val splitsByExp = splits.groupBy { it.sharedExpenseId }
        return sharedExpenses.mapNotNull { exp ->
            if (exp.isDeleted || exp.id in recordedSharedIds) return@mapNotNull null
            val userShare = splitsByExp[exp.id].orEmpty()
                .filter { !it.isExcluded && it.personId == SplitCalculator.USER_PERSON_ID }
                .sumOf { it.shareAmount }
            if (userShare > 0L) FriendPaidShareEntry(exp, userShare) else null
        }
    }
}

class AddPersonUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
) {
    suspend operator fun invoke(
        existing: PersonEntity? = null,
        name: String,
        phone: String = "",
        email: String = "",
        relationship: String = "Friend",
        notes: String = "",
    ): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val entity = (existing ?: PersonEntity(name = name)).copy(
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim(),
            relationship = relationship.trim().ifEmpty { "Friend" },
            notes = notes.trim(),
        )
        return runCatching { socialRepository.savePerson(entity) }
    }
}

class CreateGroupUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
) {
    suspend operator fun invoke(
        existing: GroupEntity? = null,
        name: String,
        description: String = "",
        currency: String = "INR",
        spendingLimitPaise: Long? = null,
        memberPersonIds: List<Long>,
    ): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }
        val entity = (existing ?: GroupEntity(name = name)).copy(
            name = name.trim(),
            description = description.trim(),
            currency = currency,
            spendingLimit = spendingLimitPaise,
        )
        return runCatching { socialRepository.saveGroup(entity, memberPersonIds) }
    }
}

class CreateEventUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
) {
    suspend operator fun invoke(
        existing: EventEntity? = null,
        name: String,
        description: String = "",
        location: String = "",
        groupId: Long? = null,
        startDateMs: Long,
        endDateMs: Long? = null,
        budgetAmountPaise: Long? = null,
        participantPersonIds: List<Long>,
    ): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Event name cannot be empty"))
        }
        val entity = (existing ?: EventEntity(name = name, startDate = startDateMs)).copy(
            name = name.trim(),
            description = description.trim(),
            location = location.trim(),
            groupId = groupId,
            startDate = startDateMs,
            endDate = endDateMs,
            budgetAmount = budgetAmountPaise,
        )
        return runCatching { socialRepository.saveEvent(entity, participantPersonIds) }
    }
}

/**
 * Records a shared expense paid by one payer (P2-13) and enforces FINANCIAL_RULES.md §1:
 * If user paid, full paid amount leaves user's account, while personalAmount = user's split share only.
 */
class AddSharedExpenseUseCase @Inject constructor(
    private val addMultiPayerExpenseUseCase: AddMultiPayerExpenseUseCase,
) {
    suspend operator fun invoke(
        totalAmountPaise: Long,
        description: String,
        dateMs: Long,
        categoryId: Long?,
        groupId: Long?,
        eventId: Long?,
        splitMethod: SplitMethod,
        payerPersonId: Long,
        userAccountIdIfUserPaid: Long?,
        splits: List<ExpenseSplitEntity>,
        items: List<SharedExpenseItemEntity> = emptyList(),
        notes: String = "",
    ): Result<Long> {
        return addMultiPayerExpenseUseCase(
            totalAmountPaise = totalAmountPaise,
            description = description,
            dateMs = dateMs,
            categoryId = categoryId,
            groupId = groupId,
            eventId = eventId,
            splitMethod = splitMethod,
            payers = listOf(ExpensePayerEntity(sharedExpenseId = 0L, personId = payerPersonId, amountPaid = totalAmountPaise)),
            userAccountIdIfUserPaid = userAccountIdIfUserPaid,
            splits = splits,
            items = items,
            notes = notes,
        )
    }
}

/**
 * Records a shared expense paid by one or more contributors (P2-15).
 */
class AddMultiPayerExpenseUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        totalAmountPaise: Long,
        description: String,
        dateMs: Long,
        categoryId: Long?,
        groupId: Long?,
        eventId: Long?,
        splitMethod: SplitMethod,
        payers: List<ExpensePayerEntity>,
        userAccountIdIfUserPaid: Long?,
        splits: List<ExpenseSplitEntity>,
        items: List<SharedExpenseItemEntity> = emptyList(),
        notes: String = "",
    ): Result<Long> {
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }
        SplitValidator.validate(totalAmountPaise, payers, splits).onFailure { return Result.failure(it) }

        val userPaid = payers.firstOrNull { it.personId == SplitCalculator.USER_PERSON_ID }?.amountPaid ?: 0L
        val userPersonalShare = PersonalExpenseValidator.resolveUserPersonalShare(splits)

        if (userPaid > 0L && userAccountIdIfUserPaid == null) {
            return Result.failure(IllegalArgumentException("Select the account you paid from"))
        }

        return runCatching {
            val sharedId = socialRepository.insertSharedExpense(
                expense = SharedExpenseEntity(
                    totalAmount = totalAmountPaise,
                    description = description.trim(),
                    date = dateMs,
                    categoryId = categoryId,
                    eventId = eventId,
                    groupId = groupId,
                    splitMethod = splitMethod,
                    notes = notes.trim(),
                ),
                payers = payers,
                splits = splits,
                items = items,
            )

            // Record user's financial impact in TransactionEntity:
            // If user paid > 0, account decreases by userPaid while personal spending counts userPersonalShare only.
            // If friend paid and user has a share > 0, no cash left user's account (account balance untouched),
            // though user's share is tracked in SharedExpense + ComputePersonBalanceUseCase.
            if (userPaid > 0L && userAccountIdIfUserPaid != null) {
                transactionRepository.insert(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = userPaid,
                        personalAmount = userPersonalShare,
                        amountInPrimary = userPaid,
                        date = dateMs,
                        accountId = userAccountIdIfUserPaid,
                        categoryId = categoryId,
                        description = description.trim(),
                        notes = notes.trim(),
                        isPersonal = false,
                        sharedExpenseId = sharedId,
                        groupId = groupId,
                        eventId = eventId,
                    )
                )
            }
            sharedId
        }
    }
}


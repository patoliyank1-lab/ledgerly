package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("date"),
        Index("type"),
        Index("personId"),
        Index("groupId"),
        Index("eventId"),
        Index("sharedExpenseId"),
        Index("isDeleted"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Long, // paise, actual account cashflow (always positive)
    val personalAmount: Long = amount, // paise, user's own share for expenses
    val adjustmentSign: Int = 1, // +1 credit / -1 debit for ACCOUNT_ADJUSTMENT
    val currency: String = "INR",
    val exchangeRate: Double = 1.0,
    val amountInPrimary: Long = amount,
    val date: Long, // epoch ms
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val merchantOrPayee: String = "",
    val location: String = "",
    val paymentMethod: String = "",
    val description: String = "",
    val notes: String = "",
    val discountAmount: Long = 0,
    val taxAmount: Long = 0,
    val tipAmount: Long = 0,
    val feeAmount: Long = 0,
    val warrantyEndDate: Long? = null,
    val returnDeadlineDate: Long? = null,
    val isPersonal: Boolean = true,
    val sharedExpenseId: Long? = null,
    val personId: Long? = null,
    val groupId: Long? = null,
    val eventId: Long? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val parentTransactionId: Long? = null,
    val importBatchId: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)


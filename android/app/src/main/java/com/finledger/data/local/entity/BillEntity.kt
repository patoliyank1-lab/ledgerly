package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "bills",
    indices = [Index("isActive"), Index("isDeleted")],
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val provider: String = "",
    val type: BillType,
    val expectedAmount: Long, // paise
    val currency: String = "INR",
    val dueDay: Int, // 1-31
    val nextDueDateMs: Long = System.currentTimeMillis(),
    val frequency: BillFrequency,
    val paymentAccountId: Long? = null,
    val categoryId: Long? = null,
    val reminderDaysBefore: Int = 3,
    val isActive: Boolean = true,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "bill_payments",
    indices = [Index("billId"), Index("dueDate"), Index("isDeleted")],
)
data class BillPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val dueDate: Long,
    val paidDate: Long? = null,
    val amount: Long,
    val isPaid: Boolean = false,
    val transactionId: Long? = null,
    val receiptUri: String = "",
    val isDeleted: Boolean = false,
)


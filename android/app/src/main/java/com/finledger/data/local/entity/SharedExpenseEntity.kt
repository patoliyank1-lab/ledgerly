package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Shared expense header.
 * Note: personId = 0L represents the app user ("You") inside ExpensePayerEntity and ExpenseSplitEntity.
 */
@Serializable
@Entity(
    tableName = "shared_expenses",
    indices = [Index("groupId"), Index("eventId"), Index("date"), Index("isDeleted")],
)
data class SharedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalAmount: Long, // paise
    val currency: String = "INR",
    val description: String,
    val date: Long,
    val categoryId: Long? = null,
    val eventId: Long? = null,
    val groupId: Long? = null,
    val splitMethod: SplitMethod,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "expense_payers",
    primaryKeys = ["sharedExpenseId", "personId"],
    indices = [Index("personId")],
)
data class ExpensePayerEntity(
    val sharedExpenseId: Long,
    val personId: Long, // 0L = You
    val amountPaid: Long, // paise
)

@Serializable
@Entity(
    tableName = "expense_splits",
    indices = [Index("sharedExpenseId"), Index("personId")],
)
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedExpenseId: Long,
    val personId: Long, // 0L = You
    val shareAmount: Long, // paise
    val sharePercentage: Double? = null,
    val shareWeight: Double? = null,
    val isExcluded: Boolean = false,
    val isPaid: Boolean = false,
)

@Serializable
@Entity(
    tableName = "shared_expense_items",
    indices = [Index("sharedExpenseId")],
)
data class SharedExpenseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedExpenseId: Long,
    val itemName: String,
    val amount: Long, // paise
    val assignedPersonIdsCsv: String, // e.g., "0,1,2"
)


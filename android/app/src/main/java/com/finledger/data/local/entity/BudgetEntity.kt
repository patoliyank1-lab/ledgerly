package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "budgets",
    indices = [Index("categoryId"), Index("isActive"), Index("isDeleted")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long, // paise
    val currency: String = "INR",
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val groupId: Long? = null,
    val eventId: Long? = null,
    val alertThreshold: Int = 80,
    val isRollover: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "savings_goals",
    indices = [Index("status"), Index("isDeleted")],
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val targetAmount: Long, // paise
    val currentAmount: Long = 0L, // paise
    val currency: String = "INR",
    val targetDate: Long? = null,
    val accountId: Long? = null,
    val iconName: String = "savings",
    val colorHex: String = "#0066CC",
    val status: GoalStatus = GoalStatus.IN_PROGRESS,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)


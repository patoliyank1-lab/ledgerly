package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val bankOrProvider: String = "",
    val maskedIdentifier: String = "",
    val currency: String = "INR",
    val openingBalance: Long = 0, // paise
    val openingDate: Long = System.currentTimeMillis(),
    val creditLimit: Long? = null, // paise, credit cards only
    val billingCycleDay: Int? = null, // 1-31
    val dueDay: Int? = null, // 1-31
    val minimumBalanceAlert: Long? = null, // paise
    val notes: String = "",
    val colorHex: String = "#0066CC",
    val iconName: String = "wallet",
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)


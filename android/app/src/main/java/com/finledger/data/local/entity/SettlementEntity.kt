package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "settlements",
    indices = [Index("personId"), Index("groupId"), Index("eventId"), Index("date"), Index("isDeleted")],
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val amount: Long, // paise
    val currency: String = "INR",
    val direction: SettlementDirection,
    val date: Long,
    val accountId: Long? = null,
    val paymentMethod: String = "",
    val referenceNumber: String = "",
    val proofDocumentUri: String = "",
    val notes: String = "",
    val status: SettlementStatus = SettlementStatus.CONFIRMED,
    val groupId: Long? = null,
    val eventId: Long? = null,
    val linkedTransactionId: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)


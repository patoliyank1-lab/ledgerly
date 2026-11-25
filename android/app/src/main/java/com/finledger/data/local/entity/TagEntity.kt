package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#7A7A7A",
    val isDeleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"],
    indices = [Index("tagId")],
)
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long,
)

@Serializable
@Entity(
    tableName = "audit_log",
    indices = [Index("entityType", "entityId"), Index("performedAt")],
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val action: String,
    val fieldChanged: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val performedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
)


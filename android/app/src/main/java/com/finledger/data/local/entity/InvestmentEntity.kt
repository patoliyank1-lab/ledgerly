package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "investments",
    indices = [Index("type"), Index("status"), Index("isDeleted")],
)
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val platform: String = "",
    val accountId: Long? = null,
    val symbol: String = "",
    val currentUnits: Double = 0.0,
    val averageBuyPrice: Long = 0L, // paise per unit
    val currentPrice: Long = 0L, // paise per unit
    val realizedGainPaise: Long = 0L,
    val lastPriceUpdateDate: Long? = null,
    val maturityDate: Long? = null,
    val maturityAmount: Long? = null,
    val notes: String = "",
    val status: InvestmentStatus = InvestmentStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "investment_transactions",
    indices = [Index("investmentId"), Index("date"), Index("isDeleted")],
)
data class InvestmentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investmentId: Long,
    val type: InvestmentTransactionType,
    val date: Long,
    val units: Double,
    val pricePerUnit: Long, // paise
    val totalAmount: Long, // paise
    val fees: Long = 0L,
    val taxes: Long = 0L,
    val accountId: Long? = null,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "net_worth_snapshots",
    indices = [Index("snapshotDateMs")],
)
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotDateMs: Long,
    val totalAssetsPaise: Long,
    val totalLiabilitiesPaise: Long,
    val netWorthPaise: Long,
)


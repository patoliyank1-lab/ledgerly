package com.finledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "loans",
    indices = [Index("personId"), Index("type"), Index("status"), Index("isDeleted")],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LoanType,
    val personOrOrg: String,
    val personId: Long? = null,
    val principalAmount: Long, // paise
    val currency: String = "INR",
    val dateGiven: Long,
    val expectedRepaymentDate: Long? = null,
    val interestRate: Double = 0.0,
    val interestType: String = "NONE",
    val repaymentFrequency: String = "NONE",
    val guarantorName: String = "",
    val accountId: Long? = null,
    val notes: String = "",
    val agreementDocumentUri: String = "",
    val status: LoanStatus = LoanStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "loan_repayments",
    indices = [Index("loanId"), Index("date"), Index("isDeleted")],
)
data class LoanRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val date: Long,
    val principalAmount: Long, // paise
    val interestAmount: Long = 0L,
    val feeAmount: Long = 0L,
    val accountId: Long? = null,
    val notes: String = "",
    val proofDocumentUri: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)


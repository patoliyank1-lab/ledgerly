package com.finledger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finledger.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY dateGiven DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<LoanEntity>

    @Query("SELECT * FROM loans")
    suspend fun getAllRawOnce(): List<LoanEntity>

    @Query("SELECT * FROM loan_repayments WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeAllRepayments(): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM loan_repayments WHERE isDeleted = 0 AND loanId = :loanId ORDER BY date DESC")
    fun observeRepaymentsForLoan(loanId: Long): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM loan_repayments WHERE isDeleted = 0 AND loanId = :loanId")
    suspend fun getRepaymentsForLoanOnce(loanId: Long): List<LoanRepaymentEntity>

    @Query("SELECT * FROM loan_repayments WHERE isDeleted = 0")
    suspend fun getAllRepaymentsOnce(): List<LoanRepaymentEntity>

    @Query("SELECT * FROM loan_repayments")
    suspend fun getAllRepaymentsRawOnce(): List<LoanRepaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayments(repayments: List<LoanRepaymentEntity>)

    @Query("UPDATE loans SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: LoanStatus)

    @Query("UPDATE loans SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE isDeleted = 0 AND isActive = 1 ORDER BY nextDueDateMs ASC")
    fun observeActive(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<BillEntity>

    @Query("SELECT * FROM bills")
    suspend fun getAllRawOnce(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): BillEntity?

    @Query("SELECT * FROM bill_payments WHERE isDeleted = 0 ORDER BY paidDate DESC, dueDate DESC")
    fun observeAllPayments(): Flow<List<BillPaymentEntity>>

    @Query("SELECT * FROM bill_payments WHERE isDeleted = 0")
    suspend fun getAllPaymentsOnce(): List<BillPaymentEntity>

    @Query("SELECT * FROM bill_payments")
    suspend fun getAllPaymentsRawOnce(): List<BillPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillEntity>)

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: BillPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<BillPaymentEntity>)

    @Query("UPDATE bills SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isDeleted = 0 AND isActive = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAllRawOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("UPDATE budgets SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE isDeleted = 0")
    suspend fun getAllOnce(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllRawOnce(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}


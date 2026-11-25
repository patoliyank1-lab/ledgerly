package com.finledger.data.repository

import com.finledger.data.local.dao.*
import com.finledger.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommitmentRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val auditLogDao: AuditLogDao,
) {
    // Loans
    fun observeAllLoans(): Flow<List<LoanEntity>> = loanDao.observeAll()
    fun observeLoan(id: Long): Flow<LoanEntity?> = loanDao.observeById(id)
    fun observeAllRepayments(): Flow<List<LoanRepaymentEntity>> = loanDao.observeAllRepayments()
    fun observeRepaymentsForLoan(loanId: Long): Flow<List<LoanRepaymentEntity>> = loanDao.observeRepaymentsForLoan(loanId)

    suspend fun getLoanById(id: Long): LoanEntity? = loanDao.getById(id)
    suspend fun getRepaymentsForLoanOnce(loanId: Long): List<LoanRepaymentEntity> = loanDao.getRepaymentsForLoanOnce(loanId)

    suspend fun insertLoan(loan: LoanEntity): Long {
        val id = loanDao.insertLoan(loan)
        auditLogDao.insert(
            AuditLogEntity(entityType = "loan", entityId = id, action = "CREATE", newValue = "${loan.type.name} ${loan.principalAmount}")
        )
        return id
    }

    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long {
        val id = loanDao.insertRepayment(repayment)
        auditLogDao.insert(
            AuditLogEntity(entityType = "loan_repayment", entityId = id, action = "CREATE", newValue = "principal ${repayment.principalAmount}")
        )
        return id
    }

    suspend fun updateLoanStatus(loanId: Long, status: LoanStatus) = loanDao.updateStatus(loanId, status)
    suspend fun deleteLoan(id: Long) = loanDao.softDelete(id)
    suspend fun restoreLoan(id: Long) = loanDao.restore(id)

    // Bills
    fun observeActiveBills(): Flow<List<BillEntity>> = billDao.observeActive()
    fun observeAllBillPayments(): Flow<List<BillPaymentEntity>> = billDao.observeAllPayments()
    suspend fun getBillById(id: Long): BillEntity? = billDao.getById(id)

    suspend fun saveBill(bill: BillEntity): Long {
        val isNew = bill.id == 0L
        val id = if (isNew) {
            billDao.insertBill(bill)
        } else {
            billDao.updateBill(bill)
            bill.id
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "bill",
                entityId = id,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = bill.name,
            )
        )
        return id
    }

    suspend fun insertBillPayment(payment: BillPaymentEntity): Long = billDao.insertPayment(payment)
    suspend fun deleteBill(id: Long) = billDao.softDelete(id)
    suspend fun restoreBill(id: Long) = billDao.restore(id)

    // Budgets
    fun observeActiveBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeActive()
    suspend fun saveBudget(budget: BudgetEntity): Long {
        val isNew = budget.id == 0L
        val id = if (isNew) {
            budgetDao.insert(budget)
        } else {
            budgetDao.update(budget)
            budget.id
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "budget",
                entityId = id,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = budget.name,
            )
        )
        return id
    }
    suspend fun deleteBudget(id: Long) = budgetDao.softDelete(id)
    suspend fun restoreBudget(id: Long) = budgetDao.restore(id)

    // Savings goals
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>> = savingsGoalDao.observeAll()
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity? = savingsGoalDao.getById(id)
    suspend fun saveSavingsGoal(goal: SavingsGoalEntity): Long {
        val isNew = goal.id == 0L
        val id = if (isNew) {
            savingsGoalDao.insert(goal)
        } else {
            savingsGoalDao.update(goal)
            goal.id
        }
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "savings_goal",
                entityId = id,
                action = if (isNew) "CREATE" else "UPDATE",
                newValue = goal.name,
            )
        )
        return id
    }
    suspend fun deleteSavingsGoal(id: Long) = savingsGoalDao.softDelete(id)
    suspend fun restoreSavingsGoal(id: Long) = savingsGoalDao.restore(id)
}


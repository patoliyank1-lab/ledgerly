package com.finledger.domain.model

import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AccountType
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionStatus
import com.finledger.data.local.entity.TransactionType

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalance: Long, // For CREDIT_CARD this is outstanding liability; for assets this is available/asset balance
) {
    val availableCredit: Long?
        get() = if (account.type == AccountType.CREDIT_CARD && account.creditLimit != null) {
            (account.creditLimit - currentBalance).coerceAtLeast(0L)
        } else {
            null
        }
}

data class AccountSummary(
    val totalAvailableCashAndBank: Long,
    val cashBalance: Long,
    val bankBalance: Long,
    val walletBalance: Long,
    val creditCardOutstanding: Long,
    val totalCreditLimit: Long,
    val investmentAccountBalance: Long,
)

data class MonthlySummary(
    val monthStartMs: Long,
    val monthEndMs: Long,
    val totalIncome: Long,
    val totalPersonalExpense: Long,
    val savings: Long,
    val savingsRatePercent: Int,
)

data class TransactionFilter(
    val searchQuery: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val startDateMs: Long? = null,
    val endDateMs: Long? = null,
)

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val accountName: String,
    val toAccountName: String? = null,
    val category: CategoryEntity? = null,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val userName: String = "Personal",
    val primaryCurrency: String = "INR",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val financialYearStartMonth: Int = 4, // April
    val onboardingCompleted: Boolean = false,
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val loanRemindersEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = false,
)

/**
 * Pure financial balance calculator enforcing all rules in FINANCIAL_RULES.md.
 */
object AccountBalanceCalculator {

    fun computeBalance(
        account: AccountEntity,
        transactions: List<TransactionEntity>,
    ): Long {
        val activeTxs = transactions.filter {
            !it.isDeleted && it.status != TransactionStatus.CANCELLED
        }

        if (account.type == AccountType.CREDIT_CARD) {
            // Credit card: openingBalance = initial outstanding debt.
            // Expenses on the card increase liability; refunds and incoming bill-payment transfers reduce it.
            var outstanding = account.openingBalance
            for (tx in activeTxs) {
                when {
                    tx.accountId == account.id && tx.type == TransactionType.EXPENSE -> {
                        outstanding += tx.amount
                    }
                    tx.accountId == account.id && tx.type == TransactionType.REFUND -> {
                        outstanding -= tx.amount
                    }
                    tx.toAccountId == account.id && tx.type == TransactionType.TRANSFER -> {
                        outstanding -= tx.amount
                    }
                    tx.accountId == account.id && tx.type == TransactionType.TRANSFER -> {
                        outstanding += tx.amount
                    }
                    tx.accountId == account.id && tx.type == TransactionType.ACCOUNT_ADJUSTMENT -> {
                        outstanding += tx.amount * tx.adjustmentSign
                    }
                }
            }
            return outstanding
        }

        // Standard asset / cash / bank / wallet / investment account
        var balance = account.openingBalance
        for (tx in activeTxs) {
            if (tx.accountId == account.id) {
                when (tx.type) {
                    TransactionType.INCOME,
                    TransactionType.REFUND,
                    TransactionType.BORROWING,
                    TransactionType.REPAYMENT_RECEIVED,
                    TransactionType.INVESTMENT_SELL -> balance += tx.amount

                    TransactionType.EXPENSE,
                    TransactionType.TRANSFER,
                    TransactionType.LENDING,
                    TransactionType.REPAYMENT_MADE,
                    TransactionType.INVESTMENT_BUY -> balance -= tx.amount

                    TransactionType.ACCOUNT_ADJUSTMENT -> balance += tx.amount * tx.adjustmentSign
                }
            }
            if (tx.toAccountId == account.id && tx.type == TransactionType.TRANSFER) {
                balance += tx.amount
            }
        }
        return balance
    }

    fun summarizeAccounts(accounts: List<AccountWithBalance>): AccountSummary {
        var cash = 0L
        var bank = 0L
        var wallet = 0L
        var ccOutstanding = 0L
        var ccLimit = 0L
        var investments = 0L

        for (item in accounts) {
            when (item.account.type) {
                AccountType.CASH -> cash += item.currentBalance
                AccountType.SAVINGS, AccountType.CURRENT, AccountType.DEBIT_CARD -> bank += item.currentBalance
                AccountType.DIGITAL_WALLET, AccountType.UPI -> wallet += item.currentBalance
                AccountType.CREDIT_CARD -> {
                    ccOutstanding += item.currentBalance
                    ccLimit += item.account.creditLimit ?: 0L
                }
                AccountType.FIXED_DEPOSIT, AccountType.INVESTMENT, AccountType.OTHER -> {
                    investments += item.currentBalance
                }
            }
        }

        return AccountSummary(
            totalAvailableCashAndBank = cash + bank + wallet,
            cashBalance = cash,
            bankBalance = bank,
            walletBalance = wallet,
            creditCardOutstanding = ccOutstanding,
            totalCreditLimit = ccLimit,
            investmentAccountBalance = investments,
        )
    }
}


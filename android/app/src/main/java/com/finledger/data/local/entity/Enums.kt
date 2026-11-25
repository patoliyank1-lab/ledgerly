package com.finledger.data.local.entity

enum class AccountType(val displayName: String, val isLiability: Boolean = false, val isInvestmentOrAsset: Boolean = false) {
    CASH("Cash & Physical Wallet"),
    SAVINGS("Savings Account"),
    CURRENT("Current Account"),
    DIGITAL_WALLET("Digital Wallet"),
    UPI("UPI Account"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card", isLiability = true),
    FIXED_DEPOSIT("Fixed Deposit", isInvestmentOrAsset = true),
    INVESTMENT("Investment / Brokerage", isInvestmentOrAsset = true),
    OTHER("Other Asset", isInvestmentOrAsset = true),
}

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    REFUND("Refund"),
    LENDING("Lending"),
    BORROWING("Borrowing"),
    REPAYMENT_RECEIVED("Repayment Received"),
    REPAYMENT_MADE("Repayment Made"),
    INVESTMENT_BUY("Investment Buy"),
    INVESTMENT_SELL("Investment Sell"),
    ACCOUNT_ADJUSTMENT("Account Adjustment"),
}

enum class TransactionStatus { PENDING, COMPLETED, CANCELLED }

enum class CategoryType { EXPENSE, INCOME, BOTH }

enum class GroupStatus { ACTIVE, ARCHIVED, CLOSED }

enum class EventStatus { OPEN, COMPLETED, ARCHIVED }

enum class SplitMethod(val displayName: String) {
    EQUAL("Equal"),
    CUSTOM_AMOUNT("Custom Amount"),
    PERCENTAGE("Percentage"),
    SHARES("Shares / Weights"),
    ITEM("By Item"),
}

enum class SettlementDirection { RECEIVED, PAID }

enum class SettlementStatus { PENDING, CONFIRMED, DISPUTED, CANCELLED }

enum class LoanType { LENT, BORROWED }

enum class LoanStatus { ACTIVE, PARTIALLY_REPAID, CLOSED }

enum class BillType(val displayName: String) {
    ELECTRICITY("Electricity"),
    WATER("Water"),
    INTERNET("Internet"),
    MOBILE("Mobile"),
    RENT("Rent"),
    INSURANCE("Insurance"),
    CREDIT_CARD("Credit Card"),
    EMI("EMI"),
    OTT("OTT Subscription"),
    SOFTWARE("Software Subscription"),
    OTHER("Other"),
}

enum class BillFrequency(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly"),
    CUSTOM("Custom"),
}

enum class BudgetPeriod(val displayName: String) {
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom"),
}

enum class GoalStatus { IN_PROGRESS, COMPLETED, ABANDONED }

enum class InvestmentType(val displayName: String) {
    STOCK("Stock"),
    MUTUAL_FUND("Mutual Fund"),
    SIP("SIP"),
    FIXED_DEPOSIT("Fixed Deposit"),
    GOLD("Gold"),
    REAL_ESTATE("Real Estate"),
    BOND("Bond"),
    CRYPTO("Cryptocurrency"),
    OTHER("Other"),
}

enum class InvestmentTransactionType { BUY, SELL, DIVIDEND, SIP }

enum class InvestmentStatus { ACTIVE, SOLD, MATURED }

enum class DocumentType(val displayName: String) {
    RECEIPT("Receipt"),
    INVOICE("Invoice"),
    BANK_STATEMENT("Bank Statement"),
    LOAN_AGREEMENT("Loan Agreement"),
    REPAYMENT_PROOF("Repayment Proof"),
    INSURANCE("Insurance"),
    INVESTMENT_STATEMENT("Investment Statement"),
    TAX_DOCUMENT("Tax Document"),
    WARRANTY("Warranty"),
    OTHER("Other"),
}

enum class ImportStatus { COMPLETED, PARTIAL, FAILED, ROLLED_BACK }


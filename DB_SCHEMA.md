# FinLedger — Room Database Schema

> [!IMPORTANT]
> - All amounts stored as `Long` (paise = 1/100 rupee). Never use `Float` or `Double` for money.
> - All timestamps as `Long` (epoch milliseconds). Use `System.currentTimeMillis()`.
> - Soft delete via `isDeleted: Boolean = false`. Never permanently delete financial records.
> - Foreign keys enforced with `onDelete = ForeignKey.RESTRICT` to prevent orphaned records.

---

## Core Entities (Phase 1)

### AccountEntity
```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,          // CASH, SAVINGS, CURRENT, DIGITAL_WALLET,
                                    // DEBIT_CARD, CREDIT_CARD, FIXED_DEPOSIT,
                                    // INVESTMENT, OTHER
    val bankOrProvider: String = "",
    val maskedIdentifier: String = "",  // last 4 digits
    val currency: String = "INR",
    val openingBalance: Long = 0,   // in paise
    val openingDate: Long = System.currentTimeMillis(),
    val creditLimit: Long? = null,  // paise, credit cards only
    val billingCycleDay: Int? = null, // 1-31, credit cards
    val notes: String = "",
    val colorHex: String = "#0066CC",
    val iconName: String = "wallet",
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

### TransactionEntity
```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"],
                   childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"],
                   childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,      // EXPENSE, INCOME, TRANSFER, REFUND,
                                    // LENDING, BORROWING, REPAYMENT_RECEIVED,
                                    // REPAYMENT_MADE, INVESTMENT_BUY, INVESTMENT_SELL,
                                    // ACCOUNT_ADJUSTMENT
    val amount: Long,               // paise, always positive
    val currency: String = "INR",
    val exchangeRate: Double = 1.0, // for multi-currency
    val amountInPrimary: Long = amount, // paise in user's primary currency
    val date: Long,                 // epoch ms
    val accountId: Long,
    val toAccountId: Long? = null,  // for TRANSFER type
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val merchantOrPayee: String = "",
    val location: String = "",
    val paymentMethod: String = "",
    val description: String = "",
    val notes: String = "",
    val isPersonal: Boolean = true,
    val sharedExpenseId: Long? = null, // links to SharedExpenseEntity
    val personId: Long? = null,     // for lending/borrowing/settlement
    val groupId: Long? = null,
    val eventId: Long? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED, // PENDING, COMPLETED, CANCELLED
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val parentTransactionId: Long? = null, // for refund links
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

### CategoryEntity
```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: CategoryType,         // EXPENSE, INCOME, BOTH
    val parentId: Long? = null,     // null = top-level category
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0,
)
```

### TagEntity
```kotlin
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#7A7A7A",
    val isDeleted: Boolean = false,
)

@Entity(tableName = "transaction_tags",
        primaryKeys = ["transactionId", "tagId"])
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long,
)
```

### AuditLogEntity
```kotlin
@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,         // "transaction", "account", "loan", etc.
    val entityId: Long,
    val action: String,             // "CREATE", "UPDATE", "DELETE", "RESTORE"
    val fieldChanged: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val performedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
)
```

---

## Phase 2 Entities (Friends & Groups)

### PersonEntity
```kotlin
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val relationship: String = "",  // "Friend", "Family", "Colleague", etc.
    val notes: String = "",
    val profileImageUri: String = "",
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

### GroupEntity
```kotlin
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val imageUri: String = "",
    val currency: String = "INR",
    val status: GroupStatus = GroupStatus.ACTIVE, // ACTIVE, ARCHIVED, CLOSED
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "group_members",
        primaryKeys = ["groupId", "personId"])
data class GroupMemberEntity(
    val groupId: Long,
    val personId: Long,
    val role: String = "MEMBER",    // "ADMIN", "MEMBER"
    val joinedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
)
```

### EventEntity
```kotlin
@Entity(tableName = "events",
        foreignKeys = [
            ForeignKey(entity = GroupEntity::class, parentColumns = ["id"],
                       childColumns = ["groupId"], onDelete = ForeignKey.SET_NULL)
        ])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val location: String = "",
    val groupId: Long? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val budgetAmount: Long? = null, // paise
    val status: EventStatus = EventStatus.OPEN, // OPEN, COMPLETED, ARCHIVED
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "event_participants",
        primaryKeys = ["eventId", "personId"])
data class EventParticipantEntity(
    val eventId: Long,
    val personId: Long,
    val isActive: Boolean = true,
)
```

### SharedExpenseEntity
```kotlin
@Entity(tableName = "shared_expenses")
data class SharedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalAmount: Long,          // paise — total paid by all payers
    val currency: String = "INR",
    val description: String,
    val date: Long,
    val categoryId: Long? = null,
    val eventId: Long? = null,
    val groupId: Long? = null,
    val splitMethod: SplitMethod,   // EQUAL, CUSTOM_AMOUNT, PERCENTAGE, SHARES, ITEM
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

// Who paid how much
@Entity(tableName = "expense_payers",
        primaryKeys = ["sharedExpenseId", "personId"])
data class ExpensePayerEntity(
    val sharedExpenseId: Long,
    val personId: Long,             // 0 = the app user themselves
    val amountPaid: Long,           // paise
)

// What each person owes
@Entity(tableName = "expense_splits")
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sharedExpenseId: Long,
    val personId: Long,             // 0 = the app user themselves
    val shareAmount: Long,          // paise — this person's portion
    val sharePercentage: Double? = null,
    val shareWeight: Double? = null,
    val isExcluded: Boolean = false,
    val isPaid: Boolean = false,
)
```

### SettlementEntity
```kotlin
@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val amount: Long,               // paise
    val currency: String = "INR",
    val direction: SettlementDirection, // RECEIVED (person paid user), PAID (user paid person)
    val date: Long,
    val accountId: Long? = null,    // which account was used
    val paymentMethod: String = "",
    val referenceNumber: String = "",
    val proofDocumentUri: String = "",
    val notes: String = "",
    val status: SettlementStatus = SettlementStatus.CONFIRMED, // PENDING, CONFIRMED, DISPUTED, CANCELLED
    val groupId: Long? = null,
    val eventId: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

---

## Phase 3 Entities (Loans & Bills)

### LoanEntity
```kotlin
@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LoanType,             // LENT (user lent), BORROWED (user borrowed)
    val personOrOrg: String,        // who lent to / who borrowed from
    val personId: Long? = null,     // links to PersonEntity if a contact
    val principalAmount: Long,      // paise
    val currency: String = "INR",
    val dateGiven: Long,
    val expectedRepaymentDate: Long? = null,
    val interestRate: Double = 0.0, // annual %
    val interestType: String = "NONE", // NONE, SIMPLE, COMPOUND
    val repaymentFrequency: String = "NONE", // NONE, MONTHLY (EMI), CUSTOM
    val accountId: Long? = null,    // from/to which account
    val notes: String = "",
    val agreementDocumentUri: String = "",
    val status: LoanStatus = LoanStatus.ACTIVE, // ACTIVE, PARTIALLY_REPAID, CLOSED
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "loan_repayments")
data class LoanRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val date: Long,
    val principalAmount: Long,      // paise
    val interestAmount: Long = 0,   // paise
    val feeAmount: Long = 0,        // paise
    val accountId: Long? = null,
    val notes: String = "",
    val proofDocumentUri: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

### BillEntity
```kotlin
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val provider: String = "",
    val type: BillType,             // ELECTRICITY, WATER, INTERNET, MOBILE, RENT,
                                    // INSURANCE, CREDIT_CARD, EMI, OTT, SOFTWARE, OTHER
    val expectedAmount: Long,       // paise
    val currency: String = "INR",
    val dueDay: Int,                // day of month (1-31)
    val frequency: BillFrequency,   // MONTHLY, QUARTERLY, YEARLY, WEEKLY, CUSTOM
    val paymentAccountId: Long? = null,
    val categoryId: Long? = null,
    val reminderDaysBefore: Int = 3,
    val isActive: Boolean = true,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bill_payments")
data class BillPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val dueDate: Long,
    val paidDate: Long? = null,
    val amount: Long,               // paise (actual paid amount)
    val isPaid: Boolean = false,
    val transactionId: Long? = null, // links to main transaction
    val receiptUri: String = "",
    val isDeleted: Boolean = false,
)
```

### BudgetEntity
```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,               // paise — budget limit
    val currency: String = "INR",
    val period: BudgetPeriod,       // MONTHLY, YEARLY, CUSTOM
    val startDate: Long,
    val endDate: Long? = null,      // null = recurring
    val categoryId: Long? = null,   // null = all categories
    val accountId: Long? = null,    // null = all accounts
    val groupId: Long? = null,
    val eventId: Long? = null,
    val alertThreshold: Int = 80,   // % — alert when spending reaches this
    val isRollover: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val targetAmount: Long,         // paise
    val currentAmount: Long = 0,    // paise — manually updated
    val currency: String = "INR",
    val targetDate: Long? = null,
    val accountId: Long? = null,    // which account tracks this goal
    val iconName: String = "savings",
    val colorHex: String = "#0066CC",
    val status: GoalStatus = GoalStatus.IN_PROGRESS, // IN_PROGRESS, COMPLETED, ABANDONED
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

---

## Phase 4 Entities (Investments)

### InvestmentEntity
```kotlin
@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,               // e.g., "HDFC Nifty 50 Fund"
    val type: InvestmentType,       // STOCK, MUTUAL_FUND, SIP, FIXED_DEPOSIT,
                                    // GOLD, REAL_ESTATE, BOND, CRYPTO, OTHER
    val platform: String = "",      // e.g., "Zerodha", "HDFC Securities"
    val accountId: Long? = null,    // investment account
    val symbol: String = "",        // ticker/ISIN
    val currentUnits: Double = 0.0,
    val averageBuyPrice: Long = 0,  // paise per unit
    val currentPrice: Long = 0,     // paise per unit — manually updated
    val lastPriceUpdateDate: Long? = null,
    val maturityDate: Long? = null, // for FDs, bonds
    val maturityAmount: Long? = null, // paise
    val notes: String = "",
    val status: InvestmentStatus = InvestmentStatus.ACTIVE,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "investment_transactions")
data class InvestmentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investmentId: Long,
    val type: InvestmentTransactionType, // BUY, SELL, DIVIDEND, SIP
    val date: Long,
    val units: Double,
    val pricePerUnit: Long,         // paise
    val totalAmount: Long,          // paise (units × price)
    val fees: Long = 0,             // paise brokerage/fees
    val taxes: Long = 0,            // paise STT/taxes
    val accountId: Long? = null,    // cash account debited/credited
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

---

## Phase 5 Entities (Documents & Import)

### DocumentEntity
```kotlin
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DocumentType,         // RECEIPT, INVOICE, BANK_STATEMENT, LOAN_AGREEMENT,
                                    // REPAYMENT_PROOF, INSURANCE, INVESTMENT_STATEMENT,
                                    // TAX_DOCUMENT, WARRANTY, OTHER
    val fileUri: String,            // local file URI (internal storage)
    val mimeType: String,           // "image/jpeg", "application/pdf", etc.
    val fileSizeBytes: Long = 0,
    val linkedEntityType: String = "",  // "transaction", "account", "loan", etc.
    val linkedEntityId: Long? = null,
    val tags: String = "",          // comma-separated
    val notes: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val importedAt: Long = System.currentTimeMillis(),
    val totalRows: Int,
    val successRows: Int,
    val failedRows: Int,
    val duplicateRows: Int,
    val status: ImportStatus,       // COMPLETED, PARTIAL, FAILED, ROLLED_BACK
    val errorFileUri: String = "",  // downloadable error report
    val canRollback: Boolean = false,
    val rolledBackAt: Long? = null,
)
```

---

## Enums

```kotlin
enum class AccountType { CASH, SAVINGS, CURRENT, DIGITAL_WALLET, UPI,
                         DEBIT_CARD, CREDIT_CARD, FIXED_DEPOSIT, INVESTMENT, OTHER }

enum class TransactionType { EXPENSE, INCOME, TRANSFER, REFUND, LENDING, BORROWING,
                              REPAYMENT_RECEIVED, REPAYMENT_MADE, INVESTMENT_BUY,
                              INVESTMENT_SELL, ACCOUNT_ADJUSTMENT }

enum class TransactionStatus { PENDING, COMPLETED, CANCELLED }

enum class CategoryType { EXPENSE, INCOME, BOTH }

enum class GroupStatus { ACTIVE, ARCHIVED, CLOSED }

enum class EventStatus { OPEN, COMPLETED, ARCHIVED }

enum class SplitMethod { EQUAL, CUSTOM_AMOUNT, PERCENTAGE, SHARES, ITEM }

enum class SettlementDirection { RECEIVED, PAID }

enum class SettlementStatus { PENDING, CONFIRMED, DISPUTED, CANCELLED }

enum class LoanType { LENT, BORROWED }

enum class LoanStatus { ACTIVE, PARTIALLY_REPAID, CLOSED }

enum class BillType { ELECTRICITY, WATER, INTERNET, MOBILE, RENT, INSURANCE,
                      CREDIT_CARD, EMI, OTT, SOFTWARE, OTHER }

enum class BillFrequency { WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM }

enum class BudgetPeriod { MONTHLY, YEARLY, CUSTOM }

enum class GoalStatus { IN_PROGRESS, COMPLETED, ABANDONED }

enum class InvestmentType { STOCK, MUTUAL_FUND, SIP, FIXED_DEPOSIT, GOLD,
                             REAL_ESTATE, BOND, CRYPTO, OTHER }

enum class InvestmentTransactionType { BUY, SELL, DIVIDEND, SIP }

enum class InvestmentStatus { ACTIVE, SOLD, MATURED }

enum class DocumentType { RECEIPT, INVOICE, BANK_STATEMENT, LOAN_AGREEMENT,
                           REPAYMENT_PROOF, INSURANCE, INVESTMENT_STATEMENT,
                           TAX_DOCUMENT, WARRANTY, OTHER }

enum class ImportStatus { COMPLETED, PARTIAL, FAILED, ROLLED_BACK }
```


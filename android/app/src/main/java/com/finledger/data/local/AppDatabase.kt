package com.finledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.finledger.data.local.dao.*
import com.finledger.data.local.entity.*

@Database(
    entities = [
        // Phase 1
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        AuditLogEntity::class,
        // Phase 2
        PersonEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        EventEntity::class,
        EventParticipantEntity::class,
        SharedExpenseEntity::class,
        ExpensePayerEntity::class,
        ExpenseSplitEntity::class,
        SharedExpenseItemEntity::class,
        SettlementEntity::class,
        // Phase 3
        LoanEntity::class,
        LoanRepaymentEntity::class,
        BillEntity::class,
        BillPaymentEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        // Phase 4
        InvestmentEntity::class,
        InvestmentTransactionEntity::class,
        NetWorthSnapshotEntity::class,
        // Phase 5
        DocumentEntity::class,
        ImportHistoryEntity::class,
        BackupHistoryEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun auditLogDao(): AuditLogDao

    // Phase 2
    abstract fun personDao(): PersonDao
    abstract fun groupDao(): GroupDao
    abstract fun eventDao(): EventDao
    abstract fun sharedExpenseDao(): SharedExpenseDao
    abstract fun settlementDao(): SettlementDao

    // Phase 3
    abstract fun loanDao(): LoanDao
    abstract fun billDao(): BillDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    // Phase 4
    abstract fun investmentDao(): InvestmentDao
    abstract fun netWorthDao(): NetWorthDao

    // Phase 5
    abstract fun documentDao(): DocumentDao
    abstract fun importHistoryDao(): ImportHistoryDao
    abstract fun backupHistoryDao(): BackupHistoryDao
}

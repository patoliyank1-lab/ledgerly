package com.finledger.di

import android.content.Context
import androidx.room.Room
import com.finledger.data.local.AppDatabase
import com.finledger.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "finledger.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()

    @Provides fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()
    @Provides fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()
    @Provides fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()
    @Provides fun provideSharedExpenseDao(db: AppDatabase): SharedExpenseDao = db.sharedExpenseDao()
    @Provides fun provideSettlementDao(db: AppDatabase): SettlementDao = db.settlementDao()

    @Provides fun provideLoanDao(db: AppDatabase): LoanDao = db.loanDao()
    @Provides fun provideBillDao(db: AppDatabase): BillDao = db.billDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideSavingsGoalDao(db: AppDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides fun provideInvestmentDao(db: AppDatabase): InvestmentDao = db.investmentDao()
    @Provides fun provideNetWorthDao(db: AppDatabase): NetWorthDao = db.netWorthDao()

    @Provides fun provideDocumentDao(db: AppDatabase): DocumentDao = db.documentDao()
    @Provides fun provideImportHistoryDao(db: AppDatabase): ImportHistoryDao = db.importHistoryDao()
    @Provides fun provideBackupHistoryDao(db: AppDatabase): BackupHistoryDao = db.backupHistoryDao()
}

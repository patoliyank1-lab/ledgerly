package com.finledger.data.local

import androidx.room.TypeConverter
import com.finledger.data.local.entity.*

class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromTransactionStatus(v: TransactionStatus): String = v.name
    @TypeConverter fun toTransactionStatus(v: String): TransactionStatus = TransactionStatus.valueOf(v)

    @TypeConverter fun fromCategoryType(v: CategoryType): String = v.name
    @TypeConverter fun toCategoryType(v: String): CategoryType = CategoryType.valueOf(v)

    @TypeConverter fun fromGroupStatus(v: GroupStatus): String = v.name
    @TypeConverter fun toGroupStatus(v: String): GroupStatus = GroupStatus.valueOf(v)

    @TypeConverter fun fromEventStatus(v: EventStatus): String = v.name
    @TypeConverter fun toEventStatus(v: String): EventStatus = EventStatus.valueOf(v)

    @TypeConverter fun fromSplitMethod(v: SplitMethod): String = v.name
    @TypeConverter fun toSplitMethod(v: String): SplitMethod = SplitMethod.valueOf(v)

    @TypeConverter fun fromSettlementDirection(v: SettlementDirection): String = v.name
    @TypeConverter fun toSettlementDirection(v: String): SettlementDirection = SettlementDirection.valueOf(v)

    @TypeConverter fun fromSettlementStatus(v: SettlementStatus): String = v.name
    @TypeConverter fun toSettlementStatus(v: String): SettlementStatus = SettlementStatus.valueOf(v)

    @TypeConverter fun fromLoanType(v: LoanType): String = v.name
    @TypeConverter fun toLoanType(v: String): LoanType = LoanType.valueOf(v)

    @TypeConverter fun fromLoanStatus(v: LoanStatus): String = v.name
    @TypeConverter fun toLoanStatus(v: String): LoanStatus = LoanStatus.valueOf(v)

    @TypeConverter fun fromBillType(v: BillType): String = v.name
    @TypeConverter fun toBillType(v: String): BillType = BillType.valueOf(v)

    @TypeConverter fun fromBillFrequency(v: BillFrequency): String = v.name
    @TypeConverter fun toBillFrequency(v: String): BillFrequency = BillFrequency.valueOf(v)

    @TypeConverter fun fromBudgetPeriod(v: BudgetPeriod): String = v.name
    @TypeConverter fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun fromGoalStatus(v: GoalStatus): String = v.name
    @TypeConverter fun toGoalStatus(v: String): GoalStatus = GoalStatus.valueOf(v)

    @TypeConverter fun fromInvestmentType(v: InvestmentType): String = v.name
    @TypeConverter fun toInvestmentType(v: String): InvestmentType = InvestmentType.valueOf(v)

    @TypeConverter fun fromInvestmentTxType(v: InvestmentTransactionType): String = v.name
    @TypeConverter fun toInvestmentTxType(v: String): InvestmentTransactionType = InvestmentTransactionType.valueOf(v)

    @TypeConverter fun fromInvestmentStatus(v: InvestmentStatus): String = v.name
    @TypeConverter fun toInvestmentStatus(v: String): InvestmentStatus = InvestmentStatus.valueOf(v)

    @TypeConverter fun fromDocumentType(v: DocumentType): String = v.name
    @TypeConverter fun toDocumentType(v: String): DocumentType = DocumentType.valueOf(v)

    @TypeConverter fun fromImportStatus(v: ImportStatus): String = v.name
    @TypeConverter fun toImportStatus(v: String): ImportStatus = ImportStatus.valueOf(v)
}


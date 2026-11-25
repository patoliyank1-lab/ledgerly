package com.finledger.ui.navigation

/**
 * Navigation routes covering all 17 modules from Product.md and their detail flows.
 */
sealed class Screen(val route: String) {
    // Primary bottom-nav tabs
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object Accounts : Screen("accounts")
    data object Friends : Screen("friends")
    data object More : Screen("more")

    // Transaction flows (Phase 1)
    data object AddExpense : Screen("add_expense")
    data object AddIncome : Screen("add_income")
    data object AddTransfer : Screen("add_transfer")
    data object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }

    // Account flows (Phase 1)
    data object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Long) = "account_detail/$accountId"
    }

    // Settings & personalization (Phase 1)
    data object Settings : Screen("settings")
    data object ManageCategories : Screen("manage_categories")
    data object ManageTags : Screen("manage_tags")

    // Friends, Groups, Events, Balances (Phase 2)
    data object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: Long) = "person_detail/$personId"
    }
    data object RecordSettlement : Screen("record_settlement?personId={personId}") {
        fun createRoute(personId: Long? = null) =
            if (personId != null) "record_settlement?personId=$personId" else "record_settlement"
    }
    data object Groups : Screen("groups")
    data object CreateEditGroup : Screen("create_edit_group?groupId={groupId}") {
        fun createRoute(groupId: Long? = null) =
            if (groupId != null) "create_edit_group?groupId=$groupId" else "create_edit_group"
    }
    data object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: Long) = "group_detail/$groupId"
    }
    data object Events : Screen("events")
    data object CreateEditEvent : Screen("create_edit_event?eventId={eventId}&groupId={groupId}") {
        fun createRoute(eventId: Long? = null, groupId: Long? = null) = buildString {
            append("create_edit_event")
            val params = mutableListOf<String>()
            if (eventId != null) params.add("eventId=$eventId")
            if (groupId != null) params.add("groupId=$groupId")
            if (params.isNotEmpty()) append("?${params.joinToString("&")}")
        }
    }
    data object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }
    data object AddSharedExpense : Screen("add_shared_expense?eventId={eventId}&groupId={groupId}") {
        fun createRoute(eventId: Long? = null, groupId: Long? = null) = buildString {
            append("add_shared_expense")
            val params = mutableListOf<String>()
            if (eventId != null) params.add("eventId=$eventId")
            if (groupId != null) params.add("groupId=$groupId")
            if (params.isNotEmpty()) append("?${params.joinToString("&")}")
        }
    }
    data object EventSummary : Screen("event_summary/{eventId}") {
        fun createRoute(eventId: Long) = "event_summary/$eventId"
    }
    data object Balances : Screen("balances")

    // Loans, Bills, Budgets, Goals (Phase 3)
    data object Loans : Screen("loans")
    data object AddLoan : Screen("add_loan")
    data object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    data object RecordRepayment : Screen("record_repayment/{loanId}") {
        fun createRoute(loanId: Long) = "record_repayment/$loanId"
    }
    data object Bills : Screen("bills")
    data object AddEditBill : Screen("add_edit_bill?billId={billId}") {
        fun createRoute(billId: Long? = null) =
            if (billId != null) "add_edit_bill?billId=$billId" else "add_edit_bill"
    }
    data object Budgets : Screen("budgets")
    data object AddEditBudget : Screen("add_edit_budget?budgetId={budgetId}") {
        fun createRoute(budgetId: Long? = null) =
            if (budgetId != null) "add_edit_budget?budgetId=$budgetId" else "add_edit_budget"
    }
    data object SavingsGoals : Screen("savings_goals")
    data object AddEditGoal : Screen("add_edit_goal?goalId={goalId}") {
        fun createRoute(goalId: Long? = null) =
            if (goalId != null) "add_edit_goal?goalId=$goalId" else "add_edit_goal"
    }
    data object NotificationSettings : Screen("notification_settings")
    data object ReminderSettings : Screen("reminder_settings")

    // Analytics, Investments, Net Worth, Calendar (Phase 4)
    data object Analytics : Screen("analytics")
    data object Investments : Screen("investments")
    data object AddInvestment : Screen("add_investment")
    data object InvestmentDetail : Screen("investment_detail/{investmentId}") {
        fun createRoute(investmentId: Long) = "investment_detail/$investmentId"
    }
    data object NetWorth : Screen("net_worth")
    data object FinancialCalendar : Screen("financial_calendar")

    // Import, Export, Backup, Vault, History (Phase 5)
    data object ImportExport : Screen("import_export")
    data object ColumnMapper : Screen("column_mapper")
    data object ImportPreview : Screen("import_preview")
    data object ImportResult : Screen("import_result")
    data object ImportHistory : Screen("import_history")
    data object BackupSettings : Screen("backup_settings")
    data object Restore : Screen("restore")
    data object DocumentVault : Screen("document_vault")
    data object DocumentViewer : Screen("document_viewer/{documentId}") {
        fun createRoute(documentId: Long) = "document_viewer/$documentId"
    }
    data object Trash : Screen("trash")
    data object AuditHistory : Screen("audit_history")
    data object Archive : Screen("archive")

    // Security & Onboarding
    data object Onboarding : Screen("onboarding")
    data object PinLock : Screen("pin_lock")
}


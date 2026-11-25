package com.finledger.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.finledger.ui.screens.accounts.AccountDetailScreen
import com.finledger.ui.screens.accounts.AccountsScreen
import com.finledger.ui.screens.analytics.AnalyticsScreen
import com.finledger.ui.screens.analytics.FinancialCalendarScreen
import com.finledger.ui.screens.analytics.NetWorthScreen
import com.finledger.ui.screens.bills.AddEditBillScreen
import com.finledger.ui.screens.bills.BillsScreen
import com.finledger.ui.screens.budgets.AddEditBudgetScreen
import com.finledger.ui.screens.budgets.AddEditGoalScreen
import com.finledger.ui.screens.budgets.BudgetsScreen
import com.finledger.ui.screens.budgets.SavingsGoalsScreen
import com.finledger.ui.screens.dashboard.DashboardScreen
import com.finledger.ui.screens.data.ArchiveScreen
import com.finledger.ui.screens.data.AuditHistoryScreen
import com.finledger.ui.screens.data.ColumnMapperScreen
import com.finledger.ui.screens.data.DocumentVaultScreen
import com.finledger.ui.screens.data.DocumentViewerScreen
import com.finledger.ui.screens.data.ImportExportScreen
import com.finledger.ui.screens.data.ImportHistoryScreen
import com.finledger.ui.screens.data.ImportPreviewScreen
import com.finledger.ui.screens.data.ImportResultScreen
import com.finledger.ui.screens.data.RestoreScreen
import com.finledger.ui.screens.data.TrashScreen
import com.finledger.ui.screens.events.AddSharedExpenseScreen
import com.finledger.ui.screens.events.CreateEditEventScreen
import com.finledger.ui.screens.events.EventDetailScreen
import com.finledger.ui.screens.events.EventSummaryScreen
import com.finledger.ui.screens.events.EventsScreen
import com.finledger.ui.screens.friends.BalancesScreen
import com.finledger.ui.screens.friends.FriendsScreen
import com.finledger.ui.screens.friends.PersonDetailScreen
import com.finledger.ui.screens.friends.RecordSettlementScreen
import com.finledger.ui.screens.groups.CreateEditGroupScreen
import com.finledger.ui.screens.groups.GroupDetailScreen
import com.finledger.ui.screens.groups.GroupsScreen
import com.finledger.ui.screens.investments.AddInvestmentScreen
import com.finledger.ui.screens.investments.InvestmentDetailScreen
import com.finledger.ui.screens.investments.InvestmentsScreen
import com.finledger.ui.screens.loans.AddLoanScreen
import com.finledger.ui.screens.loans.LoanDetailScreen
import com.finledger.ui.screens.loans.LoansScreen
import com.finledger.ui.screens.loans.RecordRepaymentScreen
import com.finledger.ui.screens.security.OnboardingFlow
import com.finledger.ui.screens.security.PinLockSettingsScreen
import com.finledger.ui.screens.settings.ManageCategoriesScreen
import com.finledger.ui.screens.settings.ManageTagsScreen
import com.finledger.ui.screens.settings.NotificationSettingsScreen
import com.finledger.ui.screens.settings.SettingsScreen
import com.finledger.ui.screens.transactions.AddExpenseScreen
import com.finledger.ui.screens.transactions.AddIncomeScreen
import com.finledger.ui.screens.transactions.AddTransferScreen
import com.finledger.ui.screens.transactions.EditTransactionScreen
import com.finledger.ui.screens.transactions.TransactionDetailScreen
import com.finledger.ui.screens.transactions.TransactionsScreen
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun FinLedgerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
    ) {
        // Primary tabs (Phase 1 wired)
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                onAddIncome = { navController.navigate(Screen.AddIncome.route) },
                onAddTransfer = { navController.navigate(Screen.AddTransfer.route) },
                onAddSharedExpense = { navController.navigate(Screen.AddSharedExpense.createRoute()) },
                onAddLoan = { navController.navigate(Screen.AddLoan.route) },
                onRecordSettlement = { navController.navigate(Screen.RecordSettlement.createRoute()) },
                onSeeAllTransactions = { navController.navigate(Screen.Transactions.route) },
                onOpenTransaction = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenBills = { navController.navigate(Screen.Bills.route) },
                onOpenBudgets = { navController.navigate(Screen.Budgets.route) },
                onOpenBalances = { navController.navigate(Screen.Balances.route) },
                onOpenNetWorth = { navController.navigate(Screen.NetWorth.route) },
                onOpenInvestments = { navController.navigate(Screen.Investments.route) },
            )
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                onAddIncome = { navController.navigate(Screen.AddIncome.route) },
                onAddTransfer = { navController.navigate(Screen.AddTransfer.route) },
                onOpenTransactionDetail = { id -> navController.navigate(Screen.TransactionDetail.createRoute(id)) },
            )
        }
        composable(Screen.Accounts.route) {
            AccountsScreen(
                onOpenAccountDetail = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
            )
        }
        composable(Screen.Friends.route) {
            FriendsScreen(
                onOpenPersonDetail = { id -> navController.navigate(Screen.PersonDetail.createRoute(id)) },
            )
        }
        composable(Screen.More.route) {
            MoreHubScreen(onNavigate = { route -> navController.navigate(route) })
        }

        // Phase 1 routes
        composable(Screen.AddExpense.route) {
            AddExpenseScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AddIncome.route) {
            AddIncomeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AddTransfer.route) {
            AddTransferScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("transactionId") ?: 0L
            TransactionDetailScreen(
                transactionId = id,
                onBack = { navController.popBackStack() },
                onEdit = { txId -> navController.navigate(Screen.EditTransaction.createRoute(txId)) },
            )
        }
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("transactionId") ?: 0L
            EditTransactionScreen(
                transactionId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("accountId") ?: 0L
            AccountDetailScreen(
                accountId = id,
                onBack = { navController.popBackStack() },
                onOpenTransaction = { txId -> navController.navigate(Screen.TransactionDetail.createRoute(txId)) },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageCategories = { navController.navigate(Screen.ManageCategories.route) },
                onManageTags = { navController.navigate(Screen.ManageTags.route) },
                onOpenNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                onOpenBackup = { navController.navigate(Screen.ImportExport.route) },
                onOpenTrash = { navController.navigate(Screen.Trash.route) },
                onOpenArchive = { navController.navigate(Screen.Archive.route) },
                onOpenAuditHistory = { navController.navigate(Screen.AuditHistory.route) },
                onSetupPin = { navController.navigate(Screen.PinLock.route) },
            )
        }
        composable(Screen.ManageCategories.route) {
            ManageCategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ManageTags.route) {
            ManageTagsScreen(onBack = { navController.popBackStack() })
        }

        // Phase 2 routes
        composable(
            route = Screen.PersonDetail.route,
            arguments = listOf(navArgument("personId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("personId") ?: 0L
            PersonDetailScreen(
                personId = id,
                onBack = { navController.popBackStack() },
                onSettleUp = { pid -> navController.navigate(Screen.RecordSettlement.createRoute(pid)) },
            )
        }
        composable(
            route = Screen.RecordSettlement.route,
            arguments = listOf(navArgument("personId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val id = entry.arguments?.getLong("personId")?.takeIf { it > 0L }
            RecordSettlementScreen(
                initialPersonId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Groups.route) {
            GroupsScreen(
                onBack = { navController.popBackStack() },
                onCreateGroup = { navController.navigate(Screen.CreateEditGroup.createRoute()) },
                onOpenGroupDetail = { id -> navController.navigate(Screen.GroupDetail.createRoute(id)) },
            )
        }
        composable(
            route = Screen.CreateEditGroup.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val id = entry.arguments?.getLong("groupId")?.takeIf { it > 0L }
            CreateEditGroupScreen(
                groupId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("groupId") ?: 0L
            GroupDetailScreen(
                groupId = id,
                onBack = { navController.popBackStack() },
                onEditGroup = { gid -> navController.navigate(Screen.CreateEditGroup.createRoute(gid)) },
                onAddEvent = { gid -> navController.navigate(Screen.CreateEditEvent.createRoute(groupId = gid)) },
                onOpenEvent = { eid -> navController.navigate(Screen.EventDetail.createRoute(eid)) },
                onAddSharedExpense = { gid -> navController.navigate(Screen.AddSharedExpense.createRoute(groupId = gid)) },
            )
        }
        composable(Screen.Events.route) {
            EventsScreen(
                onBack = { navController.popBackStack() },
                onCreateEvent = { navController.navigate(Screen.CreateEditEvent.createRoute()) },
                onOpenEvent = { eid -> navController.navigate(Screen.EventDetail.createRoute(eid)) },
            )
        }
        composable(
            route = Screen.CreateEditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("groupId") { type = NavType.LongType; defaultValue = -1L },
            )
        ) { entry ->
            val eid = entry.arguments?.getLong("eventId")?.takeIf { it > 0L }
            val gid = entry.arguments?.getLong("groupId")?.takeIf { it > 0L }
            CreateEditEventScreen(
                eventId = eid,
                initialGroupId = gid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { entry ->
            val eid = entry.arguments?.getLong("eventId") ?: 0L
            EventDetailScreen(
                eventId = eid,
                onBack = { navController.popBackStack() },
                onAddSharedExpense = { evId, gId ->
                    navController.navigate(Screen.AddSharedExpense.createRoute(eventId = evId, groupId = gId))
                },
                onOpenSummary = { evId -> navController.navigate(Screen.EventSummary.createRoute(evId)) },
            )
        }
        composable(
            route = Screen.AddSharedExpense.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("groupId") { type = NavType.LongType; defaultValue = -1L },
            )
        ) { entry ->
            val eid = entry.arguments?.getLong("eventId")?.takeIf { it > 0L }
            val gid = entry.arguments?.getLong("groupId")?.takeIf { it > 0L }
            AddSharedExpenseScreen(
                eventId = eid,
                groupId = gid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.EventSummary.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { entry ->
            val eid = entry.arguments?.getLong("eventId") ?: 0L
            EventSummaryScreen(
                eventId = eid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Balances.route) {
            BalancesScreen(
                onBack = { navController.popBackStack() },
                onOpenPersonDetail = { pid -> navController.navigate(Screen.PersonDetail.createRoute(pid)) },
                onRecordSettlement = { navController.navigate(Screen.RecordSettlement.createRoute()) },
            )
        }

        // Phase 3 routes
        composable(Screen.Loans.route) {
            LoansScreen(
                onBack = { navController.popBackStack() },
                onAddLoan = { navController.navigate(Screen.AddLoan.route) },
                onOpenLoanDetail = { id -> navController.navigate(Screen.LoanDetail.createRoute(id)) },
            )
        }
        composable(Screen.AddLoan.route) {
            AddLoanScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { entry ->
            val lid = entry.arguments?.getLong("loanId") ?: 0L
            LoanDetailScreen(
                loanId = lid,
                onBack = { navController.popBackStack() },
                onRecordRepayment = { id -> navController.navigate(Screen.RecordRepayment.createRoute(id)) },
            )
        }
        composable(
            route = Screen.RecordRepayment.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { entry ->
            val lid = entry.arguments?.getLong("loanId") ?: 0L
            RecordRepaymentScreen(
                loanId = lid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Bills.route) {
            BillsScreen(
                onBack = { navController.popBackStack() },
                onAddBill = { navController.navigate(Screen.AddEditBill.createRoute()) },
            )
        }
        composable(
            route = Screen.AddEditBill.route,
            arguments = listOf(navArgument("billId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val bid = entry.arguments?.getLong("billId")?.takeIf { it > 0L }
            AddEditBillScreen(
                billId = bid,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Budgets.route) {
            BudgetsScreen(
                onBack = { navController.popBackStack() },
                onAddBudget = { navController.navigate(Screen.AddEditBudget.createRoute()) },
            )
        }
        composable(
            route = Screen.AddEditBudget.route,
            arguments = listOf(navArgument("budgetId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            AddEditBudgetScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SavingsGoals.route) {
            SavingsGoalsScreen(
                onBack = { navController.popBackStack() },
                onAddGoal = { navController.navigate(Screen.AddEditGoal.createRoute()) },
            )
        }
        composable(
            route = Screen.AddEditGoal.route,
            arguments = listOf(navArgument("goalId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            AddEditGoalScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ReminderSettings.route) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }

        // Phase 4 routes
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() },
                onOpenTransaction = { txId -> navController.navigate(Screen.TransactionDetail.createRoute(txId)) },
            )
        }
        composable(Screen.Investments.route) {
            InvestmentsScreen(
                onBack = { navController.popBackStack() },
                onAddInvestment = { navController.navigate(Screen.AddInvestment.route) },
                onOpenInvestmentDetail = { id -> navController.navigate(Screen.InvestmentDetail.createRoute(id)) },
            )
        }
        composable(Screen.AddInvestment.route) {
            AddInvestmentScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.InvestmentDetail.route,
            arguments = listOf(navArgument("investmentId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("investmentId") ?: 0L
            InvestmentDetailScreen(
                investmentId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.NetWorth.route) {
            NetWorthScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.FinancialCalendar.route) {
            FinancialCalendarScreen(onBack = { navController.popBackStack() })
        }

        // Phase 5 routes
        composable(Screen.ImportExport.route) {
            ImportExportScreen(
                onBack = { navController.popBackStack() },
                onOpenColumnMapper = { navController.navigate(Screen.ColumnMapper.route) },
                onOpenImportHistory = { navController.navigate(Screen.ImportHistory.route) },
                onOpenRestore = { navController.navigate(Screen.Restore.route) },
            )
        }
        composable(Screen.ColumnMapper.route) {
            ColumnMapperScreen(
                onBack = { navController.popBackStack() },
                onPreviewReady = { navController.navigate(Screen.ImportPreview.route) },
            )
        }
        composable(Screen.ImportPreview.route) {
            ImportPreviewScreen(
                onBack = { navController.popBackStack() },
                onCommitted = { navController.navigate(Screen.ImportResult.route) },
            )
        }
        composable(Screen.ImportResult.route) {
            ImportResultScreen(
                onDone = {
                    navController.popBackStack(Screen.ImportExport.route, inclusive = false)
                },
            )
        }
        composable(Screen.ImportHistory.route) {
            ImportHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.BackupSettings.route) {
            ImportExportScreen(
                onBack = { navController.popBackStack() },
                onOpenColumnMapper = { navController.navigate(Screen.ColumnMapper.route) },
                onOpenImportHistory = { navController.navigate(Screen.ImportHistory.route) },
                onOpenRestore = { navController.navigate(Screen.Restore.route) },
            )
        }
        composable(Screen.Restore.route) {
            RestoreScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DocumentVault.route) {
            DocumentVaultScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { id -> navController.navigate(Screen.DocumentViewer.createRoute(id)) },
            )
        }
        composable(
            route = Screen.DocumentViewer.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("documentId") ?: 0L
            DocumentViewerScreen(
                documentId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Trash.route) {
            TrashScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AuditHistory.route) {
            AuditHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Archive.route) {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }

        // Security & Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingFlow(onComplete = { navController.popBackStack() })
        }
        composable(Screen.PinLock.route) {
            PinLockSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private data class MoreMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

@Composable
fun MoreHubScreen(
    onNavigate: (String) -> Unit,
) {
    val sections = listOf(
        "Shared & Debt" to listOf(
            MoreMenuItem("Person-wise Balances", "Consolidated receivables & payables", Icons.Outlined.AccountBalance, Screen.Balances.route),
            MoreMenuItem("Groups", "Shared friend & household groups", Icons.Outlined.Groups, Screen.Groups.route),
            MoreMenuItem("Events", "Trips, dinners, celebrations", Icons.Outlined.Event, Screen.Events.route),
            MoreMenuItem("Borrowing & Lending", "Loans, EMIs & repayments", Icons.Outlined.Money, Screen.Loans.route),
        ),
        "Planning & Insights" to listOf(
            MoreMenuItem("Bills & Subscriptions", "Upcoming & recurring bills", Icons.Outlined.ReceiptLong, Screen.Bills.route),
            MoreMenuItem("Budgets", "Category & period spending limits", Icons.Outlined.PieChart, Screen.Budgets.route),
            MoreMenuItem("Savings Goals", "Target amounts & milestones", Icons.Outlined.Savings, Screen.SavingsGoals.route),
            MoreMenuItem("Analytics & Reports", "Spending, income, cash flow", Icons.Outlined.Analytics, Screen.Analytics.route),
            MoreMenuItem("Investments & Assets", "Stocks, MF, FD, gold, real estate", Icons.Outlined.TrendingUp, Screen.Investments.route),
            MoreMenuItem("Net Worth", "Assets minus liabilities", Icons.Outlined.ShowChart, Screen.NetWorth.route),
            MoreMenuItem("Financial Calendar", "Combined financial schedule", Icons.Outlined.CalendarMonth, Screen.FinancialCalendar.route),
        ),
        "Data & Tools" to listOf(
            MoreMenuItem("Import, Export & Backup", "CSV, PDF reports, JSON backup", Icons.Outlined.Backup, Screen.ImportExport.route),
            MoreMenuItem("Document Vault", "Receipts, invoices, agreements", Icons.Outlined.FolderOpen, Screen.DocumentVault.route),
            MoreMenuItem("Activity History", "Audit trail of edits & deletions", Icons.Outlined.History, Screen.AuditHistory.route),
            MoreMenuItem("Archive", "Archived accounts & groups", Icons.Outlined.Archive, Screen.Archive.route),
            MoreMenuItem("Trash", "Recover soft-deleted records", Icons.Outlined.DeleteOutline, Screen.Trash.route),
            MoreMenuItem("Notifications", "Reminders & alert preferences", Icons.Outlined.Notifications, Screen.NotificationSettings.route),
            MoreMenuItem("Settings", "Profile, currency, theme, security", Icons.Outlined.Settings, Screen.Settings.route),
        ),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("More", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            sections.forEach { (sectionTitle, items) ->
                item(key = sectionTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = FinLedgerTheme.colors.inkMuted48,
                            modifier = Modifier.padding(horizontal = Spacing.xxs),
                        )
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column {
                                items.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigate(item.route) }
                                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = Spacing.sm),
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = FinLedgerTheme.colors.inkMuted48,
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = FinLedgerTheme.colors.inkMuted48,
                                        )
                                    }
                                    if (index < items.lastIndex) {
                                        HorizontalDivider(
                                            color = FinLedgerTheme.colors.dividerSoft,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(start = 50.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(Spacing.lg)) }
        }
    }
}

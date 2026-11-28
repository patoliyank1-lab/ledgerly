package com.finledger.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.dao.BackupHistoryDao
import com.finledger.data.local.entity.BackupHistoryEntity
import com.finledger.data.local.entity.BillEntity
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountSummary
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.MonthlySummary
import com.finledger.domain.model.SetupChecklistState
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.*
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DashboardAlertKind {
    OVERDUE_BILLS,
    UPCOMING_BILLS,
    BUDGET_ALERT,
}

data class DashboardTopAlert(
    val kind: DashboardAlertKind,
    val title: String,
    val subtitle: String,
    val isUrgent: Boolean,
)

object DashboardAlertSelector {
    private const val UPCOMING_WINDOW_MS = 7L * 86_400_000L

    fun selectTopAlert(
        bills: List<BillEntity>,
        budgets: List<BudgetProgress>,
        currency: String,
        nowMs: Long = System.currentTimeMillis(),
        trackPlanning: Boolean = true,
        showAlerts: Boolean = true,
    ): DashboardTopAlert? {
        if (!showAlerts || !trackPlanning) return null

        val overdue = bills.filter { it.nextDueDateMs < nowMs }
        if (overdue.isNotEmpty()) {
            val totalOverdue = overdue.sumOf { it.expectedAmount }
            val title = if (overdue.size == 1) {
                "${overdue.first().name} is overdue"
            } else {
                "${overdue.size} bills overdue"
            }
            return DashboardTopAlert(
                kind = DashboardAlertKind.OVERDUE_BILLS,
                title = title,
                subtitle = "${MoneyFormatter.format(totalOverdue, currency)} due — tap to pay",
                isUrgent = true,
            )
        }

        val exceededBudget = budgets.filter { it.isExceeded }.maxByOrNull { it.percentUsed }
        if (exceededBudget != null) {
            return DashboardTopAlert(
                kind = DashboardAlertKind.BUDGET_ALERT,
                title = "${exceededBudget.budget.name} budget ${exceededBudget.percentUsed}% used",
                subtitle = "${MoneyFormatter.format(exceededBudget.spentPaise, currency)} of ${MoneyFormatter.format(exceededBudget.budget.amount, currency)} — tap to view",
                isUrgent = true,
            )
        }

        val dueSoon = bills.filter { it.nextDueDateMs in nowMs..(nowMs + UPCOMING_WINDOW_MS) }
        if (dueSoon.isNotEmpty()) {
            val totalDue = dueSoon.sumOf { it.expectedAmount }
            val title = if (dueSoon.size == 1) {
                "${dueSoon.first().name} due ${DateFormatter.formatDate(dueSoon.first().nextDueDateMs)}"
            } else {
                "${dueSoon.size} bills due this week"
            }
            return DashboardTopAlert(
                kind = DashboardAlertKind.UPCOMING_BILLS,
                title = title,
                subtitle = "${MoneyFormatter.format(totalDue, currency)} — tap to view",
                isUrgent = false,
            )
        }

        val alertBudget = budgets.filter { it.isAlertTriggered }.maxByOrNull { it.percentUsed }
        if (alertBudget != null) {
            return DashboardTopAlert(
                kind = DashboardAlertKind.BUDGET_ALERT,
                title = "${alertBudget.budget.name} budget ${alertBudget.percentUsed}% used",
                subtitle = "${MoneyFormatter.format(alertBudget.spentPaise, currency)} of ${MoneyFormatter.format(alertBudget.budget.amount, currency)} — tap to view",
                isUrgent = false,
            )
        }

        return null
    }
}

data class DashboardUiState(
    val userName: String = "Personal",
    val primaryCurrency: String = "INR",
    val isSimpleMode: Boolean = true,
    val trackSharedAndPeople: Boolean = true,
    val trackInvestments: Boolean = false,
    val trackPlanning: Boolean = true,
    val showDashboardNetWorth: Boolean = false,
    val showDashboardAlerts: Boolean = true,
    val accountSummary: AccountSummary = AccountSummary(0, 0, 0, 0, 0, 0, 0),
    val accounts: List<AccountWithBalance> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(0, 0, 0, 0, 0, 0),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val upcomingBills: List<BillEntity> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val topAlert: DashboardTopAlert? = null,
    val setupChecklist: SetupChecklistState = SetupChecklistState(),
    val totalToReceive: Long = 0L,
    val totalToPay: Long = 0L,
    val netWorth: NetWorthBreakdown = NetWorthBreakdown(0, 0, 0, 0, 0, 0, 0, 0, 0),
    val portfolio: PortfolioOverview = PortfolioOverview(emptyList(), 0, 0, 0, 0, emptyMap()),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getAccountsUseCase: GetAccountsUseCase,
    getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    transactionRepository: TransactionRepository,
    commitmentRepository: CommitmentRepository,
    getBudgetProgressUseCase: GetBudgetProgressUseCase,
    computePersonBalanceUseCase: ComputePersonBalanceUseCase,
    getNetWorthUseCase: GetNetWorthUseCase,
    getPortfolioValueUseCase: GetPortfolioValueUseCase,
    backupHistoryDao: BackupHistoryDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine<Any?, DashboardUiState>(
        userPreferencesRepository.preferences,
        getAccountsUseCase(),
        getAccountsUseCase.observeSummary(),
        getMonthlySummaryUseCase(),
        transactionRepository.observeRecentTransactions(limit = 10),
        commitmentRepository.observeActiveBills(),
        getBudgetProgressUseCase(),
        computePersonBalanceUseCase(),
        getNetWorthUseCase(),
        getPortfolioValueUseCase(),
        backupHistoryDao.observeAll(),
    ) { values ->
        val prefs = values[0] as com.finledger.domain.model.UserPreferences
        @Suppress("UNCHECKED_CAST")
        val accounts = values[1] as List<AccountWithBalance>
        val accSummary = values[2] as AccountSummary
        val monthSummary = values[3] as MonthlySummary
        @Suppress("UNCHECKED_CAST")
        val recent = values[4] as List<TransactionWithDetails>
        @Suppress("UNCHECKED_CAST")
        val bills = values[5] as List<BillEntity>
        @Suppress("UNCHECKED_CAST")
        val budgets = values[6] as List<BudgetProgress>
        @Suppress("UNCHECKED_CAST")
        val balances = values[7] as List<PersonBalanceSummary>
        val netWorth = values[8] as NetWorthBreakdown
        val portfolio = values[9] as PortfolioOverview
        @Suppress("UNCHECKED_CAST")
        val backups = values[10] as List<BackupHistoryEntity>

        DashboardUiState(
            userName = prefs.userName,
            primaryCurrency = prefs.primaryCurrency,
            isSimpleMode = prefs.isSimpleMode,
            trackSharedAndPeople = prefs.trackSharedAndPeople,
            trackInvestments = prefs.trackInvestments,
            trackPlanning = prefs.trackPlanning,
            showDashboardNetWorth = prefs.showDashboardNetWorth,
            showDashboardAlerts = prefs.showDashboardAlerts,
            accountSummary = accSummary,
            accounts = accounts,
            monthlySummary = monthSummary,
            recentTransactions = recent,
            upcomingBills = bills,
            budgetProgress = budgets,
            topAlert = DashboardAlertSelector.selectTopAlert(
                bills = bills,
                budgets = budgets,
                currency = prefs.primaryCurrency,
                trackPlanning = prefs.trackPlanning,
                showAlerts = prefs.showDashboardAlerts,
            ),
            setupChecklist = SetupChecklistState(
                onboardingCompleted = prefs.onboardingCompleted,
                hasAnyTransaction = recent.isNotEmpty(),
                hasAnyBudget = budgets.isNotEmpty(),
                hasBackupOrAutoBackupEnabled = prefs.autoBackupEnabled || backups.isNotEmpty(),
                isDismissed = prefs.checklistDismissed,
            ),
            totalToReceive = balances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise },
            totalToPay = balances.filter { it.netBalancePaise < 0 }.sumOf { -it.netBalancePaise },
            netWorth = netWorth,
            portfolio = portfolio,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun dismissSetupChecklist() {
        viewModelScope.launch { userPreferencesRepository.setChecklistDismissed(true) }
    }
}

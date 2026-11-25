package com.finledger.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.BillEntity
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountSummary
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.MonthlySummary
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "Personal",
    val primaryCurrency: String = "INR",
    val accountSummary: AccountSummary = AccountSummary(0, 0, 0, 0, 0, 0, 0),
    val accounts: List<AccountWithBalance> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(0, 0, 0, 0, 0, 0),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val upcomingBills: List<BillEntity> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
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
    userPreferencesRepository: UserPreferencesRepository,
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

        DashboardUiState(
            userName = prefs.userName,
            primaryCurrency = prefs.primaryCurrency,
            accountSummary = accSummary,
            accounts = accounts,
            monthlySummary = monthSummary,
            recentTransactions = recent,
            upcomingBills = bills,
            budgetProgress = budgets,
            totalToReceive = balances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise },
            totalToPay = balances.filter { it.netBalancePaise < 0 }.sumOf { -it.netBalancePaise },
            netWorth = netWorth,
            portfolio = portfolio,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

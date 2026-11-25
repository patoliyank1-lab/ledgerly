package com.finledger.ui.screens.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.InvestmentTransactionEntity
import com.finledger.data.local.entity.InvestmentTransactionType
import com.finledger.data.local.entity.InvestmentType
import com.finledger.data.repository.InsightRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.usecase.AddInvestmentUseCase
import com.finledger.domain.usecase.GetAccountsUseCase
import com.finledger.domain.usecase.GetPortfolioValueUseCase
import com.finledger.domain.usecase.PortfolioOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class InvestmentsUiState(
    val portfolio: PortfolioOverview = PortfolioOverview(emptyList(), 0, 0, 0, 0, emptyMap()),
    val accounts: List<AccountWithBalance> = emptyList(),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    getPortfolioValueUseCase: GetPortfolioValueUseCase,
    private val addInvestmentUseCase: AddInvestmentUseCase,
    private val insightRepository: InsightRepository,
    getAccountsUseCase: GetAccountsUseCase,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InvestmentsUiState> = combine(
        getPortfolioValueUseCase(),
        getAccountsUseCase(),
        userPreferencesRepository.preferences,
        _error,
    ) { portfolio, accounts, prefs, err ->
        InvestmentsUiState(
            portfolio = portfolio,
            accounts = accounts,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsUiState())

    private val tradeFlows = ConcurrentHashMap<Long, StateFlow<List<InvestmentTransactionEntity>>>()

    fun observeTrades(investmentId: Long): StateFlow<List<InvestmentTransactionEntity>> = tradeFlows.getOrPut(investmentId) {
        insightRepository.observeInvestmentTransactions(investmentId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun createInvestment(
        name: String,
        type: InvestmentType,
        platform: String,
        symbol: String,
        units: Double,
        pricePerUnitPaise: Long,
        feesPaise: Long,
        taxesPaise: Long,
        dateMs: Long,
        accountId: Long?,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            addInvestmentUseCase.createAndBuy(
                name = name,
                type = type,
                platform = platform,
                symbol = symbol,
                units = units,
                pricePerUnitPaise = pricePerUnitPaise,
                feesPaise = feesPaise,
                taxesPaise = taxesPaise,
                dateMs = dateMs,
                accountId = accountId,
                notes = notes,
            ).onSuccess { _error.value = null; onSuccess() }
                .onFailure { _error.value = it.message }
        }
    }

    fun recordTrade(
        investmentId: Long,
        tradeType: InvestmentTransactionType,
        units: Double,
        pricePerUnitPaise: Long,
        feesPaise: Long = 0L,
        taxesPaise: Long = 0L,
        accountId: Long?,
        notes: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            addInvestmentUseCase.recordTrade(
                investmentId = investmentId,
                tradeType = tradeType,
                units = units,
                pricePerUnitPaise = pricePerUnitPaise,
                feesPaise = feesPaise,
                taxesPaise = taxesPaise,
                dateMs = System.currentTimeMillis(),
                accountId = accountId,
                notes = notes,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun updateValuation(investmentId: Long, newPricePerUnitPaise: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            addInvestmentUseCase.updateManualValuation(investmentId, newPricePerUnitPaise)
                .onSuccess {
                    _error.value = null
                    onSuccess()
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteInvestment(investmentId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            insightRepository.deleteInvestment(investmentId)
            onDone()
        }
    }

    fun clearError() {
        _error.value = null
    }
}


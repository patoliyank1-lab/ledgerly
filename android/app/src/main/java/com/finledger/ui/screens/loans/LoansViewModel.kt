package com.finledger.ui.screens.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.LoanRepaymentEntity
import com.finledger.data.local.entity.LoanType
import com.finledger.data.local.entity.PersonEntity
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.usecase.CreateLoanUseCase
import com.finledger.domain.usecase.GetAccountsUseCase
import com.finledger.domain.usecase.GetLoanBalanceUseCase
import com.finledger.domain.usecase.LoanWithBalance
import com.finledger.domain.usecase.RecordRepaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoansUiState(
    val loans: List<LoanWithBalance> = emptyList(),
    val totalLentOutstanding: Long = 0L,
    val totalBorrowedOutstanding: Long = 0L,
    val persons: List<PersonEntity> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

@HiltViewModel
class LoansViewModel @Inject constructor(
    getLoanBalanceUseCase: GetLoanBalanceUseCase,
    private val createLoanUseCase: CreateLoanUseCase,
    private val recordRepaymentUseCase: RecordRepaymentUseCase,
    private val commitmentRepository: CommitmentRepository,
    socialRepository: SocialRepository,
    getAccountsUseCase: GetAccountsUseCase,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LoansUiState> = combine(
        getLoanBalanceUseCase(),
        socialRepository.observeActivePersons(),
        getAccountsUseCase(),
        userPreferencesRepository.preferences,
        _error,
    ) { loans, persons, accounts, prefs, err ->
        val lent = loans.filter { it.loan.type == LoanType.LENT }.sumOf { it.outstandingPrincipalPaise }
        val borrowed = loans.filter { it.loan.type == LoanType.BORROWED }.sumOf { it.outstandingPrincipalPaise }
        LoansUiState(
            loans = loans,
            totalLentOutstanding = lent,
            totalBorrowedOutstanding = borrowed,
            persons = persons,
            accounts = accounts,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansUiState())

    fun observeRepayments(loanId: Long): StateFlow<List<LoanRepaymentEntity>> =
        commitmentRepository.observeRepaymentsForLoan(loanId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createLoan(
        type: LoanType,
        personOrOrg: String,
        personId: Long?,
        principalPaise: Long,
        dateGivenMs: Long,
        expectedRepaymentDateMs: Long?,
        interestRate: Double,
        repaymentFrequency: String,
        guarantorName: String,
        accountId: Long?,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            createLoanUseCase(
                type = type,
                personOrOrg = personOrOrg,
                personId = personId,
                principalPaise = principalPaise,
                dateGivenMs = dateGivenMs,
                expectedRepaymentDateMs = expectedRepaymentDateMs,
                interestRate = interestRate,
                repaymentFrequency = repaymentFrequency,
                guarantorName = guarantorName,
                accountId = accountId,
                notes = notes,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun recordRepayment(
        loanId: Long,
        principalPaise: Long,
        interestPaise: Long,
        feePaise: Long,
        dateMs: Long,
        accountId: Long?,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            recordRepaymentUseCase(
                loanId = loanId,
                principalPaise = principalPaise,
                interestPaise = interestPaise,
                feePaise = feePaise,
                dateMs = dateMs,
                accountId = accountId,
                notes = notes,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }
}


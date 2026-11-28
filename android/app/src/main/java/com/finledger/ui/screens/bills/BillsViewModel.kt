package com.finledger.ui.screens.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.BillEntity
import com.finledger.data.local.entity.BillFrequency
import com.finledger.data.local.entity.BillPaymentEntity
import com.finledger.data.local.entity.BillType
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.usecase.BillPaymentReceipt
import com.finledger.domain.usecase.CreateBillUseCase
import com.finledger.domain.usecase.GetAccountsUseCase
import com.finledger.domain.usecase.MarkBillPaidUseCase
import com.finledger.ui.screens.transactions.TransactionsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillsUiState(
    val bills: List<BillEntity> = emptyList(),
    val payments: List<BillPaymentEntity> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val lastUsedAccountId: Long? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val createBillUseCase: CreateBillUseCase,
    private val markBillPaidUseCase: MarkBillPaidUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BillsUiState> = combine<Any?, BillsUiState>(
        commitmentRepository.observeActiveBills(),
        commitmentRepository.observeAllBillPayments(),
        getAccountsUseCase(),
        categoryRepository.observeByType(CategoryType.EXPENSE),
        userPreferencesRepository.preferences,
        _error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val bills = values[0] as List<BillEntity>
        @Suppress("UNCHECKED_CAST")
        val payments = values[1] as List<BillPaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[2] as List<AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<CategoryEntity>
        val prefs = values[4] as com.finledger.domain.model.UserPreferences
        val err = values[5] as String?

        BillsUiState(
            bills = bills,
            payments = payments,
            accounts = accounts,
            categories = categories,
            primaryCurrency = prefs.primaryCurrency,
            lastUsedAccountId = prefs.lastUsedAccountId,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsUiState())

    fun saveBill(
        existing: BillEntity?,
        name: String,
        provider: String,
        type: BillType,
        expectedAmountPaise: Long,
        frequency: BillFrequency,
        nextDueDateMs: Long,
        accountId: Long?,
        categoryId: Long?,
        reminderDaysBefore: Int,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = nextDueDateMs }
            val bill = (existing ?: BillEntity(
                name = name,
                type = type,
                expectedAmount = expectedAmountPaise,
                dueDay = cal.get(java.util.Calendar.DAY_OF_MONTH),
                frequency = frequency,
            )).copy(
                name = name,
                provider = provider.trim(),
                type = type,
                expectedAmount = expectedAmountPaise,
                dueDay = cal.get(java.util.Calendar.DAY_OF_MONTH),
                nextDueDateMs = nextDueDateMs,
                frequency = frequency,
                paymentAccountId = accountId,
                categoryId = categoryId,
                reminderDaysBefore = reminderDaysBefore,
                notes = notes.trim(),
            )
            createBillUseCase(bill)
                .onSuccess { _error.value = null; onSuccess() }
                .onFailure { _error.value = it.message }
        }
    }

    fun quickPayBill(
        bill: BillEntity,
        onPaid: (BillPaymentReceipt) -> Unit,
        onNeedsDialog: () -> Unit,
    ) {
        val current = uiState.value
        val resolvedAccountId = resolveBillPayAccountId(bill, current.accounts, current.lastUsedAccountId)
        if (bill.expectedAmount <= 0L || resolvedAccountId == null) {
            onNeedsDialog()
            return
        }
        markBillPaid(
            bill = bill,
            actualAmountPaise = bill.expectedAmount,
            accountId = resolvedAccountId,
            onSuccess = onPaid,
        )
    }

    fun markBillPaid(
        bill: BillEntity,
        actualAmountPaise: Long,
        accountId: Long,
        onSuccess: (BillPaymentReceipt) -> Unit = {},
    ) {
        viewModelScope.launch {
            markBillPaidUseCase.markPaidWithReceipt(bill, actualAmountPaise, accountId, System.currentTimeMillis())
                .onSuccess { receipt ->
                    _error.value = null
                    userPreferencesRepository.setLastUsedAccountId(accountId)
                    onSuccess(receipt)
                }
                .onFailure { _error.value = it.message ?: "Failed to record bill payment" }
        }
    }

    suspend fun undoBillPaymentDirect(receipt: BillPaymentReceipt) {
        markBillPaidUseCase.undoBillPayment(receipt)
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch { commitmentRepository.deleteBill(id) }
    }

    suspend fun restoreBillDirect(id: Long) {
        commitmentRepository.restoreBill(id)
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun resolveBillPayAccountId(
            bill: BillEntity,
            accounts: List<AccountWithBalance>,
            lastUsedAccountId: Long?,
        ): Long? {
            val preferredBillAccount = bill.paymentAccountId
            if (preferredBillAccount != null && accounts.any { it.account.id == preferredBillAccount }) {
                return preferredBillAccount
            }
            return TransactionsViewModel.resolveDefaultAccountId(accounts, lastUsedAccountId)
        }
    }
}

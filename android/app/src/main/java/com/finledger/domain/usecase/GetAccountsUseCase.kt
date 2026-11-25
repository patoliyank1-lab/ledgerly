package com.finledger.domain.usecase

import com.finledger.data.repository.AccountRepository
import com.finledger.domain.model.AccountBalanceCalculator
import com.finledger.domain.model.AccountSummary
import com.finledger.domain.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<List<AccountWithBalance>> =
        accountRepository.observeActiveAccountsWithBalances()

    fun observeSummary(): Flow<AccountSummary> =
        accountRepository.observeActiveAccountsWithBalances().map {
            AccountBalanceCalculator.summarizeAccounts(it)
        }
}


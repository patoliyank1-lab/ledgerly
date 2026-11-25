package com.finledger.domain.usecase

import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.repository.AccountRepository
import javax.inject.Inject

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: AccountEntity): Result<Long> {
        if (account.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Account name cannot be empty"))
        }
        return runCatching {
            accountRepository.saveAccount(account.copy(name = account.name.trim()))
        }
    }
}


package com.finledger.data.local

import com.finledger.data.local.dao.AccountDao
import com.finledger.data.local.dao.CategoryDao
import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AccountType
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() {
        mutex.withLock {
            if (categoryDao.count() == 0) {
                categoryDao.insertAll(defaultCategories())
            }
            val prefs = userPreferencesRepository.preferences.first()
            val alreadySeeded = userPreferencesRepository.cashWalletSeeded.first()
            if (prefs.onboardingCompleted && !alreadySeeded) {
                seedInitialCashWalletInternal(prefs.primaryCurrency)
            }
        }
    }

    suspend fun seedInitialCashWallet(currency: String) {
        mutex.withLock {
            seedInitialCashWalletInternal(currency)
        }
    }

    private suspend fun seedInitialCashWalletInternal(currency: String) {
        if (userPreferencesRepository.cashWalletSeeded.first()) return
        if (accountDao.countActive() == 0) {
            accountDao.insert(
                AccountEntity(
                    name = "Cash Wallet",
                    type = AccountType.CASH,
                    bankOrProvider = "Cash",
                    currency = currency,
                    openingBalance = 0L,
                    iconName = "payments",
                )
            )
        }
        userPreferencesRepository.setCashWalletSeeded(true)
    }

    private fun defaultCategories(): List<CategoryEntity> = listOf(
        // Expense categories
        CategoryEntity(name = "Food & Dining", iconName = "restaurant", colorHex = "#0066CC", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 1),
        CategoryEntity(name = "Groceries", iconName = "shopping_cart", colorHex = "#1E7E34", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 2),
        CategoryEntity(name = "Transport", iconName = "directions_car", colorHex = "#0071E3", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 3),
        CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#6A1B9A", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 4),
        CategoryEntity(name = "Bills & Utilities", iconName = "receipt", colorHex = "#B26A00", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 5),
        CategoryEntity(name = "Rent & Housing", iconName = "home", colorHex = "#333333", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 6),
        CategoryEntity(name = "Health & Medical", iconName = "medical_services", colorHex = "#C62828", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 7),
        CategoryEntity(name = "Entertainment", iconName = "movie", colorHex = "#2997FF", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 8),
        CategoryEntity(name = "Travel", iconName = "flight", colorHex = "#00838F", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 9),
        CategoryEntity(name = "Education", iconName = "school", colorHex = "#283593", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 10),
        CategoryEntity(name = "Personal Care", iconName = "spa", colorHex = "#AD1457", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 11),
        CategoryEntity(name = "Investment Fee", iconName = "trending_up", colorHex = "#455A64", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 12),
        CategoryEntity(name = "Interest & Charges", iconName = "percent", colorHex = "#C62828", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 13),
        CategoryEntity(name = "Other Expense", iconName = "more_horiz", colorHex = "#7A7A7A", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 14),

        // Income categories
        CategoryEntity(name = "Salary", iconName = "work", colorHex = "#1E7E34", type = CategoryType.INCOME, isDefault = true, sortOrder = 20),
        CategoryEntity(name = "Freelance", iconName = "laptop", colorHex = "#0066CC", type = CategoryType.INCOME, isDefault = true, sortOrder = 21),
        CategoryEntity(name = "Business", iconName = "storefront", colorHex = "#283593", type = CategoryType.INCOME, isDefault = true, sortOrder = 22),
        CategoryEntity(name = "Rental Income", iconName = "apartment", colorHex = "#00838F", type = CategoryType.INCOME, isDefault = true, sortOrder = 23),
        CategoryEntity(name = "Interest & Dividends", iconName = "savings", colorHex = "#1E7E34", type = CategoryType.INCOME, isDefault = true, sortOrder = 24),
        CategoryEntity(name = "Gift", iconName = "card_giftcard", colorHex = "#6A1B9A", type = CategoryType.INCOME, isDefault = true, sortOrder = 25),
        CategoryEntity(name = "Other Income", iconName = "more_horiz", colorHex = "#7A7A7A", type = CategoryType.INCOME, isDefault = true, sortOrder = 26),
    )
}


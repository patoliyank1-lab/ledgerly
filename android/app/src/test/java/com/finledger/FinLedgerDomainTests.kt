package com.finledger

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.finledger.data.local.dao.*
import com.finledger.data.local.entity.*
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.InsightRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountBalanceCalculator
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.AppExperienceConfig
import com.finledger.domain.model.QuickTemplateCodec
import com.finledger.domain.model.QuickTransactionTemplate
import com.finledger.domain.model.SetupChecklistState
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.*
import com.finledger.domain.util.AmountExpressionEvaluator
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.CategoryPickerHelper
import com.finledger.ui.components.QuickDateHelper
import com.finledger.ui.components.UndoCoordinator
import com.finledger.ui.navigation.Screen
import com.finledger.ui.navigation.visibleBottomNavItems
import com.finledger.ui.navigation.visibleMoreSections
import com.finledger.ui.screens.analytics.AnalyticsViewModel
import com.finledger.ui.screens.dashboard.DashboardAlertKind
import com.finledger.ui.screens.dashboard.DashboardAlertSelector
import com.finledger.ui.screens.data.DataManagementViewModel
import com.finledger.ui.screens.events.formatSharedPerPersonLine
import com.finledger.ui.screens.friends.FriendsViewModel
import com.finledger.ui.screens.friends.PeopleTab
import com.finledger.ui.screens.friends.formatPersonBalanceWarning
import com.finledger.ui.screens.friends.visiblePeopleTabs
import com.finledger.ui.screens.security.AppLockManager
import com.finledger.ui.screens.security.OnboardingTrackingSelection
import com.finledger.ui.screens.settings.SettingsViewModel
import com.finledger.ui.screens.transactions.QuickAddTab
import com.finledger.ui.screens.transactions.QuickAddValidationResult
import com.finledger.ui.screens.transactions.QuickAddValidator
import com.finledger.ui.screens.transactions.TransactionsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.Calendar

class FinLedgerDomainTests {

    @Test
    fun `money formatter formats Indian rupees and parses paise accurately`() {
        assertEquals("₹1,23,456", MoneyFormatter.format(12_345_600L, "INR"))
        assertEquals("₹2,400.50", MoneyFormatter.format(240_050L, "INR"))
        assertEquals(240_050L, MoneyFormatter.parseToPaise("2400.50"))
    }

    @Test
    fun `account balance handles income, shared expense cashflow, transfer, and credit card bill payment`() {
        val bank = AccountEntity(id = 1L, name = "Bank", type = AccountType.SAVINGS, openingBalance = 100_000L) // ₹1000
        val card = AccountEntity(id = 2L, name = "Card", type = AccountType.CREDIT_CARD, openingBalance = 0L, creditLimit = 500_000L)

        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 500_000L, date = 1L, accountId = 1L), // +₹5000
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 240_000L, personalAmount = 60_000L, date = 2L, accountId = 1L, isPersonal = false), // -₹2400 cashflow
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 100_000L, personalAmount = 100_000L, date = 3L, accountId = 2L), // ₹1000 on card
            TransactionEntity(id = 4, type = TransactionType.TRANSFER, amount = 40_000L, personalAmount = 0L, date = 4L, accountId = 1L, toAccountId = 2L), // pay ₹400 card bill
        )

        assertEquals(320_000L, AccountBalanceCalculator.computeBalance(bank, txs)) // 1000 + 5000 - 2400 - 400 = ₹3200
        assertEquals(60_000L, AccountBalanceCalculator.computeBalance(card, txs))  // 1000 - 400 = ₹600 outstanding
    }

    @Test
    fun `split calculator preserves exact paise sum across equal, percentage, weight, and item splits`() {
        val equal = SplitCalculator.splitEqually(10_000L, listOf(0L, 1L, 2L))
        assertEquals(10_000L, equal.values.sum())

        val pct = SplitCalculator.splitByPercentages(10_001L, mapOf(0L to 33.3, 1L to 33.3, 2L to 33.4))
        assertEquals(10_001L, pct.values.sum())

        val weights = SplitCalculator.splitByWeights(10_000L, mapOf(0L to 1.0, 1L to 2.0))
        assertEquals(10_000L, weights.values.sum())
        assertEquals(3333L, weights[0L])
        assertEquals(6667L, weights[1L])

        val items = SplitCalculator.splitByItems(
            listOf(
                ItemSplitInput("Pizza", 60_000L, listOf(0L, 1L, 2L)),
                ItemSplitInput("Coffee", 20_000L, listOf(1L, 2L)),
            )
        )
        assertEquals(20_000L, items[0L])
        assertEquals(30_000L, items[1L])
        assertEquals(30_000L, items[2L])
    }

    @Test
    fun `bilateral delta matches critical shared expense rule (2400 paid by user for 4)`() {
        val payers = listOf(ExpensePayerEntity(1L, SplitCalculator.USER_PERSON_ID, 240_000L))
        val splits = listOf(0L, 1L, 2L, 3L).map { ExpenseSplitEntity(sharedExpenseId = 1L, personId = it, shareAmount = 60_000L) }

        val deltas = BalanceCalculator.bilateralDeltasForExpense(payers, splits)
        assertEquals(60_000L, deltas[1L])
        assertEquals(60_000L, deltas[2L])
        assertEquals(60_000L, deltas[3L])
        assertEquals(180_000L, deltas.values.sum())
        assertEquals(60_000L, PersonalExpenseValidator.resolveUserPersonalShare(splits))
    }

    @Test
    fun `debt simplification preserves net balances and reduces payment count`() {
        val simplified = BalanceCalculator.simplifyDebts(
            mapOf(0L to 100_000L, 1L to -40_000L, 2L to -60_000L)
        )
        assertEquals(2, simplified.size)
        assertEquals(100_000L, simplified.sumOf { it.amountPaise })
    }

    @Test
    fun `csv parser and validator parse valid rows and catch invalid rows`() {
        val csv = """
            Date,Amount,Merchant
            2026-11-20,450.50,Cafe Coffee Day
            bad-date,100,Test
        """.trimIndent()
        val table = CsvParser.parse(csv)
        assertEquals(2, table.rows.size)

        val mapping = ColumnMapping(dateIndex = 0, amountIndex = 1, merchantIndex = 2)
        val r1 = ImportValidator.validateRow(1, table.rows[0], mapping, 1L, "INR")
        val r2 = ImportValidator.validateRow(2, table.rows[1], mapping, 1L, "INR")

        assertEquals(ImportRowStatus.VALID, r1.status)
        assertEquals(45_050L, r1.parsedTransaction?.amount)
        assertEquals(ImportRowStatus.INVALID, r2.status)
        assertTrue(r2.errorReason?.contains("Invalid date") == true)
    }

    @Test
    fun `quick date helper resolves today and yesterday while preserving time of day`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 9, 15, 30)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val reference = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 18, 42, 10)
            set(Calendar.MILLISECOND, 123)
        }.timeInMillis

        val today = QuickDateHelper.todayWithTimeOfDay(referenceMs = reference, nowMs = now)
        val yesterday = QuickDateHelper.yesterdayWithTimeOfDay(referenceMs = reference, nowMs = now)

        assertTrue(QuickDateHelper.isSameLocalDay(today, now))
        assertFalse(QuickDateHelper.isSameLocalDay(yesterday, now))

        val yesterdayCal = Calendar.getInstance().apply { timeInMillis = yesterday }
        assertEquals(2026, yesterdayCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, yesterdayCal.get(Calendar.MONTH))
        assertEquals(24, yesterdayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, yesterdayCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(42, yesterdayCal.get(Calendar.MINUTE))
        assertEquals(123, yesterdayCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `resolveDefaultAccountId prefers last used active account and falls back safely`() {
        val accounts = listOf(
            AccountWithBalance(AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH), 0L),
            AccountWithBalance(AccountEntity(id = 7L, name = "Bank", type = AccountType.SAVINGS), 50_000L),
        )
        assertEquals(7L, TransactionsViewModel.resolveDefaultAccountId(accounts, 7L))
        assertEquals(1L, TransactionsViewModel.resolveDefaultAccountId(accounts, 999L))
        assertEquals(1L, TransactionsViewModel.resolveDefaultAccountId(accounts, null))
        assertEquals(null, TransactionsViewModel.resolveDefaultAccountId(emptyList(), 7L))
    }

    @Test
    fun `supported currencies include SGD and map distinct symbols`() {
        assertEquals(listOf("INR", "USD", "EUR", "GBP", "AED", "SGD"), MoneyFormatter.SUPPORTED_CURRENCIES)
        MoneyFormatter.SUPPORTED_CURRENCIES.forEach { code ->
            assertTrue(MoneyFormatter.currencySymbol(code).isNotBlank())
        }
    }

    @Test
    fun `app lock manager enforces timeout thresholds and picker suppression`() {
        val manager = AppLockManager()

        // Cold start locks when PIN enabled
        manager.lockIfEnabled(true)
        assertTrue(manager.isLocked.value)
        manager.unlock()
        assertFalse(manager.isLocked.value)

        // Within 60s grace period stays unlocked
        manager.onAppBackgrounded(pinEnabled = true, nowMs = 10_000L)
        manager.onAppForegrounded(pinEnabled = true, timeoutSeconds = 60, nowMs = 55_000L)
        assertFalse(manager.isLocked.value)

        // Past 60s threshold locks
        manager.onAppBackgrounded(pinEnabled = true, nowMs = 100_000L)
        manager.onAppForegrounded(pinEnabled = true, timeoutSeconds = 60, nowMs = 160_000L)
        assertTrue(manager.isLocked.value)
        manager.unlock()

        // Immediate mode (0s) locks normal backgrounding
        manager.onAppBackgrounded(pinEnabled = true, nowMs = 200_000L)
        manager.onAppForegrounded(pinEnabled = true, timeoutSeconds = 0, nowMs = 200_050L)
        assertTrue(manager.isLocked.value)
        manager.unlock()

        // Picker suppression skips lock even when timeout is 0s and picker stayed open 90s
        manager.suppressNextBackgroundLock(nowMs = 300_000L)
        manager.onAppBackgrounded(pinEnabled = true, nowMs = 300_100L)
        manager.onAppForegrounded(pinEnabled = true, timeoutSeconds = 0, nowMs = 390_000L)
        assertFalse(manager.isLocked.value)

        // Expired suppression (>5s) does NOT skip later unrelated backgrounding
        manager.suppressNextBackgroundLock(nowMs = 400_000L)
        manager.onAppBackgrounded(pinEnabled = true, nowMs = 410_000L)
        manager.onAppForegrounded(pinEnabled = true, timeoutSeconds = 0, nowMs = 410_100L)
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun `computeTopCategoryIds orders by frequency and falls back to seeded order`() {
        val categories = (1L..8L).map { id ->
            CategoryEntity(
                id = id,
                name = "Cat $id",
                iconName = "category",
                colorHex = "#607D8B",
                type = CategoryType.EXPENSE,
            )
        }

        // Empty history falls back to first 6 categories in seeded order
        assertEquals(
            listOf(1L, 2L, 3L, 4L, 5L, 6L),
            TransactionsViewModel.computeTopCategoryIds(emptyList(), categories, TransactionType.EXPENSE),
        )

        // Frequency beats default order; deleted/income transactions are ignored for EXPENSE
        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 100L, date = 10L, accountId = 1L, categoryId = 7L),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 100L, date = 20L, accountId = 1L, categoryId = 7L),
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 100L, date = 30L, accountId = 1L, categoryId = 7L),
            TransactionEntity(id = 4, type = TransactionType.EXPENSE, amount = 100L, date = 40L, accountId = 1L, categoryId = 8L),
            TransactionEntity(id = 5, type = TransactionType.EXPENSE, amount = 100L, date = 50L, accountId = 1L, categoryId = 8L),
            TransactionEntity(id = 6, type = TransactionType.EXPENSE, amount = 100L, date = 60L, accountId = 1L, categoryId = 3L),
            TransactionEntity(id = 7, type = TransactionType.INCOME, amount = 500L, date = 70L, accountId = 1L, categoryId = 5L),
            TransactionEntity(id = 8, type = TransactionType.INCOME, amount = 500L, date = 80L, accountId = 1L, categoryId = 5L),
            TransactionEntity(id = 9, type = TransactionType.EXPENSE, amount = 100L, date = 90L, accountId = 1L, categoryId = 6L, isDeleted = true),
        )

        assertEquals(
            listOf(7L, 8L, 3L, 1L, 2L, 4L),
            TransactionsViewModel.computeTopCategoryIds(txs, categories, TransactionType.EXPENSE),
        )
    }

    @Test
    fun `category picker helper prioritizes top ids and toggles compact limit`() {
        val categories = (1L..8L).map { id ->
            CategoryEntity(
                id = id,
                name = "Cat $id",
                iconName = "category",
                colorHex = "#607D8B",
                type = CategoryType.EXPENSE,
            )
        }

        val collapsed = CategoryPickerHelper.orderAndSlice(
            categories = categories,
            preferredOrderIds = listOf(8L, 6L, 4L),
            compactLimit = 6,
            showAll = false,
        )
        assertTrue(collapsed.hasMore)
        assertEquals(listOf(8L, 6L, 4L, 1L, 2L, 3L), collapsed.visibleCategories.map { it.id })

        val expanded = CategoryPickerHelper.orderAndSlice(
            categories = categories,
            preferredOrderIds = listOf(8L, 6L, 4L),
            compactLimit = 6,
            showAll = true,
        )
        assertTrue(expanded.hasMore)
        assertEquals(listOf(8L, 6L, 4L, 1L, 2L, 3L, 5L, 7L), expanded.visibleCategories.map { it.id })

        val exactFit = CategoryPickerHelper.orderAndSlice(
            categories = categories.take(6),
            compactLimit = 6,
            showAll = false,
        )
        assertFalse(exactFit.hasMore)
        assertEquals(6, exactFit.visibleCategories.size)
    }

    @Test
    fun `quick add validator enforces positive amount, accounts, and distinct transfer endpoints`() {
        val blankAmount = QuickAddValidator.validate(QuickAddTab.EXPENSE, "", 1L, null)
        assertTrue(blankAmount is QuickAddValidationResult.Invalid && blankAmount.isAmountError)

        val zeroAmount = QuickAddValidator.validate(QuickAddTab.INCOME, "0", 1L, null)
        assertTrue(zeroAmount is QuickAddValidationResult.Invalid && zeroAmount.isAmountError)

        val missingAccount = QuickAddValidator.validate(QuickAddTab.EXPENSE, "250", null, null)
        assertTrue(missingAccount is QuickAddValidationResult.Invalid && !missingAccount.isAmountError)

        val sameTransferAccounts = QuickAddValidator.validate(QuickAddTab.TRANSFER, "500", 2L, 2L)
        assertEquals(
            QuickAddValidationResult.Invalid("Source and destination accounts must differ"),
            sameTransferAccounts,
        )

        val validExpense = QuickAddValidator.validate(QuickAddTab.EXPENSE, "240.50", 1L, 1L)
        assertEquals(QuickAddValidationResult.ValidExpenseOrIncome(24_050L, 1L), validExpense)

        val validTransfer = QuickAddValidator.validate(QuickAddTab.TRANSFER, "1000", 1L, 2L)
        assertEquals(QuickAddValidationResult.ValidTransfer(100_000L, 1L, 2L), validTransfer)
    }

    @Test
    fun `merchant history helper ranks frequent merchants, filters queries, and suggests categories`() {
        val food = CategoryEntity(id = 1L, name = "Food", iconName = "restaurant", colorHex = "#FF0000", type = CategoryType.EXPENSE)
        val travel = CategoryEntity(id = 2L, name = "Travel", iconName = "flight", colorHex = "#00FF00", type = CategoryType.EXPENSE)
        val salary = CategoryEntity(id = 10L, name = "Salary", iconName = "work", colorHex = "#0000FF", type = CategoryType.INCOME)

        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 200L, date = 10L, accountId = 1L, categoryId = 1L, merchantOrPayee = "Swiggy"),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 300L, date = 20L, accountId = 1L, categoryId = 1L, merchantOrPayee = "swiggy "),
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 150L, date = 30L, accountId = 1L, categoryId = 2L, merchantOrPayee = "Swiggy"),
            TransactionEntity(id = 4, type = TransactionType.EXPENSE, amount = 400L, date = 40L, accountId = 1L, categoryId = 2L, merchantOrPayee = "Uber"),
            TransactionEntity(id = 5, type = TransactionType.EXPENSE, amount = 500L, date = 50L, accountId = 1L, categoryId = 1L, merchantOrPayee = "Blue Tokai"),
            TransactionEntity(id = 6, type = TransactionType.INCOME, amount = 50000L, date = 60L, accountId = 1L, categoryId = 10L, merchantOrPayee = "Acme Corp"),
            TransactionEntity(id = 7, type = TransactionType.EXPENSE, amount = 999L, date = 70L, accountId = 1L, categoryId = 2L, merchantOrPayee = "DeletedShop", isDeleted = true),
        )

        val stats = TransactionsViewModel.computeMerchantHistoryStats(
            transactions = txs,
            expenseCategories = listOf(food, travel),
            incomeCategories = listOf(salary),
        )

        assertEquals(listOf("Swiggy", "Blue Tokai", "Uber"), stats.topExpenseMerchants)
        assertEquals(listOf("Acme Corp"), stats.topIncomeSources)
        assertEquals(1L, TransactionsViewModel.suggestCategoryForMerchant("SWIGGY", stats.expenseCategoryByMerchant))
        assertEquals(2L, TransactionsViewModel.suggestCategoryForMerchant("uber", stats.expenseCategoryByMerchant))
        assertEquals(10L, TransactionsViewModel.suggestCategoryForMerchant("acme corp", stats.incomeCategoryBySource))
        assertEquals(null, TransactionsViewModel.suggestCategoryForMerchant("DeletedShop", stats.expenseCategoryByMerchant))

        assertEquals(
            listOf("Swiggy"),
            TransactionsViewModel.filterMerchantSuggestions(stats.topExpenseMerchants, "sw"),
        )
        assertEquals(
            emptyList<String>(),
            TransactionsViewModel.filterMerchantSuggestions(stats.topExpenseMerchants, "Swiggy"),
        )
    }

    @Test
    fun `findLastRepeatableTransaction picks newest active expense income or transfer`() {
        assertEquals(null, TransactionsViewModel.findLastRepeatableTransaction(emptyList()))

        val items = listOf(
            TransactionWithDetails(
                transaction = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 2500L, date = 100L, accountId = 1L, createdAt = 100L),
                accountName = "Bank",
            ),
            TransactionWithDetails(
                transaction = TransactionEntity(id = 2L, type = TransactionType.ACCOUNT_ADJUSTMENT, amount = 9000L, date = 500L, accountId = 1L, createdAt = 500L),
                accountName = "Bank",
            ),
            TransactionWithDetails(
                transaction = TransactionEntity(id = 3L, type = TransactionType.INCOME, amount = 100_000L, date = 300L, accountId = 1L, createdAt = 300L, isDeleted = true),
                accountName = "Bank",
            ),
            TransactionWithDetails(
                transaction = TransactionEntity(id = 4L, type = TransactionType.TRANSFER, amount = 50_000L, date = 250L, accountId = 1L, toAccountId = 2L, createdAt = 250L),
                accountName = "Bank",
            ),
        )

        val picked = TransactionsViewModel.findLastRepeatableTransaction(items)
        assertEquals(4L, picked?.transaction?.id)
    }

    @Test
    fun `quick template codec handles roundtrip serialization, deletion, and corrupt json`() {
        assertEquals(emptyList<QuickTransactionTemplate>(), QuickTemplateCodec.decode(null))
        assertEquals(emptyList<QuickTransactionTemplate>(), QuickTemplateCodec.decode(""))
        assertEquals(emptyList<QuickTransactionTemplate>(), QuickTemplateCodec.decode("{invalid json"))

        val tea = QuickTransactionTemplate(
            id = "tpl-tea",
            label = "Tea",
            type = TransactionType.EXPENSE,
            amountPaise = 2_000L,
            accountId = 1L,
            categoryId = 2L,
            merchantOrPayee = "Chai Point",
        )
        val auto = QuickTransactionTemplate(
            id = "tpl-auto",
            label = "Auto",
            type = TransactionType.EXPENSE,
            amountPaise = 8_000L,
            accountId = 1L,
            categoryId = 3L,
        )

        val added = QuickTemplateCodec.addTemplate(listOf(tea), auto)
        val encoded = QuickTemplateCodec.encode(added)
        val decoded = QuickTemplateCodec.decode(encoded)
        assertEquals(listOf(auto, tea), decoded)

        val afterDelete = QuickTemplateCodec.removeTemplate(decoded, "tpl-tea")
        assertEquals(listOf(auto), afterDelete)
    }

    @Test
    fun `amount expression evaluator handles sums, decimals, invalid syntax, and overflow`() {
        assertEquals(33_000L, AmountExpressionEvaluator.evaluateToPaise("250+80"))
        assertEquals(37_950L, AmountExpressionEvaluator.evaluateToPaise("500-120.50"))
        assertEquals(24_050L, AmountExpressionEvaluator.evaluateToPaise("240.50"))
        assertEquals(-5_000L, AmountExpressionEvaluator.evaluateToPaise("100-150"))

        assertEquals(null, AmountExpressionEvaluator.evaluateToPaise("100+"))
        assertEquals(null, AmountExpressionEvaluator.evaluateToPaise(""))
        assertEquals(null, AmountExpressionEvaluator.evaluateToPaise("abc"))
        assertEquals(null, AmountExpressionEvaluator.evaluateToPaise("100++20"))
        assertEquals(null, AmountExpressionEvaluator.evaluateToPaise("++"))
        assertEquals(null, AmountExpressionEvaluator.evaluateToPaise("90000000000000000+90000000000000000"))

        val negativeResult = QuickAddValidator.validate(QuickAddTab.EXPENSE, "100-150", 1L, null)
        assertTrue(negativeResult is QuickAddValidationResult.Invalid && negativeResult.isAmountError)

        val validSum = QuickAddValidator.validate(QuickAddTab.EXPENSE, "250+80", 1L, null)
        assertEquals(QuickAddValidationResult.ValidExpenseOrIncome(33_000L, 1L), validSum)
    }

    private inline fun <reified T : Any> recordingProxy(calls: MutableList<String>, tag: String): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, args ->
            val nonContArgs = args?.dropLast(1).orEmpty().joinToString(",")
            calls += "$tag.${method.name}($nonContArgs)"
            when (method.name) {
                "insert" -> 1L
                else -> Unit
            }
        } as T

    @Test
    fun `repositories expose softDelete and restore roundtrips across all soft-deletable entities`() = runTest {
        val calls = mutableListOf<String>()
        val auditDao = recordingProxy<AuditLogDao>(calls, "audit")
        val txDao = recordingProxy<TransactionDao>(calls, "tx")

        val socialRepo = SocialRepository(
            personDao = recordingProxy(calls, "person"),
            groupDao = recordingProxy(calls, "group"),
            eventDao = recordingProxy(calls, "event"),
            sharedExpenseDao = recordingProxy(calls, "sharedExpense"),
            settlementDao = recordingProxy(calls, "settlement"),
            transactionDao = txDao,
            auditLogDao = auditDao,
        )
        socialRepo.deletePerson(5L)
        socialRepo.restorePerson(5L)
        socialRepo.deleteSharedExpense(9L)
        socialRepo.restoreSharedExpense(9L)
        val settlement = SettlementEntity(
            id = 11L,
            personId = 5L,
            direction = SettlementDirection.THEY_PAID_ME,
            amount = 5000L,
            date = 1L,
            linkedTransactionId = 42L,
        )
        socialRepo.deleteSettlement(settlement)
        socialRepo.restoreSettlement(settlement)

        assertTrue(calls.contains("person.softDelete(5)"))
        assertTrue(calls.contains("person.restore(5)"))
        assertTrue(calls.any { it.startsWith("tx.softDeleteBySharedExpenseId(9") })
        assertTrue(calls.any { it.startsWith("tx.restoreBySharedExpenseId(9") })
        assertTrue(calls.any { it.startsWith("tx.setDeleted(42,false") })
        assertTrue(calls.contains("settlement.restore(11)"))

        val commitmentRepo = CommitmentRepository(
            loanDao = recordingProxy(calls, "loan"),
            billDao = recordingProxy(calls, "bill"),
            budgetDao = recordingProxy(calls, "budget"),
            savingsGoalDao = recordingProxy(calls, "goal"),
            auditLogDao = auditDao,
        )
        commitmentRepo.deleteLoan(1L); commitmentRepo.restoreLoan(1L)
        commitmentRepo.deleteBill(2L); commitmentRepo.restoreBill(2L)
        commitmentRepo.deleteBudget(3L); commitmentRepo.restoreBudget(3L)
        commitmentRepo.deleteSavingsGoal(4L); commitmentRepo.restoreSavingsGoal(4L)
        assertTrue(calls.containsAll(listOf("loan.restore(1)", "bill.restore(2)", "budget.restore(3)", "goal.restore(4)")))

        val insightRepo = InsightRepository(
            investmentDao = recordingProxy(calls, "investment"),
            netWorthDao = recordingProxy(calls, "netWorth"),
            auditLogDao = auditDao,
        )
        insightRepo.deleteInvestment(7L)
        insightRepo.restoreInvestment(7L)
        assertTrue(calls.containsAll(listOf("investment.softDelete(7)", "investment.restore(7)")))
    }

    @Test
    fun `undo coordinator formats messages, performs undo callback, and clears state`() = runTest {
        val coordinator = UndoCoordinator()
        var restoredId: Long? = null

        coordinator.showDeleted("Coffee") { restoredId = 99L }
        val event = coordinator.currentEvent.value
        assertEquals("Deleted Coffee", event?.message)
        assertEquals("UNDO", event?.actionLabel)

        coordinator.performUndo(event!!)
        assertEquals(99L, restoredId)
        assertEquals(null, coordinator.currentEvent.value)

        // Newer event clears safely only on matching id
        coordinator.showDeleted("Old") {}
        val firstId = coordinator.currentEvent.value!!.id
        coordinator.showDeleted("New") {}
        coordinator.clearIfMatches(firstId)
        assertEquals("Deleted New", coordinator.currentEvent.value?.message)
        coordinator.dismiss()
        assertEquals(null, coordinator.currentEvent.value)
    }

    @Test
    fun `formatPersonBalanceWarning guards unsettled balances and returns null when settled`() {
        assertEquals(null, formatPersonBalanceWarning("Raj", 0L, "INR"))
        assertEquals("Raj owes you ₹500", formatPersonBalanceWarning("Raj", 50_000L, "INR"))
        assertEquals("You owe Raj ₹500", formatPersonBalanceWarning("Raj", -50_000L, "INR"))
    }

    @Test
    fun `formatSharedPerPersonLine computes per person equal split and guards empty selection`() {
        assertEquals(
            "Each person pays ₹300",
            formatSharedPerPersonLine(90_000L, 3, SplitMethod.EQUAL, "INR"),
        )
        assertEquals(
            "Select at least 1 participant",
            formatSharedPerPersonLine(90_000L, 0, SplitMethod.EQUAL, "INR"),
        )
        assertEquals(
            "Items total: ₹500",
            formatSharedPerPersonLine(50_000L, 2, SplitMethod.ITEM, "INR"),
        )
    }

    @Test
    fun `executeQuickSettle records settlement with linked transaction and undo deletes both`() = runTest {
        val insertedTxs = mutableListOf<TransactionEntity>()
        val settlements = mutableMapOf<Long, SettlementEntity>()
        val auditDao = recordingProxy<AuditLogDao>(mutableListOf(), "audit")

        val txRepo = Proxy.newProxyInstance(
            com.finledger.data.repository.TransactionRepository::class.java.classLoader,
            arrayOf(com.finledger.data.repository.TransactionRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "insert" -> {
                    val tx = args[0] as TransactionEntity
                    val saved = tx.copy(id = 77L)
                    insertedTxs += saved
                    77L
                }
                else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as com.finledger.data.repository.TransactionRepository

        val txDaoCalls = mutableListOf<String>()
        val txDao = recordingProxy<TransactionDao>(txDaoCalls, "tx")

        val settlementDao = Proxy.newProxyInstance(
            SettlementDao::class.java.classLoader,
            arrayOf(SettlementDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "insert" -> {
                    val s = (args[0] as SettlementEntity).copy(id = 55L)
                    settlements[55L] = s
                    55L
                }
                "getById" -> settlements[args[0] as Long]
                "softDelete" -> {
                    val id = args[0] as Long
                    settlements[id]?.let { settlements[id] = it.copy(isDeleted = true) }
                    Unit
                }
                else -> Unit
            }
        } as SettlementDao

        val socialRepo = SocialRepository(
            personDao = recordingProxy(mutableListOf(), "person"),
            groupDao = recordingProxy(mutableListOf(), "group"),
            eventDao = recordingProxy(mutableListOf(), "event"),
            sharedExpenseDao = recordingProxy(mutableListOf(), "shared"),
            settlementDao = settlementDao,
            transactionDao = txDao,
            auditLogDao = auditDao,
        )

        val useCase = RecordSettlementUseCase(socialRepo, txRepo)
        val result = FriendsViewModel.executeQuickSettle(
            personId = 9L,
            amountPaise = 50_000L,
            direction = SettlementDirection.RECEIVED,
            accountId = 3L,
            groupId = 12L,
            recordSettlementUseCase = useCase,
            nowMs = 1_000L,
        )

        assertTrue(result.isSuccess)
        assertEquals(55L, result.getOrNull())
        assertEquals(TransactionType.REPAYMENT_RECEIVED, insertedTxs.single().type)
        assertEquals(3L, insertedTxs.single().accountId)

        socialRepo.deleteSettlementById(55L)
        assertEquals(true, settlements[55L]?.isDeleted)
        assertTrue(txDaoCalls.any { it.startsWith("tx.setDeleted(77,true") })
    }

    @Test
    fun `markBillPaid advances due date and undoBillPayment restores due date and deletes payment and tx`() = runTest {
        val initialDue = 1_764_000_000_000L
        var storedBill = BillEntity(
            id = 5L,
            name = "Fiber Internet",
            type = BillType.UTILITY,
            expectedAmount = 99_900L,
            dueDay = 10,
            frequency = BillFrequency.MONTHLY,
            nextDueDateMs = initialDue,
            paymentAccountId = 2L,
        )
        val billPayments = mutableMapOf<Long, BillPaymentEntity>()
        val softDeletedTxIds = mutableListOf<Long>()
        val auditDao = recordingProxy<AuditLogDao>(mutableListOf(), "audit")

        val billDao = Proxy.newProxyInstance(
            BillDao::class.java.classLoader,
            arrayOf(BillDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getById" -> storedBill.takeIf { it.id == args[0] as Long }
                "updateBill" -> {
                    storedBill = args[0] as BillEntity
                    Unit
                }
                "insertPayment" -> {
                    val payment = (args[0] as BillPaymentEntity).copy(id = 88L)
                    billPayments[88L] = payment
                    88L
                }
                "softDeletePayment" -> {
                    val pid = args[0] as Long
                    billPayments[pid]?.let { billPayments[pid] = it.copy(isDeleted = true) }
                    Unit
                }
                else -> Unit
            }
        } as BillDao

        val commitmentRepo = CommitmentRepository(
            loanDao = recordingProxy(mutableListOf(), "loan"),
            billDao = billDao,
            budgetDao = recordingProxy(mutableListOf(), "budget"),
            savingsGoalDao = recordingProxy(mutableListOf(), "goal"),
            auditLogDao = auditDao,
        )
        val txRepo = Proxy.newProxyInstance(
            TransactionRepository::class.java.classLoader,
            arrayOf(TransactionRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "insert" -> 301L
                "softDelete" -> {
                    softDeletedTxIds += args[0] as Long
                    Unit
                }
                else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as TransactionRepository

        val useCase = MarkBillPaidUseCase(commitmentRepo, txRepo)
        val receipt = useCase.markPaidWithReceipt(
            bill = storedBill,
            actualAmountPaise = 99_900L,
            accountId = 2L,
            paidDateMs = 1_764_100_000_000L,
        ).getOrThrow()

        assertEquals(88L, receipt.paymentId)
        assertEquals(301L, receipt.transactionId)
        assertEquals(initialDue, receipt.previousDueDateMs)
        assertTrue(storedBill.nextDueDateMs > initialDue)
        assertEquals(false, billPayments[88L]?.isDeleted)

        val undoResult = useCase.undoBillPayment(receipt)
        assertTrue(undoResult.isSuccess)
        assertEquals(true, billPayments[88L]?.isDeleted)
        assertEquals(listOf(301L), softDeletedTxIds)
        assertEquals(initialDue, storedBill.nextDueDateMs)
    }

    @Test
    fun `suggestBudgetsFromHistory computes rounded 3-month personal spend averages and skips existing or tiny categories`() {
        val now = 1_764_200_000_000L
        val thirtyDaysMs = 30L * 86_400_000L
        val food = CategoryEntity(id = 1L, name = "Food", iconName = "restaurant", colorHex = "#E65100", type = CategoryType.EXPENSE)
        val travel = CategoryEntity(id = 2L, name = "Travel", iconName = "flight", colorHex = "#0288D1", type = CategoryType.EXPENSE)
        val coffee = CategoryEntity(id = 3L, name = "Coffee", iconName = "coffee", colorHex = "#6D4C41", type = CategoryType.EXPENSE)
        val rent = CategoryEntity(id = 4L, name = "Rent", iconName = "home", colorHex = "#2E7D32", type = CategoryType.EXPENSE)

        fun tx(id: Long, catId: Long, personalPaise: Long, date: Long, type: TransactionType = TransactionType.EXPENSE) =
            TransactionWithDetails(
                transaction = TransactionEntity(
                    id = id,
                    type = type,
                    amount = personalPaise,
                    personalAmount = personalPaise,
                    amountInPrimary = personalPaise,
                    date = date,
                    accountId = 1L,
                    categoryId = catId,
                ),
                accountName = "Bank",
                category = null,
            )

        val transactions = listOf(
            // Food: ₹3,600 over 3 months -> avg ₹1,200 (120_000 paise)
            tx(1L, 1L, 120_000L, now - 10L * 86_400_000L),
            tx(2L, 1L, 260_000L, now - thirtyDaysMs),
            tx(3L, 1L, 20_000L, now - thirtyDaysMs, TransactionType.REFUND),
            // Travel: ₹1,026 over 3 months -> avg ₹342 -> rounded to ₹350 (35_000 paise)
            tx(4L, 2L, 102_600L, now - 5L * 86_400_000L),
            // Coffee: ₹1.50 total -> avg ₹0.50 (< ₹1 unit -> skipped)
            tx(5L, 3L, 150L, now - 2L * 86_400_000L),
            // Rent: ₹30,000 total, but already has active MONTHLY budget -> skipped
            tx(6L, 4L, 3_000_000L, now - 15L * 86_400_000L),
            // Out-of-window expense (> 3 months ago) -> ignored
            tx(7L, 1L, 900_000L, now - 130L * 86_400_000L),
        )

        // Include friend-paid shared expense split for Travel (+₹300 total -> Travel becomes ₹1,326 total / 3 = ₹442 avg -> ₹450)
        val friendExpense = SharedExpenseEntity(
            id = 20L,
            totalAmount = 60_000L,
            description = "Cab Share",
            date = now - 3L * 86_400_000L,
            categoryId = 2L,
            splitMethod = SplitMethod.EQUAL,
        )
        val friendSplit = ExpenseSplitEntity(
            id = 1L,
            sharedExpenseId = 20L,
            personId = SplitCalculator.USER_PERSON_ID,
            shareAmount = 30_000L,
        )

        val existingBudgets = listOf(
            BudgetEntity(
                id = 10L,
                name = "Rent",
                amount = 1_500_000L,
                period = BudgetPeriod.MONTHLY,
                startDate = now - 60L * 86_400_000L,
                categoryId = 4L,
            ),
        )

        val suggestions = SuggestBudgetsFromHistoryUseCase.computeSuggestions(
            categories = listOf(food, travel, coffee, rent),
            existingBudgets = existingBudgets,
            transactions = transactions,
            sharedExpenses = listOf(friendExpense),
            splits = listOf(friendSplit),
            nowMs = now,
        )

        assertEquals(listOf(1L, 2L), suggestions.map { it.categoryId })
        assertEquals(120_000L, suggestions[0].suggestedLimitPaise)
        assertEquals(45_000L, suggestions[1].suggestedLimitPaise)
    }

    @Test
    fun `appExperienceConfig resolves safe upgrade defaults and presets cleanly`() {
        // Default UserPreferences starts in Simple Mode
        val defaults = UserPreferences()
        assertTrue(defaults.isSimpleMode)
        assertTrue(defaults.trackSharedAndPeople)
        assertTrue(defaults.trackPlanning)
        assertFalse(defaults.trackLoans)
        assertFalse(defaults.trackInvestments)
        assertFalse(defaults.showDashboardNetWorth)
        assertTrue(defaults.showDashboardAlerts)

        // Upgrading user (onboarding already completed, no stored mode) keeps full visibility
        val upgraded = AppExperienceConfig.resolve(
            onboardingCompleted = true,
            isSimpleModeStored = null,
            trackSharedAndPeopleStored = null,
            trackLoansStored = null,
            trackInvestmentsStored = null,
            trackPlanningStored = null,
            showDashboardNetWorthStored = null,
            showDashboardAlertsStored = null,
        )
        assertFalse(upgraded.isSimpleMode)
        assertTrue(upgraded.trackLoans)
        assertTrue(upgraded.trackInvestments)
        assertTrue(upgraded.showDashboardNetWorth)

        // Fresh install resolves to Simple Mode
        val fresh = AppExperienceConfig.resolve(
            onboardingCompleted = false,
            isSimpleModeStored = null,
            trackSharedAndPeopleStored = null,
            trackLoansStored = null,
            trackInvestmentsStored = null,
            trackPlanningStored = null,
            showDashboardNetWorthStored = null,
            showDashboardAlertsStored = null,
        )
        assertTrue(fresh.isSimpleMode)
        assertFalse(fresh.trackLoans)

        // Presets configure Simple vs Advanced deterministically
        val advanced = AppExperienceConfig.applyAdvancedPreset(defaults)
        assertFalse(advanced.isSimpleMode)
        assertTrue(advanced.trackInvestments)
        assertTrue(advanced.showDashboardNetWorth)

        val simpleAgain = AppExperienceConfig.applySimplePreset(advanced)
        assertTrue(simpleAgain.isSimpleMode)
        assertFalse(simpleAgain.trackInvestments)
        assertFalse(simpleAgain.showDashboardNetWorth)
    }

    @Test
    fun `onboardingTrackingSelection maps Simple, Daily-only, and Advanced choices cleanly`() {
        val dailyOnly = OnboardingTrackingSelection(
            isSimpleMode = true,
            trackShared = false,
            trackLoans = false,
            trackInvestments = false,
            trackPlanning = false,
        ).toExperienceFlags()
        assertTrue(dailyOnly.isSimpleMode)
        assertFalse(dailyOnly.trackSharedAndPeople)
        assertFalse(dailyOnly.trackLoans)
        assertFalse(dailyOnly.trackInvestments)
        assertFalse(dailyOnly.trackPlanning)
        assertFalse(dailyOnly.showDashboardNetWorth)
        assertTrue(dailyOnly.showDashboardAlerts)

        val advanced = OnboardingTrackingSelection.forMode(simpleMode = false).toExperienceFlags()
        assertFalse(advanced.isSimpleMode)
        assertTrue(advanced.trackSharedAndPeople)
        assertTrue(advanced.trackLoans)
        assertTrue(advanced.trackInvestments)
        assertTrue(advanced.trackPlanning)
        assertTrue(advanced.showDashboardNetWorth)
    }

    @Test
    fun `userPreferencesRepository persists mode presets and individual module toggles roundtrip`() = runTest {
        val state = MutableStateFlow(emptyPreferences())
        val fakeStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = state
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                val updated = transform(state.value)
                state.value = updated
                return updated
            }
        }
        val repo = UserPreferencesRepository(fakeStore)

        // Switch to Advanced preset, then selectively toggle modules off & back on
        repo.applyAdvancedPreset()
        var prefs = repo.preferences.first()
        assertFalse(prefs.isSimpleMode)
        assertTrue(prefs.trackInvestments)
        assertTrue(prefs.showDashboardNetWorth)

        repo.setTrackInvestments(false)
        prefs = repo.preferences.first()
        assertFalse(prefs.trackInvestments)
        assertTrue(prefs.trackLoans) // unrelated flags unaffected

        repo.setTrackInvestments(true)
        prefs = repo.preferences.first()
        assertTrue(prefs.trackInvestments)

        repo.applySimplePreset()
        prefs = repo.preferences.first()
        assertTrue(prefs.isSimpleMode)
        assertTrue(prefs.trackSharedAndPeople)
        assertTrue(prefs.trackPlanning)
        assertFalse(prefs.trackLoans)
        assertFalse(prefs.trackInvestments)
    }

    @Test
    fun `dashboardAlertSelector prioritizes overdue bills, exceeded budgets, due-soon bills, and alert budgets`() {
        val now = 1_764_200_000_000L
        val overdueBill = BillEntity(
            id = 1L,
            name = "Rent",
            type = BillType.RENT,
            expectedAmount = 1_500_000L,
            dueDay = 1,
            frequency = BillFrequency.MONTHLY,
            nextDueDateMs = now - 86_400_000L,
        )
        val dueSoonBill = BillEntity(
            id = 2L,
            name = "Internet",
            type = BillType.UTILITY,
            expectedAmount = 99_900L,
            dueDay = 5,
            frequency = BillFrequency.MONTHLY,
            nextDueDateMs = now + 2 * 86_400_000L,
        )
        val farBill = BillEntity(
            id = 3L,
            name = "Insurance",
            type = BillType.INSURANCE,
            expectedAmount = 500_000L,
            dueDay = 20,
            frequency = BillFrequency.MONTHLY,
            nextDueDateMs = now + 20 * 86_400_000L,
        )
        val budgetEntity = BudgetEntity(
            id = 1L,
            name = "Food",
            amount = 100_000L,
            period = BudgetPeriod.MONTHLY,
            startDate = now - 10 * 86_400_000L,
        )
        val warningBudget = BudgetProgress(
            budget = budgetEntity,
            spentPaise = 85_000L,
            remainingPaise = 15_000L,
            percentUsed = 85,
            isAlertTriggered = true,
            isExceeded = false,
        )
        val exceededBudget = BudgetProgress(
            budget = budgetEntity,
            spentPaise = 120_000L,
            remainingPaise = -20_000L,
            percentUsed = 120,
            isAlertTriggered = true,
            isExceeded = true,
        )

        // 1. Overdue beats everything
        val overdueAlert = DashboardAlertSelector.selectTopAlert(
            bills = listOf(overdueBill, dueSoonBill),
            budgets = listOf(exceededBudget),
            currency = "INR",
            nowMs = now,
        )
        assertEquals(DashboardAlertKind.OVERDUE_BILLS, overdueAlert?.kind)
        assertEquals(true, overdueAlert?.isUrgent)

        // 2. Exceeded budget beats upcoming bills
        val exceededAlert = DashboardAlertSelector.selectTopAlert(
            bills = listOf(dueSoonBill),
            budgets = listOf(exceededBudget),
            currency = "INR",
            nowMs = now,
        )
        assertEquals(DashboardAlertKind.BUDGET_ALERT, exceededAlert?.kind)
        assertEquals(true, exceededAlert?.isUrgent)

        // 3. Due-soon bills beat non-exceeded warning budget
        val dueSoonAlert = DashboardAlertSelector.selectTopAlert(
            bills = listOf(dueSoonBill, dueSoonBill.copy(id = 4L, name = "Water")),
            budgets = listOf(warningBudget),
            currency = "INR",
            nowMs = now,
        )
        assertEquals(DashboardAlertKind.UPCOMING_BILLS, dueSoonAlert?.kind)
        assertEquals("2 bills due this week", dueSoonAlert?.title)

        // 4. Warning budget shown when no urgent bills exist
        val warningAlert = DashboardAlertSelector.selectTopAlert(
            bills = listOf(farBill),
            budgets = listOf(warningBudget),
            currency = "INR",
            nowMs = now,
        )
        assertEquals(DashboardAlertKind.BUDGET_ALERT, warningAlert?.kind)
        assertEquals("Food budget 85% used", warningAlert?.title)

        // 5. Disabled planning/alerts hides alert cleanly
        assertEquals(
            null,
            DashboardAlertSelector.selectTopAlert(
                bills = listOf(overdueBill),
                budgets = listOf(exceededBudget),
                currency = "INR",
                nowMs = now,
                trackPlanning = false,
            ),
        )
        assertEquals(
            null,
            DashboardAlertSelector.selectTopAlert(
                bills = listOf(overdueBill),
                budgets = listOf(exceededBudget),
                currency = "INR",
                nowMs = now,
                showAlerts = false,
            ),
        )
    }

    @Test
    fun `bottomNav, peopleTabs, and moreHub adapt cleanly between Simple and Advanced modes`() {
        val simpleDefault = UserPreferences()
        assertEquals(
            listOf(
                Screen.Dashboard.route,
                Screen.Transactions.route,
                Screen.QuickAdd.createRoute("EXPENSE"),
                Screen.Plan.route,
                Screen.People.route,
            ),
            visibleBottomNavItems(simpleDefault).map { it.route },
        )
        assertEquals(
            listOf(PeopleTab.FRIENDS, PeopleTab.GROUPS, PeopleTab.EVENTS),
            visiblePeopleTabs(simpleDefault.trackSharedAndPeople, simpleDefault.trackLoans, simpleDefault.isSimpleMode),
        )
        val simpleMoreItems = visibleMoreSections(simpleDefault).flatMap { it.second }
        assertTrue("Expected <= 8 More items in Simple Mode, got ${simpleMoreItems.size}", simpleMoreItems.size <= 8)
        assertFalse(simpleMoreItems.any { it.route == Screen.Investments.route })
        assertFalse(simpleMoreItems.any { it.route == Screen.DocumentVault.route })

        // Even with Loans & Investments enabled inside Simple Mode, stays <= 8 items
        val simpleWithLoansAndInv = simpleDefault.copy(trackLoans = true, trackInvestments = true)
        val expandedSimpleMore = visibleMoreSections(simpleWithLoansAndInv).flatMap { it.second }
        assertTrue(expandedSimpleMore.size <= 8)
        assertTrue(expandedSimpleMore.any { it.route == Screen.Investments.route })
        assertTrue(expandedSimpleMore.any { it.route == Screen.Loans.route })

        // Daily-only Simple Mode hides Plan & People bottom tabs cleanly
        val dailyOnly = simpleDefault.copy(trackSharedAndPeople = false, trackPlanning = false)
        assertEquals(
            listOf(Screen.Dashboard.route, Screen.Transactions.route, Screen.QuickAdd.createRoute("EXPENSE")),
            visibleBottomNavItems(dailyOnly).map { it.route },
        )

        // Advanced Mode exposes all tabs & power tools
        val advanced = AppExperienceConfig.applyAdvancedPreset(simpleDefault)
        assertEquals(
            listOf(PeopleTab.FRIENDS, PeopleTab.GROUPS, PeopleTab.EVENTS, PeopleTab.LOANS),
            visiblePeopleTabs(advanced.trackSharedAndPeople, advanced.trackLoans, advanced.isSimpleMode),
        )
        val advancedRoutes = visibleMoreSections(advanced).flatMap { it.second }.map { it.route }.toSet()
        assertTrue(Screen.Investments.route in advancedRoutes)
        assertTrue(Screen.NetWorth.route in advancedRoutes)
        assertTrue(Screen.FinancialCalendar.route in advancedRoutes)
        assertTrue(Screen.DocumentVault.route in advancedRoutes)
        assertTrue(Screen.ImportExport.route in advancedRoutes)
        assertTrue(Screen.AuditHistory.route in advancedRoutes)
    }

    @Test
    fun `setupChecklistState tracks 3 steps, first-run CTA, completion auto-hide, and dismissal`() {
        val beforeOnboarding = SetupChecklistState(onboardingCompleted = false)
        assertFalse(beforeOnboarding.shouldShowFirstRunCta)
        assertFalse(beforeOnboarding.shouldShowChecklist)

        val fresh = SetupChecklistState(onboardingCompleted = true)
        assertTrue(fresh.shouldShowFirstRunCta)
        assertTrue(fresh.shouldShowChecklist)
        assertEquals(0, fresh.completedCount)
        assertFalse(fresh.allCompleted)

        val partial = fresh.copy(hasAnyTransaction = true, hasAnyBudget = true)
        assertFalse(partial.shouldShowFirstRunCta)
        assertTrue(partial.shouldShowChecklist)
        assertEquals(2, partial.completedCount)

        val done = partial.copy(hasBackupOrAutoBackupEnabled = true)
        assertEquals(3, done.completedCount)
        assertTrue(done.allCompleted)
        assertFalse(done.shouldShowChecklist)

        val dismissed = fresh.copy(isDismissed = true)
        assertTrue(dismissed.shouldShowFirstRunCta)
        assertFalse(dismissed.shouldShowChecklist)
    }

    @Test
    fun `settingsViewModel hasExistingFinancialData ignores zero-balance empty ledger and detects txs or balances`() {
        val zeroAcc = AccountWithBalance(
            account = AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalance = 0L),
            currentBalance = 0L,
        )
        val deletedTx = TransactionWithDetails(
            transaction = TransactionEntity(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 10_000L,
                date = 1_000L,
                accountId = 1L,
                isDeleted = true,
            ),
            accountName = "Cash",
        )
        assertFalse(SettingsViewModel.hasExistingFinancialData(emptyList(), listOf(zeroAcc)))
        assertFalse(SettingsViewModel.hasExistingFinancialData(listOf(deletedTx), listOf(zeroAcc)))

        val activeTx = deletedTx.copy(transaction = deletedTx.transaction.copy(isDeleted = false))
        assertTrue(SettingsViewModel.hasExistingFinancialData(listOf(activeTx), listOf(zeroAcc)))

        val nonZeroAcc = zeroAcc.copy(currentBalance = 50_000L)
        assertTrue(SettingsViewModel.hasExistingFinancialData(emptyList(), listOf(nonZeroAcc)))
    }

    @Test
    fun `importValidator isMappingConfident succeeds for Date-Amount headers and rejects ambiguous columns`() {
        val standard = CsvTable(
            headers = listOf("Date", "Amount", "Merchant"),
            rows = listOf(listOf("2026-11-20", "450.00", "Swiggy")),
        )
        val standardMapping = ImportValidator.autoDetectMapping(standard.headers)
        assertEquals(0, standardMapping.dateIndex)
        assertEquals(1, standardMapping.amountIndex)
        assertTrue(ImportValidator.isMappingConfident(standard, standardMapping))

        val genericHeaders = CsvTable(
            headers = listOf("Col1", "Col2", "Col3"),
            rows = listOf(listOf("2026-11-20", "450.00", "Swiggy")),
        )
        val genericMapping = ImportValidator.autoDetectMapping(genericHeaders.headers)
        assertFalse(ImportValidator.isMappingConfident(genericHeaders, genericMapping))

        val unparseableRows = CsvTable(
            headers = listOf("Date", "Amount"),
            rows = listOf(listOf("not-a-date", "abc")),
        )
        assertFalse(ImportValidator.isMappingConfident(unparseableRows, ImportValidator.autoDetectMapping(unparseableRows.headers)))

        val duplicateIndices = standardMapping.copy(amountIndex = 0)
        assertFalse(ImportValidator.isMappingConfident(standard, duplicateIndices))
    }

    @Test
    fun `shouldShowBackupReminder triggers at 20+ txs until backed up, auto-backup enabled, or snoozed`() {
        val now = 1_764_300_000_000L
        assertFalse(
            DataManagementViewModel.shouldShowBackupReminder(
                activeTransactionCount = 19,
                lastBackupAtMs = null,
                autoBackupEnabled = false,
                snoozedUntilMs = 0L,
                nowMs = now,
            ),
        )
        assertTrue(
            DataManagementViewModel.shouldShowBackupReminder(
                activeTransactionCount = 20,
                lastBackupAtMs = null,
                autoBackupEnabled = false,
                snoozedUntilMs = 0L,
                nowMs = now,
            ),
        )
        assertFalse(
            DataManagementViewModel.shouldShowBackupReminder(
                activeTransactionCount = 25,
                lastBackupAtMs = null,
                autoBackupEnabled = false,
                snoozedUntilMs = now + 86_400_000L,
                nowMs = now,
            ),
        )
        assertFalse(
            DataManagementViewModel.shouldShowBackupReminder(
                activeTransactionCount = 25,
                lastBackupAtMs = now - 86_400_000L, // recent manual backup
                autoBackupEnabled = false,
                snoozedUntilMs = 0L,
                nowMs = now,
            ),
        )
        assertFalse(
            DataManagementViewModel.shouldShowBackupReminder(
                activeTransactionCount = 25,
                lastBackupAtMs = now - 10L * 86_400_000L,
                autoBackupEnabled = true,
                snoozedUntilMs = 0L,
                nowMs = now,
            ),
        )
    }

    @Test
    fun `saveDocumentUseCase links and unlinks vault receipts cleanly on transaction entities`() = runTest {
        val docs = mutableMapOf(
            7L to DocumentEntity(
                id = 7L,
                name = "Dinner Bill.jpg",
                type = DocumentType.RECEIPT,
                fileUri = "/vault/dinner.jpg",
                mimeType = "image/jpeg",
            ),
        )
        val docDao = Proxy.newProxyInstance(
            DocumentDao::class.java.classLoader,
            arrayOf(DocumentDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getByIdOnce" -> docs[args[0] as Long]
                "insert" -> {
                    val d = args[0] as DocumentEntity
                    docs[d.id] = d
                    d.id
                }
                else -> Unit
            }
        } as DocumentDao

        val linked = SaveDocumentUseCase.linkExistingToEntity(
            documentDao = docDao,
            entityType = SaveDocumentUseCase.ENTITY_TYPE_TRANSACTION,
            entityId = 42L,
            documentId = 7L,
        )
        assertTrue(linked.isSuccess)
        assertEquals("transaction", docs[7L]?.linkedEntityType)
        assertEquals(42L, docs[7L]?.linkedEntityId)

        val unlinked = SaveDocumentUseCase.unlinkFromEntity(docDao, 7L)
        assertTrue(unlinked.isSuccess)
        assertEquals("", docs[7L]?.linkedEntityType)
        assertEquals(null, docs[7L]?.linkedEntityId)

        val missing = SaveDocumentUseCase.linkExistingToEntity(docDao, "transaction", 42L, 999L)
        assertTrue(missing.isFailure)
    }

    @Test
    fun `analyticsViewModel buildSpendingInsight formats increase, decrease, zero previous month, and no spend`() {
        val food = CategorySpendingItem(
            categoryId = 1L,
            categoryName = "Food",
            colorHex = "#E65100",
            totalPaise = 60_000L,
            percent = 60f,
            transactionCount = 4,
        )

        val increased = AnalyticsViewModel.buildSpendingInsight(
            comparison = PeriodComparison(0L, 0L, currentExpense = 112_000L, previousExpense = 100_000L),
            categoryBreakdown = listOf(food),
            currency = "INR",
        )
        assertEquals("You spent 12% more than last month, mostly on Food.", increased)

        val decreased = AnalyticsViewModel.buildSpendingInsight(
            comparison = PeriodComparison(0L, 0L, currentExpense = 92_000L, previousExpense = 100_000L),
            categoryBreakdown = listOf(food),
            currency = "INR",
        )
        assertEquals("Spending is down 8% vs last month.", decreased)

        val zeroPrev = AnalyticsViewModel.buildSpendingInsight(
            comparison = PeriodComparison(0L, 0L, currentExpense = 50_000L, previousExpense = 0L),
            categoryBreakdown = listOf(food),
            currency = "INR",
        )
        assertEquals("You spent ₹500 this month, mostly on Food.", zeroPrev)

        val noSpend = AnalyticsViewModel.buildSpendingInsight(
            comparison = PeriodComparison(0L, 0L, currentExpense = 0L, previousExpense = 0L),
            categoryBreakdown = emptyList(),
            currency = "INR",
        )
        assertEquals("Log expenses to see monthly spending insights.", noSpend)
    }
}


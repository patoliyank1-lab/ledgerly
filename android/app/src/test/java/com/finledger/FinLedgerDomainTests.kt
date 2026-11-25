package com.finledger

import com.finledger.data.local.dao.*
import com.finledger.data.local.entity.*
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.InsightRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.domain.model.AccountBalanceCalculator
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.QuickTemplateCodec
import com.finledger.domain.model.QuickTransactionTemplate
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.*
import com.finledger.domain.util.AmountExpressionEvaluator
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.CategoryPickerHelper
import com.finledger.ui.components.QuickDateHelper
import com.finledger.ui.components.UndoCoordinator
import com.finledger.ui.screens.friends.formatPersonBalanceWarning
import com.finledger.ui.screens.security.AppLockManager
import com.finledger.ui.screens.transactions.QuickAddTab
import com.finledger.ui.screens.transactions.QuickAddValidationResult
import com.finledger.ui.screens.transactions.QuickAddValidator
import com.finledger.ui.screens.transactions.TransactionsViewModel
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
}


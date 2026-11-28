package com.finledger

import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.TransactionRepository
import com.finledger.domain.model.TransactionFilter
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.DeleteTransactionUseCase
import com.finledger.domain.usecase.GetTransactionsUseCase
import com.finledger.ui.screens.transactions.TransactionsUiState
import com.finledger.ui.screens.transactions.TransactionsViewModel
import com.finledger.ui.screens.transactions.YearMonthKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

class GetTransactionsUseCaseTest {

    private fun sampleItems(): List<TransactionWithDetails> = listOf(
        TransactionWithDetails(
            transaction = TransactionEntity(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 12_000L,
                date = 1_000L,
                accountId = 1L,
                merchantOrPayee = "Coffee",
                isPersonal = true,
            ),
            accountName = "Cash",
        ),
        TransactionWithDetails(
            transaction = TransactionEntity(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 45_000L,
                personalAmount = 22_500L,
                date = 2_000L,
                accountId = 1L,
                merchantOrPayee = "Dinner",
                isPersonal = false,
            ),
            accountName = "Bank",
        ),
        TransactionWithDetails(
            transaction = TransactionEntity(
                id = 3L,
                type = TransactionType.EXPENSE,
                amount = 30_000L,
                date = 3_000L,
                accountId = 1L,
                merchantOrPayee = "Cab",
                isPersonal = true,
                sharedExpenseId = 99L,
            ),
            accountName = "Bank",
        ),
        TransactionWithDetails(
            transaction = TransactionEntity(
                id = 4L,
                type = TransactionType.INCOME,
                amount = 100_000L,
                date = 4_000L,
                accountId = 1L,
                merchantOrPayee = "Salary",
                isPersonal = true,
            ),
            accountName = "Bank",
        ),
        TransactionWithDetails(
            transaction = TransactionEntity(
                id = 5L,
                type = TransactionType.TRANSFER,
                amount = 20_000L,
                date = 5_000L,
                accountId = 1L,
                toAccountId = 2L,
                isPersonal = true,
            ),
            accountName = "Bank",
        ),
    )

    private fun fakeRepo(items: List<TransactionWithDetails>): TransactionRepository =
        Proxy.newProxyInstance(
            TransactionRepository::class.java.classLoader,
            arrayOf(TransactionRepository::class.java),
        ) { _, method, _ ->
            if (method.name == "observeAllTransactionsWithDetails") {
                flowOf(items) as Flow<*>
            } else {
                throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as TransactionRepository

    @Test
    fun `onlyShared returns non-personal or sharedExpenseId linked transactions`() = runTest {
        val useCase = GetTransactionsUseCase(fakeRepo(sampleItems()))

        val all = useCase(TransactionFilter()).first()
        assertEquals(5, all.size)

        val sharedOnly = useCase(TransactionFilter(onlyShared = true)).first()
        assertEquals(listOf(2L, 3L), sharedOnly.map { it.transaction.id })

        val sharedSearch = useCase(TransactionFilter(onlyShared = true, searchQuery = "cab")).first()
        assertEquals(listOf(3L), sharedSearch.map { it.transaction.id })
    }

    @Test
    fun `computeFilteredSummary calculates count, spent, income, transfer, and net totals`() {
        val allSummary = TransactionsViewModel.computeFilteredSummary(sampleItems())
        assertEquals(5, allSummary.count)
        assertEquals(87_000L, allSummary.spentPaise)
        assertEquals(100_000L, allSummary.incomePaise)
        assertEquals(20_000L, allSummary.transferPaise)
        assertEquals(13_000L, allSummary.netTotalPaise)

        val sharedOnly = GetTransactionsUseCase.filterTransactions(
            sampleItems(),
            TransactionFilter(onlyShared = true),
        )
        val sharedSummary = TransactionsViewModel.computeFilteredSummary(sharedOnly)
        assertEquals(2, sharedSummary.count)
        assertEquals(75_000L, sharedSummary.spentPaise)
        assertEquals(-75_000L, sharedSummary.netTotalPaise)
    }

    @Test
    fun `month navigation shifts cleanly across year boundaries and resolves exact bounds`() {
        val dec2025 = YearMonthKey(2025, 11)
        val jan2026 = TransactionsViewModel.shiftMonth(dec2025, 1)
        assertEquals(YearMonthKey(2026, 0), jan2026)

        val feb2026 = TransactionsViewModel.shiftMonth(jan2026, 1)
        val backToJan = TransactionsViewModel.shiftMonth(feb2026, -1)
        assertEquals(jan2026, backToJan)

        val (startMs, endMs) = TransactionsViewModel.monthBoundsFor(jan2026)
        assertEquals(jan2026, TransactionsViewModel.resolveYearMonthFromBounds(startMs, endMs))
        assertEquals(null, TransactionsViewModel.resolveYearMonthFromBounds(startMs + 1_000L, endMs))
        assertEquals(null, TransactionsViewModel.resolveYearMonthFromBounds(null, null))
    }

    @Test
    fun `formatFilteredSummaryText matches category, type, shared, and empty states`() {
        val emptyState = TransactionsUiState(filteredCount = 0)
        assertEquals(null, TransactionsViewModel.formatFilteredSummaryText(emptyState))

        val foodCat = CategoryEntity(
            id = 3L,
            name = "Food",
            iconName = "restaurant",
            colorHex = "#FF0000",
            type = CategoryType.EXPENSE,
        )
        val foodExpenseState = TransactionsUiState(
            filteredCount = 23,
            filteredSpentPaise = 482_000L,
            filteredNetTotalPaise = -482_000L,
            filter = TransactionFilter(categoryId = 3L),
            allCategories = listOf(foodCat),
            primaryCurrency = "INR",
        )
        assertEquals("Food · ₹4,820 · 23 items", TransactionsViewModel.formatFilteredSummaryText(foodExpenseState))

        val sharedState = TransactionsUiState(
            filteredCount = 1,
            filteredSpentPaise = 75_000L,
            filteredNetTotalPaise = -75_000L,
            filter = TransactionFilter(onlyShared = true),
            primaryCurrency = "INR",
        )
        assertEquals("Shared · ₹750 · 1 item", TransactionsViewModel.formatFilteredSummaryText(sharedState))

        val mixedState = TransactionsUiState(
            filteredCount = 4,
            filteredSpentPaise = 30_000L,
            filteredIncomePaise = 50_000L,
            filteredNetTotalPaise = 20_000L,
            primaryCurrency = "INR",
        )
        assertEquals("All · Net +₹200 · 4 items", TransactionsViewModel.formatFilteredSummaryText(mixedState))
    }

    @Test
    fun `bulk delete, restore, and category updates operate safely across selected transactions`() = runTest {
        val store = mutableMapOf(
            1L to TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 500L, date = 1L, accountId = 1L, categoryId = 10L),
            2L to TransactionEntity(id = 2L, type = TransactionType.INCOME, amount = 1000L, date = 2L, accountId = 1L, categoryId = 10L),
            3L to TransactionEntity(id = 3L, type = TransactionType.TRANSFER, amount = 2000L, date = 3L, accountId = 1L, toAccountId = 2L, categoryId = null),
        )
        val updatedEvents = mutableListOf<Long>()

        val repo = Proxy.newProxyInstance(
            TransactionRepository::class.java.classLoader,
            arrayOf(TransactionRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "softDelete" -> {
                    val id = args[0] as Long
                    store[id]?.let { store[id] = it.copy(isDeleted = true) }
                    Unit
                }
                "restore" -> {
                    val id = args[0] as Long
                    store[id]?.let { store[id] = it.copy(isDeleted = false) }
                    Unit
                }
                "getById" -> {
                    val id = args[0] as Long
                    store[id]
                }
                "update" -> {
                    val updated = args[1] as TransactionEntity
                    store[updated.id] = updated
                    updatedEvents += updated.id
                    Unit
                }
                else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as TransactionRepository

        val deleteUseCase = DeleteTransactionUseCase(repo)
        val deleted = TransactionsViewModel.executeBulkDelete(listOf(1L, 2L), deleteUseCase)
        assertEquals(listOf(1L, 2L), deleted)
        assertEquals(true, store[1L]?.isDeleted)
        assertEquals(true, store[2L]?.isDeleted)
        assertEquals(false, store[3L]?.isDeleted)

        val restoredCount = TransactionsViewModel.executeBulkRestore(deleted, deleteUseCase)
        assertEquals(2, restoredCount)
        assertEquals(false, store[1L]?.isDeleted)
        assertEquals(false, store[2L]?.isDeleted)

        val categoryChangedCount = TransactionsViewModel.executeBulkUpdateCategory(
            ids = listOf(1L, 2L, 3L),
            categoryId = 25L,
            transactionRepository = repo,
        )
        assertEquals(2, categoryChangedCount)
        assertEquals(listOf(1L, 2L), updatedEvents)
        assertEquals(25L, store[1L]?.categoryId)
        assertEquals(25L, store[2L]?.categoryId)
        assertEquals(null, store[3L]?.categoryId)
    }
}


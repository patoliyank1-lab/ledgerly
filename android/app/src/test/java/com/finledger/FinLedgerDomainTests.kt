package com.finledger

import com.finledger.data.local.entity.*
import com.finledger.domain.model.AccountBalanceCalculator
import com.finledger.domain.usecase.*
import com.finledger.domain.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}


package com.finledger.domain.usecase

import com.finledger.data.local.entity.ExpensePayerEntity
import com.finledger.data.local.entity.ExpenseSplitEntity
import kotlin.math.roundToLong

data class ItemSplitInput(
    val itemName: String,
    val amountPaise: Long,
    val assignedPersonIds: List<Long>,
)

/**
 * Exact paise-preserving split calculation engine (P2-14).
 * Supports EQUAL, CUSTOM_AMOUNT, PERCENTAGE, SHARES, and ITEM split methods.
 */
object SplitCalculator {

    const val USER_PERSON_ID = 0L

    fun splitEqually(totalPaise: Long, participantIds: List<Long>): Map<Long, Long> {
        if (participantIds.isEmpty()) return emptyMap()
        val count = participantIds.size
        val base = totalPaise / count
        val remainder = (totalPaise % count).toInt()
        return participantIds.mapIndexed { index, id ->
            id to (base + if (index < remainder) 1L else 0L)
        }.toMap()
    }

    fun splitByAmounts(amounts: Map<Long, Long>): Map<Long, Long> = amounts.toMap()

    fun splitByPercentages(totalPaise: Long, percentages: Map<Long, Double>): Map<Long, Long> {
        if (percentages.isEmpty()) return emptyMap()
        val entries = percentages.entries.toList()
        val result = LinkedHashMap<Long, Long>()
        var allocated = 0L
        entries.forEachIndexed { index, (personId, pct) ->
            if (index == entries.lastIndex) {
                result[personId] = (totalPaise - allocated).coerceAtLeast(0L)
            } else {
                val share = ((totalPaise * pct) / 100.0).roundToLong()
                result[personId] = share
                allocated += share
            }
        }
        return result
    }

    fun splitByWeights(totalPaise: Long, weights: Map<Long, Double>): Map<Long, Long> {
        val totalWeight = weights.values.sum()
        if (totalWeight <= 0.0 || weights.isEmpty()) return emptyMap()
        val entries = weights.entries.toList()
        val result = LinkedHashMap<Long, Long>()
        var allocated = 0L
        entries.forEachIndexed { index, (personId, weight) ->
            if (index == entries.lastIndex) {
                result[personId] = (totalPaise - allocated).coerceAtLeast(0L)
            } else {
                val share = ((totalPaise * weight) / totalWeight).roundToLong()
                result[personId] = share
                allocated += share
            }
        }
        return result
    }

    fun splitByItems(items: List<ItemSplitInput>): Map<Long, Long> {
        val totals = LinkedHashMap<Long, Long>()
        for (item in items) {
            if (item.assignedPersonIds.isEmpty()) continue
            val itemSplits = splitEqually(item.amountPaise, item.assignedPersonIds)
            for ((personId, share) in itemSplits) {
                totals[personId] = (totals[personId] ?: 0L) + share
            }
        }
        return totals
    }
}

/**
 * Validates that payer amounts and split shares both equal totalAmount (P2-19).
 */
object SplitValidator {
    fun validate(
        totalAmountPaise: Long,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
    ): Result<Unit> {
        if (totalAmountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Total amount must be positive"))
        }
        val paidSum = payers.sumOf { it.amountPaid }
        if (paidSum != totalAmountPaise) {
            return Result.failure(
                IllegalArgumentException("Total paid ($paidSum) must equal expense total ($totalAmountPaise)")
            )
        }
        val activeSplits = splits.filter { !it.isExcluded }
        if (activeSplits.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one participant must be included in the split"))
        }
        val splitSum = activeSplits.sumOf { it.shareAmount }
        if (splitSum != totalAmountPaise) {
            return Result.failure(
                IllegalArgumentException("Split total ($splitSum) must equal expense total ($totalAmountPaise)")
            )
        }
        return Result.success(Unit)
    }
}

/**
 * Enforces FINANCIAL_RULES.md §1 (P2-20):
 * User's personal expense equals the user's split share ONLY, never the full paid amount.
 */
object PersonalExpenseValidator {
    fun resolveUserPersonalShare(splits: List<ExpenseSplitEntity>): Long {
        val userSplit = splits.firstOrNull { it.personId == SplitCalculator.USER_PERSON_ID && !it.isExcluded }
        return userSplit?.shareAmount ?: 0L
    }

    fun isValidPersonalImpact(userPersonalExpense: Long, splits: List<ExpenseSplitEntity>): Boolean {
        return userPersonalExpense == resolveUserPersonalShare(splits)
    }
}


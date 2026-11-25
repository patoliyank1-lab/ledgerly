package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.InsightRepository
import com.finledger.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.roundToLong

data class InvestmentSummary(
    val investment: InvestmentEntity,
    val investedCostPaise: Long,
    val currentValuePaise: Long,
    val unrealizedPnLPaise: Long,
    val unrealizedPnLPercent: Double,
)

data class PortfolioOverview(
    val items: List<InvestmentSummary>,
    val totalInvestedPaise: Long,
    val totalCurrentValuePaise: Long,
    val totalUnrealizedPnLPaise: Long,
    val totalRealizedPnLPaise: Long,
    val allocationByType: Map<InvestmentType, Long>,
)

/**
 * Adds investments and buy/sell/dividend transactions while preserving FINANCIAL_RULES.md §8:
 * Buying an investment moves value from cash into an investment asset (not an expense);
 * fees & taxes are recorded separately; realized vs unrealized gains are tracked separately.
 */
class AddInvestmentUseCase @Inject constructor(
    private val insightRepository: InsightRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun createAndBuy(
        name: String,
        type: InvestmentType,
        platform: String,
        symbol: String,
        units: Double,
        pricePerUnitPaise: Long,
        feesPaise: Long = 0L,
        taxesPaise: Long = 0L,
        dateMs: Long,
        accountId: Long?,
        notes: String = "",
    ): Result<Long> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Asset name is required"))
        if (units <= 0.0 || pricePerUnitPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Units and price must be positive"))
        }
        return runCatching {
            val totalAmount = (units * pricePerUnitPaise).roundToLong()
            val invId = insightRepository.saveInvestment(
                InvestmentEntity(
                    name = name.trim(),
                    type = type,
                    platform = platform.trim(),
                    symbol = symbol.trim(),
                    accountId = accountId,
                    currentUnits = units,
                    averageBuyPrice = pricePerUnitPaise,
                    currentPrice = pricePerUnitPaise,
                    lastPriceUpdateDate = dateMs,
                    notes = notes.trim(),
                )
            )
            insightRepository.insertInvestmentTransaction(
                InvestmentTransactionEntity(
                    investmentId = invId,
                    type = InvestmentTransactionType.BUY,
                    date = dateMs,
                    units = units,
                    pricePerUnit = pricePerUnitPaise,
                    totalAmount = totalAmount,
                    fees = feesPaise,
                    taxes = taxesPaise,
                    accountId = accountId,
                    notes = notes.trim(),
                )
            )
            if (accountId != null) {
                transactionRepository.insert(
                    TransactionEntity(
                        type = TransactionType.INVESTMENT_BUY,
                        amount = totalAmount,
                        personalAmount = 0L,
                        amountInPrimary = totalAmount,
                        date = dateMs,
                        accountId = accountId,
                        merchantOrPayee = name.trim(),
                        description = "Investment buy: ${name.trim()}",
                    )
                )
                val charges = feesPaise + taxesPaise
                if (charges > 0L) {
                    transactionRepository.insert(
                        TransactionEntity(
                            type = TransactionType.EXPENSE,
                            amount = charges,
                            personalAmount = charges,
                            amountInPrimary = charges,
                            date = dateMs,
                            accountId = accountId,
                            merchantOrPayee = platform.ifBlank { name.trim() },
                            description = "Investment brokerage & taxes: ${name.trim()}",
                        )
                    )
                }
            }
            invId
        }
    }

    suspend fun recordTrade(
        investmentId: Long,
        tradeType: InvestmentTransactionType,
        units: Double,
        pricePerUnitPaise: Long,
        feesPaise: Long = 0L,
        taxesPaise: Long = 0L,
        dateMs: Long,
        accountId: Long?,
        notes: String = "",
    ): Result<Unit> {
        val inv = insightRepository.getInvestmentById(investmentId)
            ?: return Result.failure(IllegalStateException("Investment not found"))
        if (units <= 0.0 || pricePerUnitPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Units and price must be positive"))
        }
        if (tradeType == InvestmentTransactionType.SELL && units > inv.currentUnits + 0.0001) {
            return Result.failure(IllegalArgumentException("Cannot sell more units than held (${inv.currentUnits})"))
        }

        return runCatching {
            val totalAmount = (units * pricePerUnitPaise).roundToLong()
            insightRepository.insertInvestmentTransaction(
                InvestmentTransactionEntity(
                    investmentId = investmentId,
                    type = tradeType,
                    date = dateMs,
                    units = units,
                    pricePerUnit = pricePerUnitPaise,
                    totalAmount = totalAmount,
                    fees = feesPaise,
                    taxes = taxesPaise,
                    accountId = accountId,
                    notes = notes.trim(),
                )
            )

            val updatedInv = when (tradeType) {
                InvestmentTransactionType.BUY, InvestmentTransactionType.SIP -> {
                    val newUnits = inv.currentUnits + units
                    val oldCost = inv.currentUnits * inv.averageBuyPrice
                    val newCost = units * pricePerUnitPaise
                    val newAvg = ((oldCost + newCost) / newUnits).roundToLong()
                    inv.copy(
                        currentUnits = newUnits,
                        averageBuyPrice = newAvg,
                        currentPrice = pricePerUnitPaise,
                        lastPriceUpdateDate = dateMs,
                        status = InvestmentStatus.ACTIVE,
                    )
                }
                InvestmentTransactionType.SELL -> {
                    val remaining = (inv.currentUnits - units).coerceAtLeast(0.0)
                    val realizedDelta = ((pricePerUnitPaise - inv.averageBuyPrice) * units).roundToLong() - feesPaise - taxesPaise
                    inv.copy(
                        currentUnits = remaining,
                        realizedGainPaise = inv.realizedGainPaise + realizedDelta,
                        currentPrice = pricePerUnitPaise,
                        lastPriceUpdateDate = dateMs,
                        status = if (remaining <= 0.0001) InvestmentStatus.SOLD else InvestmentStatus.ACTIVE,
                    )
                }
                InvestmentTransactionType.DIVIDEND -> {
                    inv.copy(realizedGainPaise = inv.realizedGainPaise + totalAmount)
                }
            }
            insightRepository.saveInvestment(updatedInv)

            if (accountId != null) {
                when (tradeType) {
                    InvestmentTransactionType.BUY, InvestmentTransactionType.SIP -> {
                        transactionRepository.insert(
                            TransactionEntity(
                                type = TransactionType.INVESTMENT_BUY,
                                amount = totalAmount,
                                personalAmount = 0L,
                                date = dateMs,
                                accountId = accountId,
                                merchantOrPayee = inv.name,
                                description = "Investment ${tradeType.name}: ${inv.name}",
                            )
                        )
                    }
                    InvestmentTransactionType.SELL -> {
                        transactionRepository.insert(
                            TransactionEntity(
                                type = TransactionType.INVESTMENT_SELL,
                                amount = totalAmount,
                                personalAmount = 0L,
                                date = dateMs,
                                accountId = accountId,
                                merchantOrPayee = inv.name,
                                description = "Investment sell: ${inv.name}",
                            )
                        )
                    }
                    InvestmentTransactionType.DIVIDEND -> {
                        transactionRepository.insert(
                            TransactionEntity(
                                type = TransactionType.INCOME,
                                amount = totalAmount,
                                personalAmount = totalAmount,
                                date = dateMs,
                                accountId = accountId,
                                merchantOrPayee = inv.name,
                                description = "Dividend / Interest: ${inv.name}",
                            )
                        )
                    }
                }
            }
            Unit
        }
    }

    suspend fun updateManualValuation(investmentId: Long, newPricePerUnitPaise: Long): Result<Unit> {
        val inv = insightRepository.getInvestmentById(investmentId)
            ?: return Result.failure(IllegalStateException("Investment not found"))
        if (newPricePerUnitPaise <= 0L) return Result.failure(IllegalArgumentException("Valuation must be positive"))
        return runCatching {
            insightRepository.saveInvestment(
                inv.copy(currentPrice = newPricePerUnitPaise, lastPriceUpdateDate = System.currentTimeMillis())
            )
            Unit
        }
    }
}

class GetPortfolioValueUseCase @Inject constructor(
    private val insightRepository: InsightRepository,
) {
    operator fun invoke(): Flow<PortfolioOverview> {
        return insightRepository.observeInvestments().map { list ->
            val items = list.map { inv ->
                val cost = (inv.currentUnits * inv.averageBuyPrice).roundToLong()
                val current = (inv.currentUnits * inv.currentPrice).roundToLong()
                val pnl = current - cost
                val pct = if (cost > 0L) (pnl * 100.0) / cost else 0.0
                InvestmentSummary(
                    investment = inv,
                    investedCostPaise = cost,
                    currentValuePaise = current,
                    unrealizedPnLPaise = pnl,
                    unrealizedPnLPercent = pct,
                )
            }
            val allocation = items.groupBy { it.investment.type }
                .mapValues { (_, group) -> group.sumOf { it.currentValuePaise } }
            PortfolioOverview(
                items = items,
                totalInvestedPaise = items.sumOf { it.investedCostPaise },
                totalCurrentValuePaise = items.sumOf { it.currentValuePaise },
                totalUnrealizedPnLPaise = items.sumOf { it.unrealizedPnLPaise },
                totalRealizedPnLPaise = list.sumOf { it.realizedGainPaise },
                allocationByType = allocation,
            )
        }
    }
}


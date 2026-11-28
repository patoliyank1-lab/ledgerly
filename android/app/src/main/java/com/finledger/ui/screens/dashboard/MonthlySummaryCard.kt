package com.finledger.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finledger.domain.model.MonthlySummary
import com.finledger.domain.usecase.NetWorthBreakdown
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

/**
 * Full-bleed dark tile showing monthly income, personal spending, and savings rate
 * (DESIGN.md alternating white ↔ near-black tile rhythm), plus optional liquid/debt note.
 */
@Composable
fun MonthlySummaryCard(
    summary: MonthlySummary,
    currency: String,
    netWorth: NetWorthBreakdown? = null,
    showHealthNote: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = FinLedgerTheme.colors.tileDark1,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        ) {
            Text(
                text = DateFormatter.formatMonthYear(
                    if (summary.monthStartMs > 0L) summary.monthStartMs else System.currentTimeMillis()
                ),
                style = MaterialTheme.typography.titleSmall,
                color = FinLedgerTheme.colors.bodyMuted,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Net Savings: ${MoneyFormatter.format(summary.savings, currency, showPlus = true)}",
                style = MaterialTheme.typography.headlineMedium,
                color = if (summary.savings >= 0) FinLedgerTheme.colors.skyLinkBlue else FinLedgerTheme.colors.negative,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Monthly Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = FinLedgerTheme.colors.bodyMuted,
                    )
                    Text(
                        text = MoneyFormatter.format(summary.totalIncome, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
                Column {
                    Text(
                        text = "Personal Spending",
                        style = MaterialTheme.typography.labelMedium,
                        color = FinLedgerTheme.colors.bodyMuted,
                    )
                    Text(
                        text = MoneyFormatter.format(summary.totalPersonalExpense, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
                Column {
                    Text(
                        text = "Savings Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = FinLedgerTheme.colors.bodyMuted,
                    )
                    Text(
                        text = "${summary.savingsRatePercent}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FinLedgerTheme.colors.skyLinkBlue,
                    )
                }
            }
            if (showHealthNote && netWorth != null) {
                val debtRatio = if (netWorth.totalAssets > 0) {
                    ((netWorth.totalLiabilities * 100) / netWorth.totalAssets).toInt()
                } else {
                    0
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Liquid cash: ${MoneyFormatter.format(netWorth.cashAndBankAssets, currency)} · Debt-to-Asset: $debtRatio%",
                    style = MaterialTheme.typography.labelMedium,
                    color = FinLedgerTheme.colors.bodyMuted,
                )
            }
        }
    }
}


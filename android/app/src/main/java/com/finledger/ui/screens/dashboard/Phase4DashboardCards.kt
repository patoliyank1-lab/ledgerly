package com.finledger.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finledger.domain.model.MonthlySummary
import com.finledger.domain.usecase.NetWorthBreakdown
import com.finledger.domain.usecase.PortfolioOverview
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun NetWorthCard(
    netWorth: NetWorthBreakdown,
    currency: String,
    onOpenNetWorth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onOpenNetWorth),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Net Worth", style = MaterialTheme.typography.bodyLarge)
                Text("Details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = MoneyFormatter.format(netWorth.netWorth, currency),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Assets ${MoneyFormatter.format(netWorth.totalAssets, currency)} · Liabilities ${MoneyFormatter.format(netWorth.totalLiabilities, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = FinLedgerTheme.colors.inkMuted48,
            )
        }
    }
}

@Composable
fun InvestmentValueCard(
    portfolio: PortfolioOverview,
    currency: String,
    onOpenInvestments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (portfolio.items.isEmpty()) return
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onOpenInvestments),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Investments", style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.inkMuted48)
                Text(MoneyFormatter.format(portfolio.totalCurrentValuePaise, currency), style = MaterialTheme.typography.bodyLarge)
            }
            Column {
                Text("Unrealized P&L", style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.inkMuted48)
                Text(
                    text = MoneyFormatter.format(portfolio.totalUnrealizedPnLPaise, currency, showPlus = true),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (portfolio.totalUnrealizedPnLPaise >= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
                )
            }
        }
    }
}

@Composable
fun FinancialHealthCard(
    monthly: MonthlySummary,
    netWorth: NetWorthBreakdown,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val debtRatio = if (netWorth.totalAssets > 0) {
        ((netWorth.totalLiabilities * 100) / netWorth.totalAssets).toInt()
    } else 0

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text("Financial Health", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Savings Rate: ${monthly.savingsRatePercent}% · Debt-to-Asset Ratio: $debtRatio%",
                style = MaterialTheme.typography.bodySmall,
                color = FinLedgerTheme.colors.inkMuted80,
            )
            Text(
                text = "Liquid cash: ${MoneyFormatter.format(netWorth.cashAndBankAssets, currency)} (excludes receivables)",
                style = MaterialTheme.typography.labelMedium,
                color = FinLedgerTheme.colors.inkMuted48,
            )
        }
    }
}


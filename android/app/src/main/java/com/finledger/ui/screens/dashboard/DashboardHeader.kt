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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finledger.domain.model.AccountSummary
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

/**
 * Full-bleed white hero tile separating spendable cash/bank balances from credit card
 * liabilities and non-spendable investment assets (Product.md §4).
 */
@Composable
fun DashboardHeader(
    userName: String,
    summary: AccountSummary,
    currency: String,
    onOpenAccounts: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAccounts),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Welcome back, $userName",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
                Text(
                    text = "Accounts →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Available Money",
                style = MaterialTheme.typography.titleSmall,
                color = FinLedgerTheme.colors.inkMuted80,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = MoneyFormatter.format(summary.totalAvailableCashAndBank, currency),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                BalanceMiniCard(
                    label = "Cash",
                    value = MoneyFormatter.format(summary.cashBalance, currency),
                    modifier = Modifier.weight(1f),
                )
                BalanceMiniCard(
                    label = "Bank",
                    value = MoneyFormatter.format(summary.bankBalance, currency),
                    modifier = Modifier.weight(1f),
                )
                BalanceMiniCard(
                    label = "Wallets",
                    value = MoneyFormatter.format(summary.walletBalance, currency),
                    modifier = Modifier.weight(1f),
                )
            }

            if (summary.creditCardOutstanding > 0L || summary.investmentAccountBalance > 0L) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (summary.creditCardOutstanding > 0L) {
                        BalanceMiniCard(
                            label = "Card Due",
                            value = MoneyFormatter.format(summary.creditCardOutstanding, currency),
                            isNegative = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (summary.investmentAccountBalance > 0L) {
                        BalanceMiniCard(
                            label = "Investments",
                            value = MoneyFormatter.format(summary.investmentAccountBalance, currency),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceMiniCard(
    label: String,
    value: String,
    isNegative: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = FinLedgerTheme.colors.parchment),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = FinLedgerTheme.colors.inkMuted48,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isNegative) FinLedgerTheme.colors.negative else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}


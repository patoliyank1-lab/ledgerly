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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finledger.data.local.entity.BillEntity
import com.finledger.domain.usecase.BudgetProgress
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun TopAlertCard(
    alert: DashboardTopAlert?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (alert == null) return
    val accent = if (alert.isUrgent) FinLedgerTheme.colors.negative else FinLedgerTheme.colors.warning
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (alert.isUrgent) accent else MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = accent,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (alert.isUrgent) accent else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = alert.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = FinLedgerTheme.colors.inkMuted48,
            )
        }
    }
}

@Composable
fun UpcomingBillsCard(
    bills: List<BillEntity>,
    currency: String,
    onOpenBills: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bills.isEmpty()) return
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onOpenBills),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Upcoming Bills", style = MaterialTheme.typography.bodyLarge)
                Text("View all", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            bills.take(3).forEach { bill ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${bill.name} · ${DateFormatter.formatDate(bill.nextDueDateMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.inkMuted80,
                    )
                    Text(
                        text = MoneyFormatter.format(bill.expectedAmount, currency),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetProgressCard(
    budgets: List<BudgetProgress>,
    currency: String,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (budgets.isEmpty()) return
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onOpenBudgets),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Budget Progress", style = MaterialTheme.typography.bodyLarge)
                Text("View all", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            budgets.take(3).forEach { bp ->
                val ratio = (bp.percentUsed / 100f).coerceIn(0f, 1f)
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(bp.budget.name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${MoneyFormatter.format(bp.spentPaise, currency)} / ${MoneyFormatter.format(bp.budget.amount, currency)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = FinLedgerTheme.colors.inkMuted48,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (bp.isExceeded) FinLedgerTheme.colors.negative else MaterialTheme.colorScheme.primary,
                        trackColor = FinLedgerTheme.colors.dividerSoft,
                    )
                }
            }
        }
    }
}

@Composable
fun LendingBorrowingCard(
    totalToReceive: Long,
    totalToPay: Long,
    currency: String,
    onOpenBalances: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalToReceive == 0L && totalToPay == 0L) return
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onOpenBalances),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("People Owe You", style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.inkMuted48)
                Text(
                    text = MoneyFormatter.format(totalToReceive, currency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinLedgerTheme.colors.positive,
                )
            }
            Column {
                Text("You Owe Others", style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.inkMuted48)
                Text(
                    text = MoneyFormatter.format(totalToPay, currency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinLedgerTheme.colors.negative,
                )
            }
        }
    }
}


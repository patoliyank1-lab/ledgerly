package com.finledger.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.ui.screens.transactions.TransactionRowCard
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun RecentTransactionsList(
    transactions: List<TransactionWithDetails>,
    onSeeAll: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (transactions.isEmpty()) {
            Text(
                text = "No activity recorded yet. Tap + to add your first expense or income.",
                style = MaterialTheme.typography.bodySmall,
                color = FinLedgerTheme.colors.inkMuted48,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        } else {
            transactions.forEach { item ->
                TransactionRowCard(
                    item = item,
                    onClick = { onOpenTransaction(item.transaction.id) },
                )
            }
        }
    }
}


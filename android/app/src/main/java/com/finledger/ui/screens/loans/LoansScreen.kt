package com.finledger.ui.screens.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.LoanType
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun LoansScreen(
    onBack: () -> Unit,
    onAddLoan: () -> Unit,
    onOpenLoanDetail: (Long) -> Unit,
    viewModel: LoansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }
    val filtered = state.loans.filter {
        if (tabIndex == 0) it.loan.type == LoanType.LENT else it.loan.type == LoanType.BORROWED
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Loans", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLoan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add loan")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Surface(
                color = FinLedgerTheme.colors.tileDark1,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Lent (Receivable)", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                        Text(
                            text = MoneyFormatter.format(state.totalLentOutstanding, state.primaryCurrency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = FinLedgerTheme.colors.positive,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Borrowed (Payable)", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                        Text(
                            text = MoneyFormatter.format(state.totalBorrowedOutstanding, state.primaryCurrency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = FinLedgerTheme.colors.negative,
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Lent") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Borrowed") })
            }

            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Money,
                    title = if (tabIndex == 0) "No lending recorded" else "No borrowing recorded",
                    subtitle = "Track principal, interest, repayment schedules, and partial repayments.",
                    actionLabel = "Record Loan",
                    onAction = onAddLoan,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(filtered, key = { it.loan.id }) { item ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLoanDetail(item.loan.id) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.loan.personOrOrg, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${DateFormatter.formatDate(item.loan.dateGiven)} · ${item.loan.status.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FinLedgerTheme.colors.inkMuted48,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MoneyFormatter.format(item.outstandingPrincipalPaise, item.loan.currency),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = "of ${MoneyFormatter.format(item.loan.principalAmount, item.loan.currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FinLedgerTheme.colors.inkMuted48,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


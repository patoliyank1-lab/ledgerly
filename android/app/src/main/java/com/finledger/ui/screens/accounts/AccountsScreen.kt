package com.finledger.ui.screens.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AccountType
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun AccountsScreen(
    onOpenAccountDetail: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val spendable = state.accounts.filter { !it.account.type.isLiability && !it.account.type.isInvestmentOrAsset }
    val creditCards = state.accounts.filter { it.account.type == AccountType.CREDIT_CARD }
    val assets = state.accounts.filter { it.account.type.isInvestmentOrAsset }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Wallets", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add account")
            }
        },
    ) { padding ->
        if (state.accounts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.AccountBalanceWallet,
                title = "No accounts yet",
                subtitle = "Add your cash wallet, bank accounts, digital wallets, and cards.",
                actionLabel = "Add Account",
                onAction = { showAddSheet = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                // Dark summary hero tile per DESIGN.md alternating light/dark rhythm
                item {
                    Surface(
                        color = FinLedgerTheme.colors.tileDark1,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = "Available Money",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.bodyMuted,
                            )
                            Spacer(modifier = Modifier.height(Spacing.xxs))
                            Text(
                                text = MoneyFormatter.format(state.summary.totalAvailableCashAndBank, state.primaryCurrency),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                SummaryMiniStat("Cash", state.summary.cashBalance, state.primaryCurrency)
                                SummaryMiniStat("Bank", state.summary.bankBalance, state.primaryCurrency)
                                SummaryMiniStat("Card Due", state.summary.creditCardOutstanding, state.primaryCurrency)
                            }
                        }
                    }
                }

                if (spendable.isNotEmpty()) {
                    item { SectionLabel("Cash, Banks & Wallets") }
                    items(spendable, key = { it.account.id }) { item ->
                        AccountRowCard(
                            item = item,
                            onClick = { onOpenAccountDetail(item.account.id) },
                        )
                    }
                }

                if (creditCards.isNotEmpty()) {
                    item { SectionLabel("Credit Cards (Liabilities)") }
                    items(creditCards, key = { it.account.id }) { item ->
                        AccountRowCard(
                            item = item,
                            onClick = { onOpenAccountDetail(item.account.id) },
                        )
                    }
                }

                if (assets.isNotEmpty()) {
                    item { SectionLabel("Fixed Deposits & Investments") }
                    items(assets, key = { it.account.id }) { item ->
                        AccountRowCard(
                            item = item,
                            onClick = { onOpenAccountDetail(item.account.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditAccountSheet(
            existing = null,
            defaultCurrency = state.primaryCurrency,
            onDismiss = { showAddSheet = false },
            onSave = { name, type, bank, masked, currency, opening, limit, billingDay, dueDay, notes ->
                viewModel.saveAccount(
                    existing = null,
                    name = name,
                    type = type,
                    bankOrProvider = bank,
                    maskedIdentifier = masked,
                    currency = currency,
                    openingBalancePaise = opening,
                    creditLimitPaise = limit,
                    billingCycleDay = billingDay,
                    dueDay = dueDay,
                    notes = notes,
                    onSaved = { showAddSheet = false },
                )
            },
        )
    }
}

@Composable
private fun SummaryMiniStat(label: String, amount: Long, currency: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = FinLedgerTheme.colors.bodyMuted,
        )
        Text(
            text = MoneyFormatter.format(amount, currency),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = FinLedgerTheme.colors.inkMuted48,
        modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xs),
    )
}

@Composable
private fun AccountRowCard(
    item: AccountWithBalance,
    onClick: () -> Unit,
) {
    val account = item.account
    val isCard = account.type == AccountType.CREDIT_CARD
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val meta = buildList {
                        add(account.type.displayName)
                        if (account.bankOrProvider.isNotBlank()) add(account.bankOrProvider)
                        if (account.maskedIdentifier.isNotBlank()) add("•••• ${account.maskedIdentifier}")
                    }.joinToString(" · ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyFormatter.format(item.currentBalance, account.currency),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCard && item.currentBalance > 0) FinLedgerTheme.colors.negative else MaterialTheme.colorScheme.onSurface,
                    )
                    if (isCard) {
                        Text(
                            text = "Outstanding",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinLedgerTheme.colors.inkMuted48,
                        )
                    }
                }
            }
            if (isCard && account.creditLimit != null && account.creditLimit > 0L) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                val progress = (item.currentBalance.toFloat() / account.creditLimit.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (progress > 0.8f) FinLedgerTheme.colors.negative else MaterialTheme.colorScheme.primary,
                    trackColor = FinLedgerTheme.colors.dividerSoft,
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = "Available credit: ${MoneyFormatter.format(item.availableCredit ?: 0L, account.currency)} of ${MoneyFormatter.format(account.creditLimit, account.currency)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
        }
    }
}


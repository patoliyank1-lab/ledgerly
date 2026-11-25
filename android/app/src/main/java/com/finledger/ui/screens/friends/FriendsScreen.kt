package com.finledger.ui.screens.friends

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.finledger.domain.usecase.PersonBalanceSummary
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun FriendsScreen(
    onOpenPersonDetail: (Long) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Friends & Contacts", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add contact")
            }
        },
    ) { padding ->
        if (state.balances.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.People,
                title = "No friends added yet",
                subtitle = "Add friends to split expenses, track lending, and view consolidated balances.",
                actionLabel = "Add Contact",
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
                item {
                    BalancesSummaryTile(
                        toReceive = state.totalToReceive,
                        toPay = state.totalToPay,
                        currency = state.primaryCurrency,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
                items(state.balances, key = { it.person.id }) { summary ->
                    PersonBalanceRow(
                        summary = summary,
                        currency = state.primaryCurrency,
                        onClick = { onOpenPersonDetail(summary.person.id) },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditPersonSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, phone, email, rel, notes ->
                viewModel.savePerson(
                    name = name,
                    phone = phone,
                    email = email,
                    relationship = rel,
                    notes = notes,
                    onSaved = { showAddSheet = false },
                )
            },
        )
    }
}

@Composable
fun BalancesSummaryTile(
    toReceive: Long,
    toPay: Long,
    currency: String,
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
                Text("People owe you", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                Text(
                    text = MoneyFormatter.format(toReceive, currency),
                    style = MaterialTheme.typography.headlineMedium,
                    color = FinLedgerTheme.colors.positive,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("You owe people", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                Text(
                    text = MoneyFormatter.format(toPay, currency),
                    style = MaterialTheme.typography.headlineMedium,
                    color = FinLedgerTheme.colors.negative,
                )
            }
        }
    }
}

@Composable
fun PersonBalanceRow(
    summary: PersonBalanceSummary,
    currency: String,
    onClick: () -> Unit,
) {
    val net = summary.netBalancePaise
    val directionLabel = when {
        net > 0 -> "owes you"
        net < 0 -> "you owe"
        else -> "settled"
    }
    val color = when {
        net > 0 -> FinLedgerTheme.colors.positive
        net < 0 -> FinLedgerTheme.colors.negative
        else -> FinLedgerTheme.colors.inkMuted48
    }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.person.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = summary.person.relationship,
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (net == 0L) "Settled" else MoneyFormatter.format(kotlin.math.abs(net), currency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                )
                if (net != 0L) {
                    Text(
                        text = directionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                    )
                }
            }
        }
    }
}


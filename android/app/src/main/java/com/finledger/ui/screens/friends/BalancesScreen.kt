package com.finledger.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun BalancesScreen(
    onBack: () -> Unit,
    onOpenPersonDetail: (Long) -> Unit,
    onRecordSettlement: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Person-wise Balances", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.balances.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.AccountBalance,
                title = "No balances yet",
                subtitle = "Consolidated balances across all groups, events, and settlements appear here.",
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
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Button(
                            onClick = onRecordSettlement,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Record Settlement")
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "All People",
                            style = MaterialTheme.typography.titleSmall,
                            color = FinLedgerTheme.colors.inkMuted48,
                        )
                    }
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
}


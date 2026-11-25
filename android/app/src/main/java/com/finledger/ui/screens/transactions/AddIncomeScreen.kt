package com.finledger.ui.screens.transactions

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Legacy wrapper forwarding to [QuickAddScreen] on [QuickAddTab.INCOME].
 */
@Composable
fun AddIncomeScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    QuickAddScreen(
        initialTab = QuickAddTab.INCOME,
        onBack = onBack,
        viewModel = viewModel,
    )
}

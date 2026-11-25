package com.finledger.ui.screens.transactions

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Legacy wrapper forwarding to [QuickAddScreen] on [QuickAddTab.EXPENSE].
 */
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    QuickAddScreen(
        initialTab = QuickAddTab.EXPENSE,
        onBack = onBack,
        viewModel = viewModel,
    )
}

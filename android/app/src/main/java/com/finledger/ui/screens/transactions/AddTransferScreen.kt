package com.finledger.ui.screens.transactions

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Legacy wrapper forwarding to [QuickAddScreen] on [QuickAddTab.TRANSFER].
 */
@Composable
fun AddTransferScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    QuickAddScreen(
        initialTab = QuickAddTab.TRANSFER,
        onBack = onBack,
        viewModel = viewModel,
    )
}

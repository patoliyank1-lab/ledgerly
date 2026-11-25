package com.finledger.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun QuickActionFab(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Quick actions")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                text = { Text("Add Expense") },
                onClick = { expanded = false; onAddExpense() },
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                text = { Text("Add Income") },
                onClick = { expanded = false; onAddIncome() },
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                text = { Text("Transfer") },
                onClick = { expanded = false; onAddTransfer() },
            )
        }
    }
}


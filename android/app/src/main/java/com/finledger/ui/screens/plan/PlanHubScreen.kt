package com.finledger.ui.screens.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.finledger.ui.screens.bills.BillsContent
import com.finledger.ui.screens.budgets.BudgetsContent
import com.finledger.ui.screens.budgets.SavingsGoalsContent

enum class PlanTab(val label: String) {
    BUDGETS("Budgets"),
    BILLS("Bills"),
    GOALS("Goals"),
}

@Composable
fun PlanHubScreen(
    initialTab: Int = 0,
    onAddBudget: () -> Unit,
    onEditBudget: (Long) -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (Long) -> Unit,
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var selectedTab by rememberSaveable(initialTab) {
        mutableIntStateOf(initialTab.coerceIn(0, PlanTab.entries.lastIndex))
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val activeTab = PlanTab.entries[selectedTab]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Plan", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeTab) {
                        PlanTab.BUDGETS -> onAddBudget()
                        PlanTab.BILLS -> onAddBill()
                        PlanTab.GOALS -> onAddGoal()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = when (activeTab) {
                        PlanTab.BUDGETS -> "Add budget"
                        PlanTab.BILLS -> "Add bill"
                        PlanTab.GOALS -> "Add goal"
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                PlanTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label, maxLines = 1) },
                    )
                }
            }

            when (activeTab) {
                PlanTab.BUDGETS -> BudgetsContent(
                    onAddBudget = onAddBudget,
                    onEditBudget = onEditBudget,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                PlanTab.BILLS -> BillsContent(
                    onAddBill = onAddBill,
                    onEditBill = onEditBill,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                PlanTab.GOALS -> SavingsGoalsContent(
                    onAddGoal = onAddGoal,
                    onEditGoal = onEditGoal,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}


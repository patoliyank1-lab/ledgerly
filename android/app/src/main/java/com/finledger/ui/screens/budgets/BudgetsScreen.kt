package com.finledger.ui.screens.budgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.BudgetPeriod
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    onAddBudget: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Budgets", style = MaterialTheme.typography.headlineMedium) },
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
                onClick = onAddBudget,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add budget")
            }
        },
    ) { padding ->
        if (state.budgets.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.PieChart,
                title = "No budgets created",
                subtitle = "Set monthly or yearly spending limits by category to track adherence.",
                actionLabel = "Create Budget",
                onAction = onAddBudget,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.budgets, key = { it.budget.id }) { progress ->
                    val ratio = (progress.percentUsed / 100f).coerceIn(0f, 1f)
                    val barColor = when {
                        progress.isExceeded -> FinLedgerTheme.colors.negative
                        progress.isAlertTriggered -> FinLedgerTheme.colors.warning
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(progress.budget.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${progress.percentUsed}%", style = MaterialTheme.typography.bodyLarge, color = barColor)
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth(),
                                color = barColor,
                                trackColor = FinLedgerTheme.colors.dividerSoft,
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Spent: ${MoneyFormatter.format(progress.spentPaise, state.primaryCurrency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                                Text(
                                    text = "Limit: ${MoneyFormatter.format(progress.budget.amount, state.primaryCurrency)}",
                                    style = MaterialTheme.typography.bodySmall,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditBudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var isRollover by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create Budget", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Budget Name") },
                placeholder = { Text("e.g., Monthly Food Budget") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountTextField(
                value = limitText,
                onValueChange = { limitText = it; errorText = null },
                label = "Spending Limit",
                currency = state.primaryCurrency,
            )
            Text("Period", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                BudgetPeriod.entries.forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.displayName) })
                }
            }

            CategoryPickerGrid(
                categories = state.categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { selectedCategoryId = if (selectedCategoryId == it.id) null else it.id },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isRollover, onCheckedChange = { isRollover = it })
                Text("Roll over unused budget to next period", style = MaterialTheme.typography.bodySmall)
            }

            (errorText ?: state.errorMessage)?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val limit = MoneyFormatter.parseToPaise(limitText) ?: 0L
                    if (limit <= 0L) {
                        errorText = "Enter a valid positive limit"
                        return@Button
                    }
                    viewModel.saveBudget(
                        name = name,
                        limitPaise = limit,
                        period = period,
                        categoryId = selectedCategoryId,
                        alertThreshold = 80,
                        isRollover = isRollover,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Budget") }
        }
    }
}

@Composable
fun SavingsGoalsScreen(
    onBack: () -> Unit,
    onAddGoal: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var contributeGoalId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", style = MaterialTheme.typography.headlineMedium) },
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
                onClick = onAddGoal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add goal")
            }
        },
    ) { padding ->
        if (state.goals.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Savings,
                title = "No savings goals yet",
                subtitle = "Plan emergency funds, travel goals, or major purchases.",
                actionLabel = "Create Goal",
                onAction = onAddGoal,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    val progress = (goal.currentAmount.toFloat() / goal.targetAmount.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(goal.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = FinLedgerTheme.colors.dividerSoft,
                            )
                            Text(
                                text = "${MoneyFormatter.format(goal.currentAmount, state.primaryCurrency)} of ${MoneyFormatter.format(goal.targetAmount, state.primaryCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                            goal.targetDate?.let {
                                Text("Target: ${DateFormatter.formatDate(it)}", style = MaterialTheme.typography.labelMedium, color = FinLedgerTheme.colors.inkMuted48)
                            }
                            OutlinedButton(
                                onClick = { contributeGoalId = goal.id },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { Text("+ Add Contribution") }
                        }
                    }
                }
            }
        }
    }

    contributeGoalId?.let { goalId ->
        var amountText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { contributeGoalId = null },
            title = { Text("Contribute to Goal") },
            text = {
                AmountTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Amount",
                    currency = state.primaryCurrency,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = MoneyFormatter.parseToPaise(amountText) ?: 0L
                        if (amt > 0L) viewModel.contributeToGoal(goalId, amt)
                        contributeGoalId = null
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { contributeGoalId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun AddEditGoalScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("") }
    var targetDateMs by remember { mutableStateOf<Long?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create Savings Goal", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Goal Name") },
                placeholder = { Text("e.g., Emergency Fund, Japan Trip") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountTextField(
                value = targetText,
                onValueChange = { targetText = it; errorText = null },
                label = "Target Amount",
                currency = state.primaryCurrency,
            )
            AmountTextField(
                value = initialText,
                onValueChange = { initialText = it },
                label = "Saved So Far (Optional)",
                currency = state.primaryCurrency,
            )
            DatePickerField(
                dateMs = targetDateMs ?: (System.currentTimeMillis() + 180L * 86_400_000L),
                onDateSelected = { targetDateMs = it },
                label = "Target Date",
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (Optional)") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            (errorText ?: state.errorMessage)?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val target = MoneyFormatter.parseToPaise(targetText) ?: 0L
                    if (target <= 0L) {
                        errorText = "Enter a valid positive target amount"
                        return@Button
                    }
                    viewModel.saveGoal(
                        name = name,
                        description = description,
                        targetPaise = target,
                        currentPaise = MoneyFormatter.parseToPaise(initialText) ?: 0L,
                        targetDateMs = targetDateMs,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Goal") }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


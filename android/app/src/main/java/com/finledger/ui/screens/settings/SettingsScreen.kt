package com.finledger.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    onManageTags: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRecovery: () -> Unit,
    onOpenAuditHistory: () -> Unit,
    onSetupPin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingCurrencyCode by remember { mutableStateOf<String?>(null) }

    val currencies = MoneyFormatter.SUPPORTED_CURRENCIES

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text("Profile & Currency", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNameDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Profile Name", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                                Text(prefs.userName, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        Text("Primary Currency", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            currencies.forEach { code ->
                                FilterChip(
                                    selected = prefs.primaryCurrency == code,
                                    onClick = {
                                        if (code != prefs.primaryCurrency) {
                                            if (state.hasExistingFinancialData) {
                                                pendingCurrencyCode = code
                                            } else {
                                                viewModel.updateCurrency(code)
                                            }
                                        }
                                    },
                                    label = { Text(code) },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Appearance", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text("Theme", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                        ThemeSelectorRow(
                            selected = prefs.themeMode,
                            onSelect = viewModel::updateThemeMode,
                        )
                    }
                }
            }

            item {
                Text("App Complexity & Modules", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        Column(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text("Experience Mode", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                FilterChip(
                                    selected = prefs.isSimpleMode,
                                    onClick = { viewModel.setSimpleModePreset(true) },
                                    label = { Text("Simple Mode") },
                                )
                                FilterChip(
                                    selected = !prefs.isSimpleMode,
                                    onClick = { viewModel.setSimpleModePreset(false) },
                                    label = { Text("Advanced Mode") },
                                )
                            }
                            Text(
                                text = "Turning a module off hides its tabs & cards without deleting any saved records.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinLedgerTheme.colors.inkMuted48,
                            )
                        }
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Shared Expenses & Groups",
                            subtitle = "Friends, splits, groups & events",
                            checked = prefs.trackSharedAndPeople,
                            onCheckedChange = viewModel::setTrackSharedAndPeople,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Loans",
                            subtitle = "Money lent, borrowed & EMIs",
                            checked = prefs.trackLoans,
                            onCheckedChange = viewModel::setTrackLoans,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Investments & Net Worth",
                            subtitle = "Holdings, assets & net worth tracking",
                            checked = prefs.trackInvestments,
                            onCheckedChange = viewModel::setTrackInvestments,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Bills, Budgets & Goals",
                            subtitle = "Monthly limits, recurring bills & goals",
                            checked = prefs.trackPlanning,
                            onCheckedChange = viewModel::setTrackPlanning,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Optional Home Cards",
                            subtitle = "Net worth & investment summary cards on Home",
                            checked = prefs.showDashboardNetWorth,
                            onCheckedChange = viewModel::setShowDashboardNetWorth,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Home Alerts Banner",
                            subtitle = "Concise bill & budget alert on Home",
                            checked = prefs.showDashboardAlerts,
                            onCheckedChange = viewModel::setShowDashboardAlerts,
                        )
                    }
                }
            }

            item {
                Text("Organization", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        SettingsNavRow("Custom Categories", "${state.categories.size} active", onManageCategories)
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsNavRow("Tags", "${state.tags.size} tags", onManageTags)
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsNavRow("Notifications & Reminders", "Bills, loans & alerts", onOpenNotifications)
                    }
                }
            }

            item {
                Text("Security & Data", style = MaterialTheme.typography.titleSmall, color = FinLedgerTheme.colors.inkMuted48)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        SettingsNavRow(
                            "App Lock (PIN & Biometrics)",
                            if (prefs.pinEnabled) "Enabled" else "Off",
                            onSetupPin,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsSwitchRow(
                            title = "Scheduled Local Backup",
                            subtitle = "Weekly offline JSON snapshot",
                            checked = prefs.autoBackupEnabled,
                            onCheckedChange = viewModel::setAutoBackupEnabled,
                        )
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsNavRow("Backup & Export Settings", "JSON, CSV, PDF", onOpenBackup)
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsNavRow("Change Log", "Audit trail of edits", onOpenAuditHistory)
                        HorizontalDivider(color = FinLedgerTheme.colors.dividerSoft)
                        SettingsNavRow("Recovery (Trash & Archive)", "Restore deleted or archived items", onOpenRecovery)
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        var nameInput by remember { mutableStateOf(prefs.userName) }
        var nameError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Profile Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        nameError = false
                    },
                    singleLine = true,
                    label = { Text("Your Name") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name cannot be blank") }
                    } else null,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        viewModel.updateUserName(nameInput.trim())
                        showNameDialog = false
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    pendingCurrencyCode?.let { targetCode ->
        val oldSymbol = MoneyFormatter.currencySymbol(prefs.primaryCurrency).trim()
        val newSymbol = MoneyFormatter.currencySymbol(targetCode).trim()
        AlertDialog(
            onDismissRequest = { pendingCurrencyCode = null },
            title = { Text("Change Primary Currency?") },
            text = {
                Text(
                    "Existing amounts will keep their numeric values (e.g., ${oldSymbol}100 becomes ${newSymbol}100) without FX conversion. Continue?",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCurrency(targetCode)
                        pendingCurrencyCode = null
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Change Currency") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurrencyCode = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = FinLedgerTheme.colors.inkMuted48,
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}


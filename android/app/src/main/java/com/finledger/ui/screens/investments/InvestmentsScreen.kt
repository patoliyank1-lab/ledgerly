package com.finledger.ui.screens.investments

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.InvestmentTransactionType
import com.finledger.data.local.entity.InvestmentType
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.components.EmptyStateView
import com.finledger.ui.components.ExpandableSection
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun InvestmentsScreen(
    onBack: () -> Unit,
    onAddInvestment: () -> Unit,
    onOpenInvestmentDetail: (Long) -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val portfolio = state.portfolio

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Investments & Assets", style = MaterialTheme.typography.headlineMedium) },
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
                onClick = onAddInvestment,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add investment")
            }
        },
    ) { padding ->
        if (portfolio.items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.TrendingUp,
                title = "No investments tracked yet",
                subtitle = "Track stocks, mutual funds, SIPs, fixed deposits, gold, bonds, and real estate.",
                actionLabel = "Add Investment",
                onAction = onAddInvestment,
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
                    Surface(
                        color = FinLedgerTheme.colors.tileDark1,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text("Portfolio Value", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                            Text(
                                text = MoneyFormatter.format(portfolio.totalCurrentValuePaise, state.primaryCurrency),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Invested: ${MoneyFormatter.format(portfolio.totalInvestedPaise, state.primaryCurrency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.bodyMuted,
                                )
                                Text(
                                    text = "Unrealized: ${MoneyFormatter.format(portfolio.totalUnrealizedPnLPaise, state.primaryCurrency, showPlus = true)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (portfolio.totalUnrealizedPnLPaise >= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(Spacing.md)) }

                items(portfolio.items, key = { it.investment.id }) { item ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
                            .clickable { onOpenInvestmentDetail(item.investment.id) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.investment.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${item.investment.type.displayName} · ${"%.2f".format(item.investment.currentUnits)} units",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = MoneyFormatter.format(item.currentValuePaise, state.primaryCurrency),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "${MoneyFormatter.format(item.unrealizedPnLPaise, state.primaryCurrency, showPlus = true)} (${"%.1f".format(item.unrealizedPnLPercent)}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.unrealizedPnLPaise >= 0) FinLedgerTheme.colors.positive else FinLedgerTheme.colors.negative,
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
fun AddInvestmentScreen(
    onBack: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(InvestmentType.MUTUAL_FUND) }
    var platform by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var unitsText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var feesText by remember { mutableStateOf("") }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var showMoreOptions by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = state.accounts.first().account.id
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Add Investment", style = MaterialTheme.typography.headlineMedium) },
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
                label = { Text("Asset / Instrument Name") },
                placeholder = { Text("e.g., Nifty 50 Index Fund, Gold ETF") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Asset Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                InvestmentType.entries.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.displayName) })
                }
            }
            OutlinedTextField(
                value = unitsText,
                onValueChange = { unitsText = it; errorText = null },
                label = { Text("Quantity / Units") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountTextField(
                value = priceText,
                onValueChange = { priceText = it; errorText = null },
                label = "Purchase Price per Unit",
                currency = state.primaryCurrency,
            )

            val defaultAccName = state.accounts.firstOrNull { it.account.id == selectedAccountId }?.account?.name
            ExpandableSection(
                title = "More options",
                subtitle = if (!showMoreOptions) {
                    val accHint = defaultAccName?.let { "Account: $it · " }.orEmpty()
                    "${accHint}Broker, ticker, fees, purchase date, notes"
                } else {
                    null
                },
                expanded = showMoreOptions,
                onToggle = { showMoreOptions = !showMoreOptions },
            ) {
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("Platform / Broker (Optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Ticker / Symbol (Optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountTextField(
                    value = feesText,
                    onValueChange = { feesText = it },
                    label = "Brokerage / Fees / Taxes (Optional)",
                    currency = state.primaryCurrency,
                )
                DatePickerField(dateMs = dateMs, onDateSelected = { dateMs = it }, label = "Purchase Date")
                AccountPickerDropdown(
                    accounts = state.accounts,
                    selectedAccountId = selectedAccountId,
                    onSelectAccount = { selectedAccountId = it.account.id },
                    label = "Paid From Account",
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            (errorText ?: state.errorMessage)?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val units = unitsText.toDoubleOrNull() ?: 0.0
                    val price = MoneyFormatter.parseToPaise(priceText) ?: 0L
                    if (units <= 0.0 || price <= 0L) {
                        errorText = "Enter valid units and price"
                        return@Button
                    }
                    viewModel.createInvestment(
                        name = name,
                        type = type,
                        platform = platform,
                        symbol = symbol,
                        units = units,
                        pricePerUnitPaise = price,
                        feesPaise = MoneyFormatter.parseToPaise(feesText) ?: 0L,
                        taxesPaise = 0L,
                        dateMs = dateMs,
                        accountId = selectedAccountId,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Investment") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvestmentDetailScreen(
    investmentId: Long,
    onBack: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val trades by viewModel.observeTrades(investmentId).collectAsStateWithLifecycle()
    val item = state.portfolio.items.firstOrNull { it.investment.id == investmentId }
    var showValDialog by remember { mutableStateOf(false) }
    var activeTradeType by remember { mutableStateOf<InvestmentTransactionType?>(null) }
    val undoCoordinator = LocalUndoCoordinator.current
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
                title = { Text(item?.investment?.name ?: "Investment", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(
                            onClick = {
                                val label = item.investment.name.ifBlank { "investment" }
                                viewModel.deleteInvestment(investmentId) {
                                    undoCoordinator?.showDeleted(label) {
                                        viewModel.restoreInvestmentDirect(investmentId)
                                    }
                                    onBack()
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete investment")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (item == null) {
            Column(modifier = Modifier.padding(padding).padding(Spacing.lg)) {
                Text("Investment not found")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Surface(
                    color = FinLedgerTheme.colors.tileDark1,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(item.investment.type.displayName, style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.bodyMuted)
                        Text(
                            text = MoneyFormatter.format(item.currentValuePaise, state.primaryCurrency),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Units: ${"%.2f".format(item.investment.currentUnits)} · Avg Buy: ${MoneyFormatter.format(item.investment.averageBuyPrice, state.primaryCurrency)} · Current: ${MoneyFormatter.format(item.investment.currentPrice, state.primaryCurrency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.bodyMuted,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Realized P&L: ${MoneyFormatter.format(item.investment.realizedGainPaise, state.primaryCurrency, showPlus = true)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinLedgerTheme.colors.skyLinkBlue,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Button(onClick = { showValDialog = true }, shape = MaterialTheme.shapes.extraLarge) {
                                Text("Update Price")
                            }
                            OutlinedButton(
                                onClick = { activeTradeType = InvestmentTransactionType.BUY },
                                shape = MaterialTheme.shapes.extraLarge,
                                border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                            ) {
                                Text("Buy / SIP", color = FinLedgerTheme.colors.skyLinkBlue)
                            }
                            OutlinedButton(
                                onClick = { activeTradeType = InvestmentTransactionType.SELL },
                                shape = MaterialTheme.shapes.extraLarge,
                                border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                            ) {
                                Text("Sell", color = FinLedgerTheme.colors.skyLinkBlue)
                            }
                            OutlinedButton(
                                onClick = { activeTradeType = InvestmentTransactionType.DIVIDEND },
                                shape = MaterialTheme.shapes.extraLarge,
                                border = BorderStroke(1.dp, FinLedgerTheme.colors.skyLinkBlue),
                            ) {
                                Text("Dividend", color = FinLedgerTheme.colors.skyLinkBlue)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleSmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
            }

            items(trades, key = { it.id }) { t ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("${t.type.name} · ${"%.2f".format(t.units)} units", style = MaterialTheme.typography.bodyLarge)
                            Text(DateFormatter.formatDate(t.date), style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                        }
                        Text(MoneyFormatter.format(t.totalAmount, state.primaryCurrency), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showValDialog && item != null) {
        var priceText by remember { mutableStateOf(MoneyFormatter.paiseToInputString(item.investment.currentPrice)) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showValDialog = false },
            title = { Text("Update Current Price") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AmountTextField(
                        value = priceText,
                        onValueChange = { priceText = it; dialogError = null },
                        label = "Current Price per Unit",
                        currency = state.primaryCurrency,
                    )
                    dialogError?.let {
                        Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = MoneyFormatter.parseToPaise(priceText) ?: 0L
                        if (p <= 0L) {
                            dialogError = "Enter a valid positive price"
                            return@Button
                        }
                        viewModel.updateValuation(investmentId, p) { showValDialog = false }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { showValDialog = false }) { Text("Cancel") } },
        )
    }

    activeTradeType?.let { tradeType ->
        if (item == null) return@let
        var selectedTradeType by remember(tradeType) { mutableStateOf(tradeType) }
        var unitsText by remember(tradeType) {
            mutableStateOf(if (tradeType == InvestmentTransactionType.SELL) item.investment.currentUnits.toString() else "1")
        }
        var priceText by remember(tradeType) {
            mutableStateOf(
                if (tradeType == InvestmentTransactionType.DIVIDEND) ""
                else MoneyFormatter.paiseToInputString(item.investment.currentPrice)
            )
        }
        var feesText by remember(tradeType) { mutableStateOf("") }
        var dialogError by remember(tradeType) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { activeTradeType = null },
            title = {
                Text(
                    when (selectedTradeType) {
                        InvestmentTransactionType.BUY -> "Buy More Units"
                        InvestmentTransactionType.SIP -> "Record SIP Installment"
                        InvestmentTransactionType.SELL -> "Sell Units"
                        InvestmentTransactionType.DIVIDEND -> "Record Dividend / Interest"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (tradeType == InvestmentTransactionType.BUY || tradeType == InvestmentTransactionType.SIP) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            FilterChip(
                                selected = selectedTradeType == InvestmentTransactionType.BUY,
                                onClick = { selectedTradeType = InvestmentTransactionType.BUY },
                                label = { Text("BUY") },
                            )
                            FilterChip(
                                selected = selectedTradeType == InvestmentTransactionType.SIP,
                                onClick = { selectedTradeType = InvestmentTransactionType.SIP },
                                label = { Text("SIP") },
                            )
                        }
                    }
                    if (selectedTradeType != InvestmentTransactionType.DIVIDEND) {
                        OutlinedTextField(
                            value = unitsText,
                            onValueChange = { unitsText = it; dialogError = null },
                            label = { Text("Units") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    AmountTextField(
                        value = priceText,
                        onValueChange = { priceText = it; dialogError = null },
                        label = if (selectedTradeType == InvestmentTransactionType.DIVIDEND) "Dividend Amount" else "Price per Unit",
                        currency = state.primaryCurrency,
                    )
                    if (selectedTradeType != InvestmentTransactionType.DIVIDEND) {
                        AmountTextField(
                            value = feesText,
                            onValueChange = { feesText = it },
                            label = "Fees & Taxes (Optional)",
                            currency = state.primaryCurrency,
                        )
                    }
                    dialogError?.let {
                        Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val u = if (selectedTradeType == InvestmentTransactionType.DIVIDEND) 0.0 else (unitsText.toDoubleOrNull() ?: 0.0)
                        val p = MoneyFormatter.parseToPaise(priceText) ?: 0L
                        val f = MoneyFormatter.parseToPaise(feesText) ?: 0L
                        if (selectedTradeType != InvestmentTransactionType.DIVIDEND && u <= 0.0) {
                            dialogError = "Enter positive units"
                            return@Button
                        }
                        if (selectedTradeType == InvestmentTransactionType.SELL && u > item.investment.currentUnits + 0.0001) {
                            dialogError = "Cannot sell more than ${item.investment.currentUnits} units"
                            return@Button
                        }
                        if (p <= 0L) {
                            dialogError = "Enter a valid positive amount"
                            return@Button
                        }
                        val accId = item.investment.accountId ?: state.accounts.firstOrNull()?.account?.id
                        viewModel.recordTrade(
                            investmentId = investmentId,
                            tradeType = selectedTradeType,
                            units = u,
                            pricePerUnitPaise = p,
                            feesPaise = f,
                            accountId = accId,
                            notes = "Manual ${selectedTradeType.name.lowercase()}",
                            onSuccess = { activeTradeType = null },
                        )
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { activeTradeType = null }) { Text("Cancel") } },
        )
    }
}


package com.finledger.ui.screens.transactions

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.DocumentEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.domain.model.QuickTransactionTemplate
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.util.AmountExpressionEvaluator
import com.finledger.domain.util.MoneyFormatter
import java.util.UUID
import com.finledger.ui.components.AccountPickerDropdown
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.CategoryPickerGrid
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.components.ExpandableSection
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

/**
 * Transaction type tabs supported by the unified Quick Add flow.
 */
enum class QuickAddTab(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer");

    companion object {
        fun fromArg(raw: String?): QuickAddTab =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EXPENSE
    }
}

sealed class QuickAddValidationResult {
    data class ValidExpenseOrIncome(val amountPaise: Long, val accountId: Long) : QuickAddValidationResult()
    data class ValidTransfer(val amountPaise: Long, val fromAccountId: Long, val toAccountId: Long) : QuickAddValidationResult()
    data class Invalid(val message: String, val isAmountError: Boolean = false) : QuickAddValidationResult()
}

object QuickAddValidator {
    fun validate(
        tab: QuickAddTab,
        amountText: String,
        accountId: Long?,
        toAccountId: Long?,
    ): QuickAddValidationResult {
        val paise = AmountExpressionEvaluator.evaluateToPaise(amountText)
        if (paise == null || paise <= 0L) {
            return QuickAddValidationResult.Invalid("Enter a valid positive amount", isAmountError = true)
        }
        return when (tab) {
            QuickAddTab.EXPENSE, QuickAddTab.INCOME -> {
                val resolvedAccount = accountId
                    ?: return QuickAddValidationResult.Invalid("Please create or select an account first")
                QuickAddValidationResult.ValidExpenseOrIncome(paise, resolvedAccount)
            }
            QuickAddTab.TRANSFER -> {
                val from = accountId
                val to = toAccountId
                if (from == null || to == null) {
                    return QuickAddValidationResult.Invalid("Select both source and destination accounts")
                }
                if (from == to) {
                    return QuickAddValidationResult.Invalid("Source and destination accounts must differ")
                }
                QuickAddValidationResult.ValidTransfer(paise, from, to)
            }
        }
    }
}

/**
 * Unified amount-first transaction creation screen combining Expense, Income, and Transfer flows.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun QuickAddScreen(
    initialTab: QuickAddTab = QuickAddTab.EXPENSE,
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableStateOf(initialTab) }
    var amountText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Expense-scoped state
    var selectedExpenseCategoryId by remember { mutableStateOf<Long?>(null) }
    var userManuallyPickedExpenseCategory by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var expenseDescription by remember { mutableStateOf("") }
    var expenseNotes by remember { mutableStateOf("") }
    var isRefund by remember { mutableStateOf(false) }
    var selectedExpenseTags by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Income-scoped state
    var selectedIncomeCategoryId by remember { mutableStateOf<Long?>(null) }
    var userManuallyPickedIncomeCategory by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf("") }
    var incomeDescription by remember { mutableStateOf("") }
    var incomeNotes by remember { mutableStateOf("") }
    var selectedIncomeTags by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Transfer-scoped state
    var transferNotes by remember { mutableStateOf("") }

    var showMoreDetails by remember { mutableStateOf(false) }
    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVaultDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateLabelDraft by remember { mutableStateOf("") }
    var templatePendingDelete by remember { mutableStateOf<QuickTransactionTemplate?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isAmountValidationError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearError() }
    }

    LaunchedEffect(
        state.accounts,
        state.expenseCategories,
        state.incomeCategories,
        state.topExpenseCategoryIds,
        state.topIncomeCategoryIds,
        state.lastUsedAccountId,
    ) {
        if (selectedAccountId == null && state.accounts.isNotEmpty()) {
            selectedAccountId = TransactionsViewModel.resolveDefaultAccountId(
                state.accounts,
                state.lastUsedAccountId,
            )
        }
        if (toAccountId == null && state.accounts.size > 1) {
            toAccountId = state.accounts.firstOrNull { it.account.id != selectedAccountId }?.account?.id
        }
        if (selectedExpenseCategoryId == null && state.expenseCategories.isNotEmpty()) {
            selectedExpenseCategoryId = state.topExpenseCategoryIds.firstOrNull()
                ?: state.expenseCategories.first().id
        }
        if (selectedIncomeCategoryId == null && state.incomeCategories.isNotEmpty()) {
            selectedIncomeCategoryId = state.topIncomeCategoryIds.firstOrNull()
                ?: state.incomeCategories.first().id
        }
    }

    fun clearErrors() {
        localError = null
        isAmountValidationError = false
        viewModel.clearError()
    }

    fun applyPrefill(
        type: TransactionType,
        amountPaise: Long,
        accountId: Long?,
        toAccountIdVal: Long?,
        categoryId: Long?,
        merchantOrPayee: String,
        description: String,
        paymentMethodVal: String,
        locationVal: String,
        notesVal: String,
    ) {
        if (amountPaise > 0L) {
            amountText = MoneyFormatter.paiseToInputString(amountPaise)
        }
        dateMs = System.currentTimeMillis()
        if (accountId != null && state.accounts.any { it.account.id == accountId }) {
            selectedAccountId = accountId
        }
        when (type) {
            TransactionType.EXPENSE, TransactionType.REFUND -> {
                activeTab = QuickAddTab.EXPENSE
                isRefund = type == TransactionType.REFUND
                if (categoryId != null && state.expenseCategories.any { it.id == categoryId }) {
                    selectedExpenseCategoryId = categoryId
                    userManuallyPickedExpenseCategory = true
                }
                merchant = merchantOrPayee
                expenseDescription = description
                paymentMethod = paymentMethodVal
                location = locationVal
                expenseNotes = notesVal
            }
            TransactionType.INCOME -> {
                activeTab = QuickAddTab.INCOME
                isRefund = false
                if (categoryId != null && state.incomeCategories.any { it.id == categoryId }) {
                    selectedIncomeCategoryId = categoryId
                    userManuallyPickedIncomeCategory = true
                }
                source = merchantOrPayee
                incomeDescription = description
                incomeNotes = notesVal
            }
            TransactionType.TRANSFER -> {
                activeTab = QuickAddTab.TRANSFER
                isRefund = false
                if (toAccountIdVal != null && state.accounts.any { it.account.id == toAccountIdVal }) {
                    toAccountId = toAccountIdVal
                }
                transferNotes = notesVal
            }
            else -> Unit
        }
        clearErrors()
    }

    val topBarTitle = if (activeTab == QuickAddTab.EXPENSE && isRefund) {
        "Add Refund"
    } else {
        "Quick Add"
    }

    val saveLabel = when (activeTab) {
        QuickAddTab.EXPENSE -> if (isRefund) "Save Refund" else "Save Expense"
        QuickAddTab.INCOME -> "Save Income"
        QuickAddTab.TRANSFER -> "Record Transfer"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle, style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val defaultName = when (activeTab) {
                                QuickAddTab.EXPENSE -> merchant.ifBlank {
                                    state.expenseCategories.firstOrNull { it.id == selectedExpenseCategoryId }?.name ?: "Expense"
                                }
                                QuickAddTab.INCOME -> source.ifBlank {
                                    state.incomeCategories.firstOrNull { it.id == selectedIncomeCategoryId }?.name ?: "Income"
                                }
                                QuickAddTab.TRANSFER -> "Transfer"
                            }
                            templateLabelDraft = defaultName
                            showSaveTemplateDialog = true
                        },
                    ) {
                        Text("Save template")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        (localError ?: state.errorMessage)?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                color = FinLedgerTheme.colors.negative,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(
                            onClick = {
                                when (
                                    val result = QuickAddValidator.validate(
                                        tab = activeTab,
                                        amountText = amountText,
                                        accountId = selectedAccountId,
                                        toAccountId = toAccountId,
                                    )
                                ) {
                                    is QuickAddValidationResult.Invalid -> {
                                        localError = result.message
                                        isAmountValidationError = result.isAmountError
                                    }
                                    is QuickAddValidationResult.ValidExpenseOrIncome -> {
                                        if (activeTab == QuickAddTab.EXPENSE) {
                                            viewModel.addExpense(
                                                amountPaise = result.amountPaise,
                                                accountId = result.accountId,
                                                categoryId = selectedExpenseCategoryId,
                                                dateMs = dateMs,
                                                merchant = merchant,
                                                location = location,
                                                paymentMethod = paymentMethod,
                                                description = expenseDescription,
                                                notes = expenseNotes,
                                                isRefund = isRefund,
                                                tagIds = selectedExpenseTags.toList(),
                                                pendingReceiptUri = pendingReceiptUri,
                                                pendingVaultDocumentId = pendingVaultDoc?.id,
                                                onSuccess = onBack,
                                            )
                                        } else {
                                            viewModel.addIncome(
                                                amountPaise = result.amountPaise,
                                                accountId = result.accountId,
                                                categoryId = selectedIncomeCategoryId,
                                                dateMs = dateMs,
                                                source = source,
                                                description = incomeDescription,
                                                notes = incomeNotes,
                                                tagIds = selectedIncomeTags.toList(),
                                                pendingReceiptUri = pendingReceiptUri,
                                                pendingVaultDocumentId = pendingVaultDoc?.id,
                                                onSuccess = onBack,
                                            )
                                        }
                                    }
                                    is QuickAddValidationResult.ValidTransfer -> {
                                        viewModel.addTransfer(
                                            amountPaise = result.amountPaise,
                                            fromAccountId = result.fromAccountId,
                                            toAccountId = result.toAccountId,
                                            dateMs = dateMs,
                                            notes = transferNotes,
                                            pendingReceiptUri = pendingReceiptUri,
                                            pendingVaultDocumentId = pendingVaultDoc?.id,
                                            onSuccess = onBack,
                                        )
                                    }
                                }
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(saveLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
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
            QuickAddModeToggle(
                selectedTab = activeTab,
                onSelectTab = { nextTab ->
                    if (nextTab != activeTab) {
                        activeTab = nextTab
                        showMoreDetails = false
                        clearErrors()
                    }
                },
            )

            if (state.lastTransaction != null || state.quickTemplates.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    state.lastTransaction?.let { lastTxDetails ->
                        item(key = "repeat_last") {
                            val tx = lastTxDetails.transaction
                            val previewTitle = tx.merchantOrPayee.ifBlank {
                                lastTxDetails.category?.name ?: tx.type.displayName
                            }
                            val previewAmount = MoneyFormatter.format(tx.amount, state.primaryCurrency)
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .clickable {
                                        applyPrefill(
                                            type = tx.type,
                                            amountPaise = tx.amount,
                                            accountId = tx.accountId,
                                            toAccountIdVal = tx.toAccountId,
                                            categoryId = tx.categoryId,
                                            merchantOrPayee = tx.merchantOrPayee,
                                            description = tx.description,
                                            paymentMethodVal = tx.paymentMethod,
                                            locationVal = tx.location,
                                            notesVal = tx.notes,
                                        )
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Replay,
                                        contentDescription = "Repeat last transaction",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(
                                        text = "Repeat last: $previewTitle · $previewAmount",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    items(items = state.quickTemplates, key = { it.id }) { tpl ->
                        val chipText = if (tpl.amountPaise > 0L) {
                            "${tpl.label} · ${MoneyFormatter.format(tpl.amountPaise, state.primaryCurrency)}"
                        } else {
                            tpl.label
                        }
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .combinedClickable(
                                    onClick = {
                                        applyPrefill(
                                            type = tpl.type,
                                            amountPaise = tpl.amountPaise,
                                            accountId = tpl.accountId,
                                            toAccountIdVal = tpl.toAccountId,
                                            categoryId = tpl.categoryId,
                                            merchantOrPayee = tpl.merchantOrPayee,
                                            description = tpl.description,
                                            paymentMethodVal = tpl.paymentMethod,
                                            locationVal = tpl.location,
                                            notesVal = tpl.notes,
                                        )
                                    },
                                    onLongClick = { templatePendingDelete = tpl },
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = Spacing.sm, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = chipText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Delete template ${tpl.label}",
                                    tint = FinLedgerTheme.colors.inkMuted48,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { templatePendingDelete = tpl },
                                )
                            }
                        }
                    }
                }
            }

            AmountTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    clearErrors()
                },
                label = when (activeTab) {
                    QuickAddTab.EXPENSE -> if (isRefund) "Refund Amount" else "Amount"
                    QuickAddTab.INCOME -> "Income Amount"
                    QuickAddTab.TRANSFER -> "Transfer Amount"
                },
                currency = state.primaryCurrency,
                isError = isAmountValidationError && localError != null,
                supportingText = if (isAmountValidationError) localError else null,
                autoFocus = true,
                allowCalculator = true,
            )

            AccountPickerDropdown(
                accounts = state.accounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = {
                    selectedAccountId = it.account.id
                    clearErrors()
                },
                label = when (activeTab) {
                    QuickAddTab.EXPENSE -> if (isRefund) "Refund To Account" else "Paid From Account"
                    QuickAddTab.INCOME -> "Deposit To Account"
                    QuickAddTab.TRANSFER -> "From Account"
                },
            )

            if (activeTab == QuickAddTab.TRANSFER) {
                AccountPickerDropdown(
                    accounts = state.accounts,
                    selectedAccountId = toAccountId,
                    onSelectAccount = {
                        toAccountId = it.account.id
                        clearErrors()
                    },
                    label = "To Account (or Credit Card)",
                )
            } else {
                CategoryPickerGrid(
                    categories = if (activeTab == QuickAddTab.EXPENSE) state.expenseCategories else state.incomeCategories,
                    selectedCategoryId = if (activeTab == QuickAddTab.EXPENSE) selectedExpenseCategoryId else selectedIncomeCategoryId,
                    onSelectCategory = { cat ->
                        if (activeTab == QuickAddTab.EXPENSE) {
                            selectedExpenseCategoryId = cat.id
                            userManuallyPickedExpenseCategory = true
                        } else {
                            selectedIncomeCategoryId = cat.id
                            userManuallyPickedIncomeCategory = true
                        }
                    },
                    preferredOrderIds = if (activeTab == QuickAddTab.EXPENSE) {
                        state.topExpenseCategoryIds
                    } else {
                        state.topIncomeCategoryIds
                    },
                    compactLimit = TransactionsViewModel.TOP_CATEGORY_LIMIT,
                )
            }

            DatePickerField(
                dateMs = dateMs,
                onDateSelected = { dateMs = it },
            )

            ExpandableSection(
                title = "More details",
                subtitle = if (!showMoreDetails) {
                    when (activeTab) {
                        QuickAddTab.EXPENSE -> "Merchant, payment method, tags, refund, notes"
                        QuickAddTab.INCOME -> "Source / payer, description, tags, notes"
                        QuickAddTab.TRANSFER -> "Notes (transfers keep totals unchanged)"
                    }
                } else {
                    null
                },
                expanded = showMoreDetails,
                onToggle = { showMoreDetails = !showMoreDetails },
            ) {
                when (activeTab) {
                    QuickAddTab.EXPENSE -> {
                        val merchantSuggestions = remember(state.merchantHistory.topExpenseMerchants, merchant) {
                            TransactionsViewModel.filterMerchantSuggestions(
                                merchants = state.merchantHistory.topExpenseMerchants,
                                query = merchant,
                            )
                        }
                        val onMerchantChanged: (String) -> Unit = { input ->
                            merchant = input
                            if (!userManuallyPickedExpenseCategory) {
                                TransactionsViewModel.suggestCategoryForMerchant(
                                    merchantInput = input,
                                    categoryMap = state.merchantHistory.expenseCategoryByMerchant,
                                )?.let { suggestedId ->
                                    if (state.expenseCategories.any { it.id == suggestedId }) {
                                        selectedExpenseCategoryId = suggestedId
                                    }
                                }
                            }
                        }
                        MerchantFieldWithSuggestions(
                            value = merchant,
                            onValueChange = onMerchantChanged,
                            label = "Merchant / Payee",
                            placeholder = null,
                            suggestions = merchantSuggestions,
                            onSelectSuggestion = onMerchantChanged,
                        )

                        OutlinedTextField(
                            value = expenseDescription,
                            onValueChange = { expenseDescription = it },
                            label = { Text("Description") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = paymentMethod,
                                onValueChange = { paymentMethod = it },
                                label = { Text("Payment Method") },
                                placeholder = { Text("UPI, Card, Cash") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Location") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (state.tags.isNotEmpty()) {
                            QuickAddTagsSection(
                                tags = state.tags,
                                selectedTags = selectedExpenseTags,
                                onToggleTag = { tagId ->
                                    selectedExpenseTags = if (tagId in selectedExpenseTags) {
                                        selectedExpenseTags - tagId
                                    } else {
                                        selectedExpenseTags + tagId
                                    }
                                },
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = isRefund,
                                onCheckedChange = { isRefund = it },
                            )
                            Text(
                                text = "This is a refund (reduces category spending)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        OutlinedTextField(
                            value = expenseNotes,
                            onValueChange = { expenseNotes = it },
                            label = { Text("Notes") },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    QuickAddTab.INCOME -> {
                        val sourceSuggestions = remember(state.merchantHistory.topIncomeSources, source) {
                            TransactionsViewModel.filterMerchantSuggestions(
                                merchants = state.merchantHistory.topIncomeSources,
                                query = source,
                            )
                        }
                        val onSourceChanged: (String) -> Unit = { input ->
                            source = input
                            if (!userManuallyPickedIncomeCategory) {
                                TransactionsViewModel.suggestCategoryForMerchant(
                                    merchantInput = input,
                                    categoryMap = state.merchantHistory.incomeCategoryBySource,
                                )?.let { suggestedId ->
                                    if (state.incomeCategories.any { it.id == suggestedId }) {
                                        selectedIncomeCategoryId = suggestedId
                                    }
                                }
                            }
                        }
                        MerchantFieldWithSuggestions(
                            value = source,
                            onValueChange = onSourceChanged,
                            label = "Income Source / Payer",
                            placeholder = "e.g., Employer, Client",
                            suggestions = sourceSuggestions,
                            onSelectSuggestion = onSourceChanged,
                        )

                        OutlinedTextField(
                            value = incomeDescription,
                            onValueChange = { incomeDescription = it },
                            label = { Text("Description") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (state.tags.isNotEmpty()) {
                            QuickAddTagsSection(
                                tags = state.tags,
                                selectedTags = selectedIncomeTags,
                                onToggleTag = { tagId ->
                                    selectedIncomeTags = if (tagId in selectedIncomeTags) {
                                        selectedIncomeTags - tagId
                                    } else {
                                        selectedIncomeTags + tagId
                                    }
                                },
                            )
                        }

                        OutlinedTextField(
                            value = incomeNotes,
                            onValueChange = { incomeNotes = it },
                            label = { Text("Notes") },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    QuickAddTab.TRANSFER -> {
                        OutlinedTextField(
                            value = transferNotes,
                            onValueChange = { transferNotes = it },
                            label = { Text("Notes (Optional)") },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                TransactionReceiptAttachRow(
                    vaultDocuments = state.vaultDocuments,
                    onPickFileUri = { uri ->
                        pendingReceiptUri = uri
                        pendingVaultDoc = null
                    },
                    onPickVaultDocument = { doc ->
                        pendingVaultDoc = doc
                        pendingReceiptUri = null
                    },
                    stagedLabel = pendingVaultDoc?.name ?: pendingReceiptUri?.lastPathSegment,
                    onClearStaged = {
                        pendingReceiptUri = null
                        pendingVaultDoc = null
                    },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }

    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save Quick Template") },
            text = {
                OutlinedTextField(
                    value = templateLabelDraft,
                    onValueChange = { templateLabelDraft = it },
                    label = { Text("Template name") },
                    placeholder = { Text("e.g., Tea, Auto, Coffee") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val label = templateLabelDraft.trim()
                        if (label.isNotEmpty()) {
                            val paise = AmountExpressionEvaluator.evaluateToPaise(amountText)?.coerceAtLeast(0L) ?: 0L
                            val template = when (activeTab) {
                                QuickAddTab.EXPENSE -> QuickTransactionTemplate(
                                    id = UUID.randomUUID().toString(),
                                    label = label,
                                    type = if (isRefund) TransactionType.REFUND else TransactionType.EXPENSE,
                                    amountPaise = paise,
                                    accountId = selectedAccountId,
                                    categoryId = selectedExpenseCategoryId,
                                    merchantOrPayee = merchant.trim(),
                                    description = expenseDescription.trim(),
                                    paymentMethod = paymentMethod.trim(),
                                    location = location.trim(),
                                    notes = expenseNotes.trim(),
                                )
                                QuickAddTab.INCOME -> QuickTransactionTemplate(
                                    id = UUID.randomUUID().toString(),
                                    label = label,
                                    type = TransactionType.INCOME,
                                    amountPaise = paise,
                                    accountId = selectedAccountId,
                                    categoryId = selectedIncomeCategoryId,
                                    merchantOrPayee = source.trim(),
                                    description = incomeDescription.trim(),
                                    notes = incomeNotes.trim(),
                                )
                                QuickAddTab.TRANSFER -> QuickTransactionTemplate(
                                    id = UUID.randomUUID().toString(),
                                    label = label,
                                    type = TransactionType.TRANSFER,
                                    amountPaise = paise,
                                    accountId = selectedAccountId,
                                    toAccountId = toAccountId,
                                    notes = transferNotes.trim(),
                                )
                            }
                            viewModel.saveQuickTemplate(template)
                            showSaveTemplateDialog = false
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    templatePendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { templatePendingDelete = null },
            title = { Text("Delete Template?") },
            text = { Text("Remove \"${pending.label}\" from your Quick Add shortcuts?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteQuickTemplate(pending.id)
                        templatePendingDelete = null
                    },
                ) {
                    Text("Delete", color = FinLedgerTheme.colors.negative)
                }
            },
            dismissButton = {
                TextButton(onClick = { templatePendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun QuickAddModeToggle(
    selectedTab: QuickAddTab,
    onSelectTab: (QuickAddTab) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            QuickAddTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { onSelectTab(tab) },
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else FinLedgerTheme.colors.inkMuted80,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddTagsSection(
    tags: List<com.finledger.data.local.entity.TagEntity>,
    selectedTags: Set<Long>,
    onToggleTag: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
            color = FinLedgerTheme.colors.inkMuted48,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            tags.forEach { tag ->
                val selected = tag.id in selectedTags
                FilterChip(
                    selected = selected,
                    onClick = { onToggleTag(tag.id) },
                    label = { Text(tag.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantFieldWithSuggestions(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    suggestions: List<String>,
    onSelectSuggestion: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { text -> { Text(text) } },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                suggestions.forEach { suggestion ->
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .clickable { onSelectSuggestion(suggestion) },
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium,
                            color = FinLedgerTheme.colors.inkMuted80,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

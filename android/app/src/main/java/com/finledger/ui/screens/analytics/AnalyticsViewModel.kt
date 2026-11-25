package com.finledger.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.NetWorthSnapshotEntity
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val categoryBreakdown: List<CategorySpendingItem> = emptyList(),
    val cashFlowPoints: List<MonthlyCashFlowPoint> = emptyList(),
    val periodComparison: PeriodComparison = PeriodComparison(0, 0, 0, 0),
    val agingReport: List<AgingBucketPerson> = emptyList(),
    val netWorth: NetWorthBreakdown = NetWorthBreakdown(0, 0, 0, 0, 0, 0, 0, 0, 0),
    val netWorthSnapshots: List<NetWorthSnapshotEntity> = emptyList(),
    val hasSelectedCategory: Boolean = false,
    val selectedCategoryId: Long? = null,
    val drilldownTransactions: List<TransactionWithDetails> = emptyList(),
    val primaryCurrency: String = "INR",
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCategoryBreakdownUseCase: GetCategoryBreakdownUseCase,
    getCashFlowUseCase: GetCashFlowUseCase,
    getPeriodComparisonUseCase: GetPeriodComparisonUseCase,
    getAgingReportUseCase: GetAgingReportUseCase,
    private val getNetWorthUseCase: GetNetWorthUseCase,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val initialBounds = GetMonthlySummaryUseCase.monthBounds()
    private val _dateRange = MutableStateFlow(initialBounds)
    private val _selectedCategory = MutableStateFlow<Pair<Boolean, Long?>>(false to null)

    private val categoryBreakdownFlow = _dateRange.flatMapLatest { (start, end) ->
        getCategoryBreakdownUseCase(start, end)
    }

    private val drilldownFlow = combine(_selectedCategory, _dateRange) { sel, range ->
        sel to range
    }.flatMapLatest { (sel, range) ->
        if (!sel.first) kotlinx.coroutines.flow.flowOf(emptyList())
        else getCategoryBreakdownUseCase.observeTransactionsInCategory(sel.second, range.first, range.second)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine<Any?, AnalyticsUiState>(
        _dateRange,
        categoryBreakdownFlow,
        getCashFlowUseCase(6),
        getPeriodComparisonUseCase(),
        getAgingReportUseCase(),
        getNetWorthUseCase(),
        getNetWorthUseCase.observeSnapshots(),
        _selectedCategory,
        drilldownFlow,
        userPreferencesRepository.preferences,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val range = values[0] as Pair<Long, Long>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<CategorySpendingItem>
        @Suppress("UNCHECKED_CAST")
        val cashFlow = values[2] as List<MonthlyCashFlowPoint>
        val comparison = values[3] as PeriodComparison
        @Suppress("UNCHECKED_CAST")
        val aging = values[4] as List<AgingBucketPerson>
        val netWorth = values[5] as NetWorthBreakdown
        @Suppress("UNCHECKED_CAST")
        val snapshots = values[6] as List<NetWorthSnapshotEntity>
        @Suppress("UNCHECKED_CAST")
        val selectedCat = values[7] as Pair<Boolean, Long?>
        @Suppress("UNCHECKED_CAST")
        val drilldown = values[8] as List<TransactionWithDetails>
        val prefs = values[9] as com.finledger.domain.model.UserPreferences

        AnalyticsUiState(
            startMs = range.first,
            endMs = range.second,
            categoryBreakdown = categories,
            cashFlowPoints = cashFlow,
            periodComparison = comparison,
            agingReport = aging,
            netWorth = netWorth,
            netWorthSnapshots = snapshots,
            hasSelectedCategory = selectedCat.first,
            selectedCategoryId = selectedCat.second,
            drilldownTransactions = drilldown,
            primaryCurrency = prefs.primaryCurrency,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setDateRange(startMs: Long, endMs: Long) {
        _dateRange.value = startMs to endMs
    }

    fun selectCategoryForDrilldown(categoryId: Long?) {
        val current = _selectedCategory.value
        _selectedCategory.value = if (current.first && current.second == categoryId) {
            false to null
        } else {
            true to categoryId
        }
    }

    fun recordNetWorthSnapshot(breakdown: NetWorthBreakdown) {
        viewModelScope.launch { getNetWorthUseCase.recordSnapshot(breakdown) }
    }
}

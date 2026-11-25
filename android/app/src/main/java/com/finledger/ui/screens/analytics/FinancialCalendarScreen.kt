package com.finledger.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CalendarActivityItem(
    val id: String,
    val targetId: Long,
    val kind: String, // "TX", "BILL", "LOAN", "EVENT"
    val dateMs: Long,
    val title: String,
    val typeLabel: String, // "Transaction", "Bill", "Loan", "Event"
    val amountPaise: Long?,
)

@HiltViewModel
class FinancialCalendarViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    commitmentRepository: CommitmentRepository,
    socialRepository: SocialRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val items: StateFlow<Pair<String, List<CalendarActivityItem>>> = combine(
        transactionRepository.observeAllTransactionsWithDetails(),
        commitmentRepository.observeActiveBills(),
        commitmentRepository.observeAllLoans(),
        socialRepository.observeAllEvents(),
        userPreferencesRepository.preferences,
    ) { txs, bills, loans, events, prefs ->
        val list = mutableListOf<CalendarActivityItem>()
        txs.forEach { d ->
            list.add(
                CalendarActivityItem(
                    id = "tx_${d.transaction.id}",
                    targetId = d.transaction.id,
                    kind = "TX",
                    dateMs = d.transaction.date,
                    title = d.transaction.merchantOrPayee.ifBlank { d.category?.name ?: d.transaction.type.displayName },
                    typeLabel = d.transaction.type.displayName,
                    amountPaise = d.transaction.amount,
                )
            )
        }
        bills.forEach { b ->
            list.add(CalendarActivityItem("bill_${b.id}", b.id, "BILL", b.nextDueDateMs, "Bill due: ${b.name}", "Bill", b.expectedAmount))
        }
        loans.forEach { l ->
            l.expectedRepaymentDate?.let { due ->
                list.add(CalendarActivityItem("loan_${l.id}", l.id, "LOAN", due, "Loan due: ${l.personOrOrg}", "Loan", l.principalAmount))
            }
        }
        events.forEach { ev ->
            list.add(CalendarActivityItem("ev_${ev.id}", ev.id, "EVENT", ev.startDate, "Event: ${ev.name}", "Event", null))
        }
        prefs.primaryCurrency to list.sortedByDescending { it.dateMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "INR" to emptyList())
}

@Composable
fun FinancialCalendarScreen(
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit = {},
    onOpenBills: () -> Unit = {},
    onOpenLoan: (Long) -> Unit = {},
    onOpenEvent: (Long) -> Unit = {},
    viewModel: FinancialCalendarViewModel = hiltViewModel(),
) {
    val (currency, allItems) = viewModel.items.collectAsStateWithLifecycle().value
    var agendaMode by remember { mutableStateOf(false) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = monthCal.get(Calendar.YEAR)
    val month = monthCal.get(Calendar.MONTH)
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = (monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)

    LaunchedEffect(daysInMonth) {
        selectedDay = selectedDay.coerceIn(1, daysInMonth)
    }

    val onItemClick: (CalendarActivityItem) -> Unit = { item ->
        when (item.kind) {
            "TX" -> onOpenTransaction(item.targetId)
            "BILL" -> onOpenBills()
            "LOAN" -> onOpenLoan(item.targetId)
            "EVENT" -> onOpenEvent(item.targetId)
        }
    }

    val dayMap = remember(allItems, year, month) {
        allItems.filter { item ->
            val c = Calendar.getInstance().apply { timeInMillis = item.dateMs }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        }.groupBy { item ->
            Calendar.getInstance().apply { timeInMillis = item.dateMs }.get(Calendar.DAY_OF_MONTH)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Financial Calendar", style = MaterialTheme.typography.headlineMedium) },
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
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FilterChip(selected = !agendaMode, onClick = { agendaMode = false }, label = { Text("Calendar") })
                FilterChip(selected = agendaMode, onClick = { agendaMode = true }, label = { Text("Agenda") })
            }

            if (agendaMode) {
                AgendaView(
                    items = allItems,
                    currency = currency,
                    onItemClick = onItemClick,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { monthOffset-- }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(DateFormatter.formatMonthYear(monthCal.timeInMillis), style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { monthOffset++ }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }

                // Weekday headers + offset 7-column day grid
                val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList()
                val rows = cells.chunked(7)
                Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = FinLedgerTheme.colors.inkMuted48,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    rows.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until 7) {
                                val day = week.getOrNull(i)
                                if (day == null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val hasEvents = dayMap.containsKey(day)
                                    val isSelected = selectedDay == day
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .clickable { selectedDay = day },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (hasEvents) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                CalendarDayDetail(
                    day = selectedDay,
                    items = dayMap[selectedDay].orEmpty(),
                    currency = currency,
                    onItemClick = onItemClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun CalendarDayDetail(
    day: Int,
    items: List<CalendarActivityItem>,
    currency: String,
    onItemClick: (CalendarActivityItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        item {
            Text(
                text = "Activity on Day $day",
                style = MaterialTheme.typography.titleSmall,
                color = FinLedgerTheme.colors.inkMuted48,
            )
        }
        if (items.isEmpty()) {
            item {
                Text("Nothing scheduled on this day.", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
            }
        } else {
            items(items, key = { it.id }) { item ->
                CalendarActivityCard(item, currency, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun AgendaView(
    items: List<CalendarActivityItem>,
    currency: String,
    onItemClick: (CalendarActivityItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(items, key = { it.id }) { item ->
            CalendarActivityCard(item, currency, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun CalendarActivityCard(
    item: CalendarActivityItem,
    currency: String,
    onClick: () -> Unit = {},
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${DateFormatter.formatDate(item.dateMs)} · ${item.typeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
            item.amountPaise?.let { amt ->
                Text(MoneyFormatter.format(amt, currency), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}


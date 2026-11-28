package com.finledger.ui.screens.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.usecase.AgingBucketPerson
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.screens.events.EventsContent
import com.finledger.ui.screens.groups.GroupsContent
import com.finledger.ui.screens.loans.LoansContent
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

enum class PeopleTab(val label: String) {
    FRIENDS("Friends"),
    GROUPS("Groups"),
    EVENTS("Events"),
    LOANS("Loans"),
}

fun visiblePeopleTabs(
    trackSharedAndPeople: Boolean,
    trackLoans: Boolean,
    isSimpleMode: Boolean = true,
): List<PeopleTab> = buildList {
    if (trackSharedAndPeople || !isSimpleMode) {
        add(PeopleTab.FRIENDS)
        add(PeopleTab.GROUPS)
        add(PeopleTab.EVENTS)
    }
    if (trackLoans || !isSimpleMode) {
        add(PeopleTab.LOANS)
    }
    if (isEmpty()) {
        add(PeopleTab.FRIENDS)
    }
}

@Composable
fun PeopleHubScreen(
    initialTab: Int = 0,
    trackSharedAndPeople: Boolean = true,
    trackLoans: Boolean = false,
    isSimpleMode: Boolean = true,
    onOpenPersonDetail: (Long) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenGroupDetail: (Long) -> Unit,
    onCreateEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onAddLoan: () -> Unit,
    onOpenLoanDetail: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    friendsViewModel: FriendsViewModel = hiltViewModel(),
) {
    val friendsState by friendsViewModel.uiState.collectAsStateWithLifecycle()
    val visibleTabs = remember(trackSharedAndPeople, trackLoans, isSimpleMode) {
        visiblePeopleTabs(
            trackSharedAndPeople = trackSharedAndPeople,
            trackLoans = trackLoans,
            isSimpleMode = isSimpleMode,
        )
    }
    var selectedTab by rememberSaveable(initialTab, visibleTabs.size) {
        mutableIntStateOf(initialTab.coerceIn(0, visibleTabs.lastIndex))
    }
    val safeIndex = selectedTab.coerceIn(0, visibleTabs.lastIndex)
    var showAddFriendSheet by remember { mutableStateOf(false) }
    var showAgingDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val activeTab = visibleTabs[safeIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("People", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { showAgingDialog = true }) {
                        val count = friendsState.agingReport.size
                        Text(if (count > 0) "Aging ($count)" else "Aging")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeTab) {
                        PeopleTab.FRIENDS -> showAddFriendSheet = true
                        PeopleTab.GROUPS -> onCreateGroup()
                        PeopleTab.EVENTS -> onCreateEvent()
                        PeopleTab.LOANS -> onAddLoan()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = when (activeTab) {
                        PeopleTab.FRIENDS -> "Add friend"
                        PeopleTab.GROUPS -> "Create group"
                        PeopleTab.EVENTS -> "Create event"
                        PeopleTab.LOANS -> "Add loan"
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
            if (visibleTabs.size > 1) {
                TabRow(selectedTabIndex = safeIndex, containerColor = MaterialTheme.colorScheme.surface) {
                    visibleTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = safeIndex == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.label, maxLines = 1) },
                        )
                    }
                }
            }

            when (activeTab) {
                PeopleTab.FRIENDS -> FriendsContent(
                    onOpenPersonDetail = onOpenPersonDetail,
                    showAddSheet = showAddFriendSheet,
                    onShowAddSheetChange = { showAddFriendSheet = it },
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                PeopleTab.GROUPS -> GroupsContent(
                    onCreateGroup = onCreateGroup,
                    onOpenGroupDetail = onOpenGroupDetail,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                PeopleTab.EVENTS -> EventsContent(
                    onCreateEvent = onCreateEvent,
                    onOpenEvent = onOpenEvent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                PeopleTab.LOANS -> LoansContent(
                    onAddLoan = onAddLoan,
                    onOpenLoanDetail = onOpenLoanDetail,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }

    if (showAgingDialog) {
        ReceivablesAgingDialog(
            items = friendsState.agingReport,
            currency = friendsState.primaryCurrency,
            onDismiss = { showAgingDialog = false },
            onOpenPerson = { pid ->
                showAgingDialog = false
                onOpenPersonDetail(pid)
            },
        )
    }
}

@Composable
private fun ReceivablesAgingDialog(
    items: List<AgingBucketPerson>,
    currency: String,
    onDismiss: () -> Unit,
    onOpenPerson: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receivables Aging") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "Shows how long money owed to you has been outstanding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
                if (items.isEmpty()) {
                    Text(
                        text = "All settled up — nobody owes you money.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinLedgerTheme.colors.inkMuted48,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        items(items, key = { it.person.id }) { row ->
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPerson(row.person.id) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.person.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = "${row.daysOutstanding} days (${row.bucketLabel})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FinLedgerTheme.colors.inkMuted48,
                                        )
                                    }
                                    Text(
                                        text = MoneyFormatter.format(row.outstandingPaise, currency),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = FinLedgerTheme.colors.positive,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}


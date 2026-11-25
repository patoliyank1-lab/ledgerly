package com.finledger.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.components.DatePickerField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditEventScreen(
    eventId: Long?,
    initialGroupId: Long?,
    onBack: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = remember(eventId, state.events) {
        eventId?.let { id -> state.events.firstOrNull { it.event.id == id }?.event }
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(initialGroupId?.takeIf { it > 0L }) }
    var startDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var budgetText by remember { mutableStateOf("") }
    var selectedParticipantIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var nameError by remember { mutableStateOf(false) }
    var populatedEventId by remember { mutableStateOf<Long?>(null) }
    var participantsEditedManually by remember { mutableStateOf(false) }

    LaunchedEffect(existing, state.eventParticipants) {
        if (existing != null && populatedEventId != existing.id) {
            name = existing.name
            description = existing.description
            location = existing.location
            selectedGroupId = existing.groupId
            startDateMs = existing.startDate
            budgetText = existing.budgetAmount?.let { MoneyFormatter.paiseToInputString(it) } ?: ""
            selectedParticipantIds = state.eventParticipants
                .filter { it.eventId == existing.id }
                .map { it.personId }
                .toSet()
            populatedEventId = existing.id
        }
    }

    LaunchedEffect(selectedGroupId, state.groupMembers, existing) {
        if (existing == null && !participantsEditedManually && selectedGroupId != null) {
            val groupMemberIds = state.groupMembers
                .filter { it.groupId == selectedGroupId }
                .map { it.personId }
                .toSet()
            if (groupMemberIds.isNotEmpty()) {
                selectedParticipantIds = groupMemberIds
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit Event" else "Create Event", style = MaterialTheme.typography.headlineMedium) },
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
                onValueChange = { name = it; nameError = false },
                label = { Text("Event Name") },
                placeholder = { Text("e.g., Weekend Dinner, Manali Trip") },
                isError = nameError,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            DatePickerField(dateMs = startDateMs, onDateSelected = { startDateMs = it }, label = "Event Date")
            AmountTextField(
                value = budgetText,
                onValueChange = { budgetText = it },
                label = "Event Budget (Optional)",
                currency = state.primaryCurrency,
            )

            if (state.groups.isNotEmpty()) {
                Text("Link to Group (Optional)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FilterChip(
                        selected = selectedGroupId == null,
                        onClick = { selectedGroupId = null },
                        label = { Text("Standalone") },
                    )
                    state.groups.forEach { g ->
                        FilterChip(
                            selected = selectedGroupId == g.id,
                            onClick = { selectedGroupId = g.id },
                            label = { Text(g.name) },
                        )
                    }
                }
            }

            Text("Participants (You are included automatically)", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                state.persons.forEach { person ->
                    val selected = person.id in selectedParticipantIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            participantsEditedManually = true
                            selectedParticipantIds = if (selected) selectedParticipantIds - person.id else selectedParticipantIds + person.id
                        },
                        label = { Text(person.name) },
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    viewModel.saveEvent(
                        existing = existing,
                        name = name,
                        description = description,
                        location = location,
                        groupId = selectedGroupId,
                        startDateMs = startDateMs,
                        budgetPaise = MoneyFormatter.parseToPaise(budgetText),
                        participantIds = selectedParticipantIds.toList(),
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Event")
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


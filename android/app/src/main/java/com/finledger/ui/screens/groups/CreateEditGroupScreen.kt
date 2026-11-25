package com.finledger.ui.screens.groups

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditGroupScreen(
    groupId: Long?,
    onBack: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existingGroup = groupId?.takeIf { it > 0L }?.let { id ->
        state.groups.firstOrNull { it.group.id == id }
    }

    var initialized by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var nameError by remember { mutableStateOf(false) }

    LaunchedEffect(existingGroup) {
        if (existingGroup != null && !initialized) {
            name = existingGroup.group.name
            description = existingGroup.group.description
            limitText = existingGroup.group.spendingLimit?.let { MoneyFormatter.paiseToInputString(it) } ?: ""
            selectedMemberIds = existingGroup.memberIds.toSet()
            initialized = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existingGroup == null) "Create Group" else "Edit Group", style = MaterialTheme.typography.headlineMedium) },
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
                label = { Text("Group Name") },
                placeholder = { Text("e.g., Goa Trip, Flatmates") },
                isError = nameError,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            AmountTextField(
                value = limitText,
                onValueChange = { limitText = it },
                label = "Spending Limit (Optional)",
                currency = state.primaryCurrency,
            )

            Text("Select Members (You are included automatically)", style = MaterialTheme.typography.titleSmall)
            if (state.persons.isEmpty()) {
                Text(
                    text = "Add friends in the Friends tab first to include them in groups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    state.persons.forEach { person ->
                        val selected = person.id in selectedMemberIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedMemberIds = if (selected) selectedMemberIds - person.id else selectedMemberIds + person.id
                            },
                            label = { Text(person.name) },
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    viewModel.saveGroup(
                        existing = existingGroup?.group,
                        name = name,
                        description = description,
                        currency = state.primaryCurrency,
                        spendingLimitPaise = MoneyFormatter.parseToPaise(limitText),
                        memberPersonIds = selectedMemberIds.toList(),
                        onSuccess = onBack,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Group")
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


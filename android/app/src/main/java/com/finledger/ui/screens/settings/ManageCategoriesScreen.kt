package com.finledger.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finledger.data.local.entity.CategoryType
import com.finledger.ui.components.IconMapper
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.headlineMedium) },
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
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add category")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            items(state.categories, key = { it.id }) { cat ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = IconMapper.categoryIcon(cat.iconName),
                                contentDescription = cat.name,
                                tint = IconMapper.parseColorSafe(cat.colorHex, MaterialTheme.colorScheme.primary),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${cat.type.name}${if (cat.isDefault) " · Default" else " · Custom"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FinLedgerTheme.colors.inkMuted48,
                                )
                            }
                        }
                        if (!cat.isDefault) {
                            IconButton(onClick = { viewModel.deleteCustomCategory(cat.id) }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete category",
                                    tint = FinLedgerTheme.colors.negative,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf(false) }
        var type by remember { mutableStateOf(CategoryType.EXPENSE) }
        var selectedIcon by remember { mutableStateOf("category") }
        var selectedColor by remember { mutableStateOf("#0066CC") }
        val iconOptions = remember {
            listOf(
                "category", "restaurant", "shopping_cart", "directions_car", "shopping_bag",
                "receipt", "home", "medical_services", "movie", "flight", "school",
                "spa", "work", "laptop", "storefront", "savings", "card_giftcard", "payments",
            )
        }
        val colorOptions = remember {
            listOf("#0066CC", "#1E7E34", "#6A1B9A", "#B26A00", "#C62828", "#00838F", "#283593", "#AD1457")
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = false
                        },
                        label = { Text("Category Name") },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Category name is required") }
                        } else null,
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        listOf(CategoryType.EXPENSE, CategoryType.INCOME).forEach { entry ->
                            FilterChip(
                                selected = type == entry,
                                onClick = { type = entry },
                                label = { Text(entry.name) },
                            )
                        }
                    }
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        val activeColor = IconMapper.parseColorSafe(selectedColor)
                        iconOptions.forEach { key ->
                            val isSelected = selectedIcon == key
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    )
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = IconMapper.categoryIcon(key),
                                    contentDescription = key,
                                    tint = if (isSelected) activeColor else FinLedgerTheme.colors.inkMuted48,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        colorOptions.forEach { hex ->
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(IconMapper.parseColorSafe(hex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else IconMapper.parseColorSafe(hex),
                                        shape = CircleShape,
                                    )
                                    .clickable { selectedColor = hex },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        viewModel.addCategory(name.trim(), type, selectedIcon, selectedColor)
                        showAddDialog = false
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
        )
    }
}


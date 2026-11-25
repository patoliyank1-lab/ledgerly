package com.finledger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

/**
 * Pill-shaped category option grid styled after DESIGN.md configurator-option-chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onSelectCategory: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall,
            color = FinLedgerTheme.colors.inkMuted48,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            categories.forEach { category ->
                val isSelected = category.id == selectedCategoryId
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.clickable { onSelectCategory(category) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = IconMapper.categoryIcon(category.iconName),
                            contentDescription = category.name,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else IconMapper.parseColorSafe(category.colorHex),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}


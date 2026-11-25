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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing

/**
 * Result of ordering categories by preferred IDs and optionally slicing to a compact limit.
 */
data class CategoryPickerSlice(
    val visibleCategories: List<CategoryEntity>,
    val hasMore: Boolean,
)

object CategoryPickerHelper {
    fun orderAndSlice(
        categories: List<CategoryEntity>,
        preferredOrderIds: List<Long> = emptyList(),
        compactLimit: Int? = null,
        showAll: Boolean = false,
    ): CategoryPickerSlice {
        val ordered = if (preferredOrderIds.isEmpty()) {
            categories
        } else {
            val rank = preferredOrderIds.withIndex().associate { (index, id) -> id to index }
            val (preferred, rest) = categories.partition { it.id in rank }
            preferred.sortedBy { rank[it.id] ?: Int.MAX_VALUE } + rest
        }

        val canCompact = compactLimit != null && compactLimit > 0 && ordered.size > compactLimit
        val visible = if (canCompact && !showAll) ordered.take(compactLimit) else ordered
        return CategoryPickerSlice(
            visibleCategories = visible,
            hasMore = canCompact,
        )
    }
}

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
    preferredOrderIds: List<Long> = emptyList(),
    compactLimit: Int? = null,
) {
    var showAll by rememberSaveable(compactLimit) { mutableStateOf(false) }
    val slice = CategoryPickerHelper.orderAndSlice(
        categories = categories,
        preferredOrderIds = preferredOrderIds,
        compactLimit = compactLimit,
        showAll = showAll,
    )

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
            slice.visibleCategories.forEach { category ->
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

            if (slice.hasMore) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { showAll = !showAll },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (showAll) "Show less" else "More",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showAll) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (showAll) "Show fewer categories" else "Show more categories",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}


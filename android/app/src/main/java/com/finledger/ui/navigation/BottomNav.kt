package com.finledger.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finledger.domain.model.UserPreferences
import com.finledger.ui.theme.FinLedgerTheme

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isQuickAddAction: Boolean = false,
)

val PrimaryBottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Dashboard.route,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    BottomNavItem(
        route = Screen.Transactions.route,
        label = "Activity",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
    ),
    BottomNavItem(
        route = Screen.QuickAdd.createRoute("EXPENSE"),
        label = "Add",
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Filled.Add,
        isQuickAddAction = true,
    ),
    BottomNavItem(
        route = Screen.Plan.route,
        label = "Plan",
        selectedIcon = Icons.Filled.PieChart,
        unselectedIcon = Icons.Outlined.PieChart,
    ),
    BottomNavItem(
        route = Screen.People.route,
        label = "People",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
    ),
)

fun visibleBottomNavItems(prefs: UserPreferences): List<BottomNavItem> {
    val showPlan = prefs.trackPlanning || !prefs.isSimpleMode
    val showPeople = prefs.trackSharedAndPeople || prefs.trackLoans || !prefs.isSimpleMode
    return PrimaryBottomNavItems.filter { item ->
        when (item.route) {
            Screen.Plan.route -> showPlan
            Screen.People.route -> showPeople
            else -> true
        }
    }
}

@Composable
fun FinLedgerBottomBar(
    currentRoute: String?,
    onSelectRoute: (String) -> Unit,
    items: List<BottomNavItem> = PrimaryBottomNavItems,
) {
    Column {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            items.forEach { item ->
                val selected = !item.isQuickAddAction && currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectRoute(item.route) },
                    icon = {
                        if (item.isQuickAddAction) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Quick Add",
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.isQuickAddAction) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Unspecified,
                            maxLines = 1,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = FinLedgerTheme.colors.inkMuted48,
                        unselectedTextColor = FinLedgerTheme.colors.inkMuted48,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
                )
            }
        }
    }
}


package com.novacorp.inmonode_app.features.main

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.novacorp.inmonode_app.R

@Composable
fun MainNavigationBar(
    navController: NavHostController,
    conflicts: Int = 0,
    modifier: Modifier = Modifier
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(modifier = modifier.navigationBarsPadding().height(64.dp), windowInsets = WindowInsets(0, 0, 0, 0)) {
        NavigationItem.entries.forEach { item ->
            val selected = (item == NavigationItem.PROSPECTS && currentDestination?.hasRoute<com.novacorp.inmonode_app.features.vouchers.presentation.navigation.SeparationListRoute>() == true) || currentDestination?.hierarchy?.any { destination ->
                destination.hasRoute(item.route::class)
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    // Unresolved lot conflicts (US-12) show on the Sync tab from any screen.
                    val count = if (item == NavigationItem.SYNC) conflicts else 0
                    val badgeDescription = if (count > 0) pluralStringResource(R.plurals.nav_conflicts_badge, count, count) else null
                    BadgedBox(badge = {
                        if (count > 0) Badge(Modifier.semantics { contentDescription = badgeDescription.orEmpty() }) { Text(count.toString()) }
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (selected) item.selectedIcon else item.icon),
                            contentDescription = null
                        )
                    }
                },
                label = { Text(text = stringResource(item.label), fontWeight = if (selected) FontWeight.SemiBold else null) },
                // Material 3 draws the active label in secondary (light green), unreadable on the light bar.
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

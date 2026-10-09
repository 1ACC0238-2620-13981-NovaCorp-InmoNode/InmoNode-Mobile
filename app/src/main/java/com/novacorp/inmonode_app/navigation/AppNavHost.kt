package com.novacorp.inmonode_app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.novacorp.inmonode_app.features.main.MainNavGraphRoute
import com.novacorp.inmonode_app.features.main.mainNavGraph

/**
 * Root navigation. Splash (US-02) and auth (US-01) graphs are added by their feature branches,
 * which also move the start destination to the splash screen.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MainNavGraphRoute,
        modifier = modifier
    ) {
        mainNavGraph()
    }
}

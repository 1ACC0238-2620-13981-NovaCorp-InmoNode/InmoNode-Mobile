package com.novacorp.inmonode_app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.novacorp.inmonode_app.features.iam.presentation.navigation.AuthNavGraphRoute
import com.novacorp.inmonode_app.features.iam.presentation.navigation.authNavGraph
import com.novacorp.inmonode_app.features.iam.presentation.session.SessionViewModel
import com.novacorp.inmonode_app.features.main.MainNavGraphRoute
import com.novacorp.inmonode_app.features.main.mainNavGraph
import com.novacorp.inmonode_app.features.splash.presentation.navigation.SplashNavGraphRoute
import com.novacorp.inmonode_app.features.splash.presentation.navigation.splashNavGraph

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val isSignedIn by sessionViewModel.isSignedIn.collectAsStateWithLifecycle()

    // Sign out or expired session while inside the app: back to the login screen.
    LaunchedEffect(isSignedIn) {
        val isInMain = navController.currentBackStackEntry?.destination?.hierarchy?.any { destination ->
            destination.hasRoute(MainNavGraphRoute::class)
        } == true
        if (isSignedIn == false && isInMain) {
            navController.navigate(AuthNavGraphRoute) {
                popUpTo(MainNavGraphRoute) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = SplashNavGraphRoute,
        modifier = modifier
    ) {
        splashNavGraph(
            onSessionFound = {
                navController.navigate(MainNavGraphRoute) {
                    popUpTo(SplashNavGraphRoute) { inclusive = true }
                }
            },
            onNoSession = {
                navController.navigate(AuthNavGraphRoute) {
                    popUpTo(SplashNavGraphRoute) { inclusive = true }
                }
            }
        )
        authNavGraph(
            onSignedIn = {
                navController.navigate(MainNavGraphRoute) {
                    popUpTo(AuthNavGraphRoute) { inclusive = true }
                }
            }
        )
        mainNavGraph()
    }
}

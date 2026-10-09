package com.novacorp.inmonode_app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.novacorp.inmonode_app.features.iam.presentation.navigation.AuthNavGraphRoute
import com.novacorp.inmonode_app.features.iam.presentation.navigation.authNavGraph
import com.novacorp.inmonode_app.features.main.MainNavGraphRoute
import com.novacorp.inmonode_app.features.main.mainNavGraph

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AuthNavGraphRoute,
        modifier = modifier
    ) {
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

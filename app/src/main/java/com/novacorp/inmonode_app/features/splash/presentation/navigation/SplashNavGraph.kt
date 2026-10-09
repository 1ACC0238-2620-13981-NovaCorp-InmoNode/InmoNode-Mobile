package com.novacorp.inmonode_app.features.splash.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.novacorp.inmonode_app.features.splash.presentation.SplashScreen
import kotlinx.serialization.Serializable

@Serializable
data object SplashNavGraphRoute

@Serializable
data object SplashRoute

fun NavGraphBuilder.splashNavGraph(
    onSessionFound: () -> Unit,
    onNoSession: () -> Unit
) {
    navigation<SplashNavGraphRoute>(startDestination = SplashRoute) {
        composable<SplashRoute> {
            SplashScreen(onSessionFound = onSessionFound, onNoSession = onNoSession)
        }
    }
}

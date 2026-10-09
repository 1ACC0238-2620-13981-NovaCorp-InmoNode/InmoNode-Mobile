package com.novacorp.inmonode_app.features.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object MainNavGraphRoute

@Serializable
data object MainRoute

fun NavGraphBuilder.mainNavGraph() {
    navigation<MainNavGraphRoute>(startDestination = MainRoute) {
        composable<MainRoute> {
            MainScreen()
        }
    }
}

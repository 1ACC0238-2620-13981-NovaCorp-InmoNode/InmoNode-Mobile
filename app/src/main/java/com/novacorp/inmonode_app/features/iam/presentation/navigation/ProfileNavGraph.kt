package com.novacorp.inmonode_app.features.iam.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.novacorp.inmonode_app.features.iam.presentation.profile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileNavGraphRoute

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileNavGraph(onPortfolio: () -> Unit = {}) {
    navigation<ProfileNavGraphRoute>(startDestination = ProfileRoute) {
        composable<ProfileRoute> {
            ProfileScreen(onPortfolio = onPortfolio)
        }
    }
}

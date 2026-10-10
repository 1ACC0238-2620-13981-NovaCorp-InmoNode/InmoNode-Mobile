package com.novacorp.inmonode_app.features.iam.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.novacorp.inmonode_app.features.iam.presentation.login.LoginScreen
import kotlinx.serialization.Serializable

@Serializable
data object AuthNavGraphRoute

@Serializable
data object LoginRoute

fun NavGraphBuilder.authNavGraph(onSignedIn: () -> Unit) {
    navigation<AuthNavGraphRoute>(startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(onLoginSuccess = onSignedIn)
        }
    }
}

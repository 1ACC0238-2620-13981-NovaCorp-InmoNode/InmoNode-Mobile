package com.novacorp.inmonode_app.features.iam.presentation.navigation

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.ComingSoonScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileNavGraphRoute

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileNavGraph() {
    navigation<ProfileNavGraphRoute>(startDestination = ProfileRoute) {
        composable<ProfileRoute> {
            ComingSoonScreen(title = stringResource(R.string.profile_title), storyId = "US-01")
        }
    }
}

package com.novacorp.inmonode_app.features.fieldsales.presentation.navigation

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.ComingSoonScreen
import kotlinx.serialization.Serializable

@Serializable
data object MapNavGraphRoute

@Serializable
data object CadastralMapRoute

@Serializable
data object ProspectsNavGraphRoute

@Serializable
data object ProspectListRoute

@Serializable
data object SyncNavGraphRoute

@Serializable
data object SyncQueueRoute

fun NavGraphBuilder.mapNavGraph() {
    navigation<MapNavGraphRoute>(startDestination = CadastralMapRoute) {
        composable<CadastralMapRoute> {
            ComingSoonScreen(title = stringResource(R.string.cadastral_map_title), storyId = "US-05")
        }
    }
}

fun NavGraphBuilder.prospectsNavGraph() {
    navigation<ProspectsNavGraphRoute>(startDestination = ProspectListRoute) {
        composable<ProspectListRoute> {
            ComingSoonScreen(title = stringResource(R.string.prospects_title), storyId = "US-04")
        }
    }
}

fun NavGraphBuilder.syncNavGraph() {
    navigation<SyncNavGraphRoute>(startDestination = SyncQueueRoute) {
        composable<SyncQueueRoute> {
            ComingSoonScreen(title = stringResource(R.string.sync_queue_title), storyId = "US-11")
        }
    }
}

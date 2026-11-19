package com.novacorp.inmonode_app.features.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.*
import com.novacorp.inmonode_app.features.vouchers.presentation.navigation.*
import com.novacorp.inmonode_app.features.iam.presentation.navigation.ProfileRoute
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.mapNavGraph
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.prospectsNavGraph
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.syncNavGraph
import com.novacorp.inmonode_app.features.iam.presentation.navigation.profileNavGraph

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = hiltViewModel()) {
    val mainNavController = rememberNavController()
    val conflicts by viewModel.unresolvedConflicts.collectAsStateWithLifecycle()

    val entry by mainNavController.currentBackStackEntryAsState()
    val bottomVisible = entry?.destination?.let { it.hasRoute<CadastralMapRoute>() || it.hasRoute<ProspectListRoute>() || it.hasRoute<SyncQueueRoute>() || it.hasRoute<ProfileRoute>() || it.hasRoute<SeparationListRoute>() } ?: true

    Scaffold(
        modifier = modifier,
        bottomBar = { if (bottomVisible) MainNavigationBar(navController = mainNavController, conflicts = conflicts) }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = NavigationItem.entries.first().route,
            modifier = Modifier.padding(innerPadding)
        ) {
            mapNavGraph(mainNavController)
            prospectsNavGraph(mainNavController)
            syncNavGraph(mainNavController)
            profileNavGraph(onPortfolio = { mainNavController.navigate(PortfolioDownloadRoute) })
            vouchersNavGraph(mainNavController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    InmoNodeAppTheme {
        MainScreen()
    }
}

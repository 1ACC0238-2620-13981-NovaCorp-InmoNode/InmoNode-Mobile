package com.novacorp.inmonode_app.features.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
fun MainScreen(modifier: Modifier = Modifier) {
    val mainNavController = rememberNavController()

    Scaffold(
        modifier = modifier,
        bottomBar = { MainNavigationBar(navController = mainNavController) }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = NavigationItem.entries.first().route,
            modifier = Modifier.padding(innerPadding)
        ) {
            mapNavGraph()
            prospectsNavGraph()
            syncNavGraph()
            profileNavGraph()
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

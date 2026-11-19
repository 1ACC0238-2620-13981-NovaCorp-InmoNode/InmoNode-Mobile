package com.novacorp.inmonode_app.features.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RequestConflictNotifications()

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

/** Lot conflicts found by the background sync are announced with a notification (US-12); Android 13+ asks first. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RequestConflictNotifications() {
    val context = LocalContext.current
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        // Once per launch; after two refusals Android stops showing the dialog by itself.
        if (!asked && !granted) {
            asked = true
            request.launch(Manifest.permission.POST_NOTIFICATIONS)
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

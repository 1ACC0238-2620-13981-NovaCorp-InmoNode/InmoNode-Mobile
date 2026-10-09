package com.novacorp.inmonode_app.features.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.MapNavGraphRoute
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.ProspectsNavGraphRoute
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.SyncNavGraphRoute
import com.novacorp.inmonode_app.features.iam.presentation.navigation.ProfileNavGraphRoute
import kotlinx.serialization.Serializable

enum class NavigationItem(
    val route: @Serializable Any,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int
) {
    MAP(
        route = MapNavGraphRoute,
        label = R.string.nav_map,
        icon = R.drawable.ic_map,
        selectedIcon = R.drawable.ic_map_filled
    ),
    PROSPECTS(
        route = ProspectsNavGraphRoute,
        label = R.string.nav_prospects,
        icon = R.drawable.ic_group,
        selectedIcon = R.drawable.ic_group_filled
    ),
    SYNC(
        route = SyncNavGraphRoute,
        label = R.string.nav_sync,
        icon = R.drawable.ic_sync,
        selectedIcon = R.drawable.ic_sync
    ),
    PROFILE(
        route = ProfileNavGraphRoute,
        label = R.string.nav_profile,
        icon = R.drawable.ic_person,
        selectedIcon = R.drawable.ic_person_filled
    )
}

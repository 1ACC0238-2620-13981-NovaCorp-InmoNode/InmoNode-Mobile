package com.novacorp.inmonode_app.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.novacorp.inmonode_app.R

/** Material Symbols Rounded icons (res/drawable/ic_*.xml), tinted by the caller. */
object InmoIcons {
    val Map: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_map)
    val MapFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_map_filled)
    val Group: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_group)
    val GroupFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_group_filled)
    val Sync: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_sync)
    val Person: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_person)
    val PersonFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_person_filled)
    val Construction: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_construction)
}

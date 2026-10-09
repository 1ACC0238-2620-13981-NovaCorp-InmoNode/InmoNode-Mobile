package com.novacorp.inmonode_app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = White,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = OnForestGreenContainer,
    secondary = LightGreen,
    onSecondary = PureBlack,
    secondaryContainer = LightGreenContainer,
    onSecondaryContainer = PureBlack,
    tertiary = AlertYellow,
    onTertiary = PureBlack,
    tertiaryContainer = AlertYellowContainer,
    onTertiaryContainer = PureBlack,
    error = TerracottaOrange,
    onError = White,
    errorContainer = TerracottaContainer,
    onErrorContainer = OnTerracottaContainer,
    background = OffWhite,
    onBackground = PureBlack,
    surface = White,
    onSurface = PureBlack,
    surfaceVariant = NeutralVariant,
    onSurfaceVariant = OnNeutralVariant,
    surfaceTint = ForestGreen,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceBright = White,
    surfaceDim = SurfaceContainerHighestLight,
    surfaceContainerLowest = White,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = PureBlack,
    primaryContainer = ForestGreenContainerDark,
    onPrimaryContainer = ForestGreenContainer,
    secondary = ForestGreen,
    onSecondary = White,
    secondaryContainer = ForestGreenContainerDark,
    onSecondaryContainer = LightGreenContainer,
    tertiary = AlertYellow,
    onTertiary = PureBlack,
    tertiaryContainer = AlertYellowContainerDark,
    onTertiaryContainer = AlertYellowContainer,
    error = TerracottaOrange,
    onError = White,
    errorContainer = TerracottaContainerDark,
    onErrorContainer = TerracottaContainer,
    background = BackgroundDark,
    onBackground = OffWhite,
    surface = SurfaceDark,
    onSurface = OffWhite,
    surfaceVariant = OnNeutralVariant,
    onSurfaceVariant = OutlineVariant,
    surfaceTint = LightGreen,
    outline = Outline,
    outlineVariant = OnNeutralVariant,
    surfaceBright = SurfaceContainerHighestDark,
    surfaceDim = BackgroundDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

@Composable
fun InmoNodeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand colors are fixed; dynamic color (Android 12+) stays opt-in
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

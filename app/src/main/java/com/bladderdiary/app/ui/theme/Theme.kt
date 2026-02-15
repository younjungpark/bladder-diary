package com.bladderdiary.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SlateBlue,
    onPrimary = AppSurface,
    primaryContainer = SlateBlueSoft,
    onPrimaryContainer = NavyInk,
    secondary = Steel,
    onSecondary = AppSurface,
    secondaryContainer = SteelSoft,
    onSecondaryContainer = NavyInk,
    tertiary = TealAccent,
    background = AppBackground,
    onBackground = NavyInk,
    surface = AppSurface,
    onSurface = NavyInk,
    surfaceVariant = AppSurfaceVariant,
    outline = AppOutline
)

private val DarkColors = darkColorScheme(
    primary = SlateBlueDark,
    onPrimary = NavyDark,
    primaryContainer = NavyInk,
    onPrimaryContainer = SlateBlueSoft,
    secondary = Color(0xFFB7C2D9),
    onSecondary = Color(0xFF1E2A40),
    secondaryContainer = AppSurfaceVariantDark,
    onSecondaryContainer = Color(0xFFD7DEEA),
    tertiary = Color(0xFF8FD3E4),
    background = AppBackgroundDark,
    onBackground = Color(0xFFE1E8F4),
    surface = AppSurfaceDark,
    onSurface = Color(0xFFE1E8F4),
    surfaceVariant = AppSurfaceVariantDark,
    outline = AppOutlineDark
)

@Composable
fun BladderDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

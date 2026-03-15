package com.bladderdiary.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppExtraColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val securityContainer: Color,
    val onSecurityContainer: Color
)

internal val LocalAppExtraColors = staticCompositionLocalOf {
    AppExtraColors(
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified,
        warningContainer = Color.Unspecified,
        onWarningContainer = Color.Unspecified,
        securityContainer = Color.Unspecified,
        onSecurityContainer = Color.Unspecified
    )
}

val MaterialTheme.appExtraColors: AppExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppExtraColors.current

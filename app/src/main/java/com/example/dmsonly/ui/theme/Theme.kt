package com.example.dmsonly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun DMsOnlyTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    val view = LocalView.current

    SideEffect {
        val activity = view.context as? Activity
        if (activity != null) {
            activity.window.statusBarColor = colors.surface.toArgb()
            activity.window.navigationBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

package com.example.dmsonly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
@Suppress("DEPRECATION")
fun DMsOnlyTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF9B8CFF),
            onPrimary = Color(0xFF171326),
            secondary = Color(0xFFBDB3FF),
            background = Color(0xFF0B0A10),
            surface = Color(0xFF14121C),
            onSurface = Color(0xFFF4F1FF),
            onSurfaceVariant = Color(0xFFB8B3C8)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF5D4AE8),
            onPrimary = Color.White,
            secondary = Color(0xFF6C5CE7),
            background = Color(0xFFF8F7FC),
            surface = Color.White,
            onSurface = Color(0xFF1B1825),
            onSurfaceVariant = Color(0xFF625D70)
        )
    }
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

package com.acilnakit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fluent Design Colors - Premium Palette
val FluentBlue = Color(0xFF0078D4)
val FluentBlueLight = Color(0xFF2B88D8)

// Dark Theme Colors (Deep & Rich)
val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF171717)
val DarkSurfaceVariant = Color(0xFF262626)
val OnDarkBackground = Color(0xFFF5F5F5)

// Light Theme Colors (Clean & Airy)
val LightBackground = Color(0xFFF9F9F9)
val LightSurface = Color(0xFFFFFFFF)
val OnLightBackground = Color(0xFF1A1A1A)

private val DarkColorScheme = darkColorScheme(
    primary = FluentBlue,
    secondary = FluentBlueLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = OnDarkBackground,
    onSurface = OnDarkBackground,
    onSurfaceVariant = Color.Gray
)

private val LightColorScheme = lightColorScheme(
    primary = FluentBlue,
    secondary = FluentBlueLight,
    background = LightBackground,
    surface = LightSurface,
    onBackground = OnLightBackground,
    onSurface = OnLightBackground,
    onSurfaceVariant = Color.Gray
)

@Composable
fun AcilNakitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

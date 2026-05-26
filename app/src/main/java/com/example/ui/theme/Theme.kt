package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WaterPrimary,
    secondary = WaterSecondary,
    tertiary = WaterTertiary,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.DarkGray,
    onBackground = Color(0xFFF1F1F5),
    onSurface = Color(0xFFF1F1F5),
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = Color(0xFFC5C5D2)
)

private val LightColorScheme = lightColorScheme(
    primary = WaterPrimary,
    secondary = WaterSecondary,
    tertiary = WaterTertiary,
    background = WaterBgLight,
    surface = WaterSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E1E24),
    onSurface = Color(0xFF1E1E24),
    surfaceVariant = Color(0xFFEDEEF5),
    onSurfaceVariant = Color(0xFF5A5B69)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We force our beautiful, highly customized corporate Tam Tam orange color scheme
    // instead of generic dynamic Android colors to maximize replica accuracy and design depth.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF356859),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E8DC),
    onPrimaryContainer = Color(0xFF0C2118),
    secondary = Color(0xFF5A7066),
    onSecondary = Color.White,
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF17201C),
    surface = Color.White,
    onSurface = Color(0xFF17201C),
    surfaceVariant = Color(0xFFE7EFEB),
    onSurfaceVariant = Color(0xFF3F4A45),
    outline = Color(0xFF6F7C75)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC5C6CA),
    onPrimary = Color(0xFF2C2D32),
    primaryContainer = Color(0xFF3E3F45),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFC1C6C6),
    onSecondary = Color(0xFF2C2D32),
    secondaryContainer = Color(0xFF3E3F45),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF323339),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF2C2D32),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF3E3F45),
    onSurfaceVariant = Color(0xFFC1C6C6),
    outline = Color(0xFF3E3F45),
    outlineVariant = Color(0xFF3E3F45)
)

// OLED-friendly palette: pure black backgrounds let AMOLED pixels stay off.
internal val OledDarkColorScheme = darkColorScheme(
    primary = OledTextPrimary,
    onPrimary = Color.Black,
    primaryContainer = OledTextPrimary,
    onPrimaryContainer = Color.Black,
    secondary = OledTextPrimary,
    onSecondary = Color.Black,
    secondaryContainer = OledBgPrimary,
    onSecondaryContainer = OledTextPrimary,
    background = OledBgSecondary,
    onBackground = OledTextPrimary,
    surface = OledBgTertiary,
    onSurface = OledTextPrimary,
    surfaceVariant = OledBgTertiary,
    onSurfaceVariant = OledTextSecondary,
    outline = OledBorder,
    outlineVariant = OledBorder
)

// Original palette retained as an explicit compatibility theme.
private val OriginalMagooshLightColorScheme = lightColorScheme(
    primary = MagooshPurple,
    secondary = MagooshPurpleDark,
    background = MagooshPurpleBg,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF666666)
)

private val OriginalMagooshDarkColorScheme = darkColorScheme(
    primary = MagooshPurple,
    secondary = MagooshPurpleDark,
    background = MagooshPurpleBg,
    surface = Color(0xFF2A2A2A),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCCCCCC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    magooshTheme: Boolean = false,
    oledTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = when {
            oledTheme -> OledDarkColorScheme
            magooshTheme && darkTheme -> OriginalMagooshDarkColorScheme
            magooshTheme -> OriginalMagooshLightColorScheme
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        },
        content = content
    )
}

/** The theme actually active in composition, for theme-specific styling. */
internal enum class AppThemeMode { LIGHT, DARK, OLED }

@Composable
internal fun currentThemeMode(): AppThemeMode = when (MaterialTheme.colorScheme) {
    OledDarkColorScheme -> AppThemeMode.OLED
    DarkColorScheme -> AppThemeMode.DARK
    else -> AppThemeMode.LIGHT
}

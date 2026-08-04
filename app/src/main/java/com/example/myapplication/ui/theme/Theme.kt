package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Magoosh-style purple palette
val MagooshPurple = Color(0xFF6B3FA0)
val MagooshPurpleDark = Color(0xFF4A2A73)
val MagooshGreen = Color(0xFF3DA05E)
val MagooshGreenLight = Color(0xFFE6F4EA)
val MagooshPink = Color(0xFFE57373)
val MagooshPinkLight = Color(0xFFFDECEA)
val MagooshAmber = Color(0xFFF0A030)
val MagooshAmberLight = Color(0xFFFFF3E0)
val MagooshBlue = Color(0xFF42A5F5)
val MagooshBlueLight = Color(0xFFE3F2FD)

private val LightColorScheme = lightColorScheme(
    primary = MagooshPurple,
    secondary = MagooshPurpleDark,
    background = MagooshPurple,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = MagooshPurple,
    secondary = MagooshPurpleDark,
    background = MagooshPurpleDark,
    surface = Color(0xFF2A2A2A),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}

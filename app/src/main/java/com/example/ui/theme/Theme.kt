package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinDarkModeColors = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = Color.Black,
    tertiary = MintTertiary,
    onTertiary = HighContrastText,
    background = DarkBackground,
    onBackground = HighContrastText,
    surface = DarkSurface,
    onSurface = HighContrastText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = HighContrastText,
    error = AccentExpense
)

private val FinLightModeColors = lightColorScheme(
    primary = MintTertiary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    tertiary = MintPrimary,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    error = Color(0xFFDC2626)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted luxury theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) FinDarkModeColors else FinLightModeColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

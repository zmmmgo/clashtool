package com.example.clashtool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClashColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.Black,
    primaryContainer = GoldSecondary,
    onPrimaryContainer = Color.White,
    secondary = ElixirPink,
    onSecondary = Color.White,
    tertiary = DarkElixirPurple,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun ClashToolTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClashColorScheme,
        typography = Typography,
        content = content
    )
}

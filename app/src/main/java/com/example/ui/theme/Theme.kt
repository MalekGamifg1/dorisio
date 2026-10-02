package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DorisioLightColorScheme = lightColorScheme(
    primary = ModernEmerald,
    onPrimary = Color.White,
    primaryContainer = ModernEmeraldLight,
    onPrimaryContainer = ModernEmeraldDark,
    secondary = LuxuryGold,
    onSecondary = Color.White,
    secondaryContainer = LuxuryGoldLight,
    onSecondaryContainer = LuxuryGold,
    tertiary = LuxuryGoldBright,
    onTertiary = Color.White,
    background = CleanWhiteBg,
    onBackground = TextDark,
    surface = CleanWhiteSurface,
    onSurface = TextDark,
    surfaceVariant = CleanWhiteCardElevated,
    onSurfaceVariant = TextSecondarySlate,
    outline = CleanWhiteBorder,
    error = CardRed,
    onError = Color.White
)

@Composable
fun DorisioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DorisioLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DorisioTheme(content = content)
}

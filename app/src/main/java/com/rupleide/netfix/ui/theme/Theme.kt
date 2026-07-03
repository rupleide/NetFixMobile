package com.rupleide.netfix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3b82f6),
    secondary = Color(0xFF7c6af7),
    background = Color(0xFF161616),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color(0xFFF4F4F5),
    onSecondary = Color(0xFFF4F4F5),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFFA1A1AA)
)

@Composable
fun NetFixMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
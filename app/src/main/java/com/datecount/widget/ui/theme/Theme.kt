package com.datecount.widget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Sage = Color(0xFF1F4E46)
private val Clay = Color(0xFFC97848)
private val Paper = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1B2A27)
private val Mist = Color(0xFF6B7C77)

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    secondary = Clay,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8E2D6),
    onSurfaceVariant = Mist,
    outline = Color(0xFFC9C1B2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBFB4),
    onPrimary = Color(0xFF0D2A26),
    secondary = Color(0xFFE0A078),
    onSecondary = Color(0xFF2B160A),
    background = Color(0xFF121A18),
    onBackground = Color(0xFFE8EFEA),
    surface = Color(0xFF1B2623),
    onSurface = Color(0xFFE8EFEA),
    surfaceVariant = Color(0xFF2A3733),
    onSurfaceVariant = Color(0xFFB7C6C1),
    outline = Color(0xFF3E4D48)
)

@Composable
fun DateCountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

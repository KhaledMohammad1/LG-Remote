package com.example.lgremote.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF121418)
val SurfaceDark = Color(0xFF1E222B)
val LGRed = Color(0xFFA50034)
val AccentCyan = Color(0xFF00E5FF)
val CardBackground = Color(0xFF262C38)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFFA0AAB8)

private val DarkColorScheme = darkColorScheme(
    primary = LGRed,
    secondary = AccentCyan,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun LGRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

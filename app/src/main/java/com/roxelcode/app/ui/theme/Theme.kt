package com.roxelcode.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF14141F),
    onSurface = Color(0xFFE5E5EA),
    secondary = Color(0xFF3B82F6),
)

@Composable
fun RoxelCodeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}

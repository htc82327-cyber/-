package com.thxrealx7.peyvodu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun accentFor(name: String): Color = when (name) {
    "blue" -> Color(0xFF268EF5)
    "amber" -> Color(0xFFE4A528)
    "green" -> Color(0xFF2EAE76)
    else -> Color(0xFF6D5DFC)
}

@Composable
fun PeyVoduTheme(themeName: String, content: @Composable () -> Unit) {
    val accent = accentFor(themeName)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFFF3F5F8),
            onBackground = Color(0xFF151820),
            surface = Color.White,
            onSurface = Color(0xFF151820),
            surfaceVariant = Color(0xFFF0F2F6),
            onSurfaceVariant = Color(0xFF737D8D),
            outline = Color(0xFFE1E5EC)
        ),
        content = content
    )
}

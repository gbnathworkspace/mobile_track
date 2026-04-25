package com.mobiletrack.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mobiletrack.app.presentation.design.MobileTrackShapes
import com.mobiletrack.app.presentation.design.MobileTrackTypography

enum class UnlockTheme { LIGHT, DARK, GLASS }

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    secondary = Color(0xFF0288D1),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    error = Color(0xFFD32F2F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF0A0E21),
    onBackground = Color.White,
    surface = Color(0xFF0F1529),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF131A2E),
    onSurfaceVariant = Color(0xFF7B8CB8),
    outline = Color(0xFF4A5A78),
    error = Color(0xFFEF5350),
    surfaceContainer = Color(0xFF0A0E21),
    surfaceContainerLow = Color(0xFF0A0E21),
    surfaceContainerHigh = Color(0xFF131A2E)
)

// Frosted-glass aesthetic: deep navy background, icy-blue accents, translucent surfaces
val GlassColors = darkColorScheme(
    primary = Color(0xFFB8D4FF),
    onPrimary = Color(0xFF001D3D),
    secondary = Color(0xFF8EC5FC),
    onSecondary = Color(0xFF001C38),
    background = Color(0xFF060D1F),
    onBackground = Color(0xFFE8F0FF),
    surface = Color(0x26FFFFFF),       // 15% white — frosted panel
    onSurface = Color(0xFFE8F0FF),
    surfaceVariant = Color(0x40FFFFFF), // 25% white — slightly opaque card
    onSurfaceVariant = Color(0xFFCCDDFF),
    outline = Color(0x80B0C8FF),
    error = Color(0xFFFF8A80)
)

@Composable
fun MobileTrackTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MobileTrackTypography,
        shapes = MobileTrackShapes,
        content = content
    )
}

@Composable
fun UnlockPromptTheme(
    theme: UnlockTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        UnlockTheme.LIGHT -> LightColors
        UnlockTheme.DARK -> DarkColors
        UnlockTheme.GLASS -> GlassColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MobileTrackTypography,
        shapes = MobileTrackShapes,
        content = content
    )
}

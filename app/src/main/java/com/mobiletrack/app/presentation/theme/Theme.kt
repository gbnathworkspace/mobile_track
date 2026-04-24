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
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003064),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFEF9A9A)
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

package com.rizal.catatemas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Gold600,
    onPrimary = Color.White,
    primaryContainer = Gold100,
    onPrimaryContainer = GoldInk,
    inversePrimary = Gold300,
    secondary = Color(0xFF6B5B45),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE6D8),
    onSecondaryContainer = Color(0xFF2A2118),
    tertiary = Green500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F0E4),
    onTertiaryContainer = Color(0xFF06301F),
    error = Red500,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Sand50,
    onBackground = Color(0xFF1B1A17),
    surface = Color.White,
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFEDE5D8),
    onSurfaceVariant = Color(0xFF5A5347),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF7F0),
    surfaceContainer = Color(0xFFF5F0E7),
    surfaceContainerHigh = Color(0xFFEFE9DE),
    surfaceContainerHighest = Color(0xFFE9E2D6),
    outline = Color(0xFF9C9384),
    outlineVariant = Color(0xFFDFD8CB),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Gold300,
    onPrimary = Color(0xFF3A2B08),
    primaryContainer = Color(0xFF584309),
    onPrimaryContainer = Gold100,
    inversePrimary = Gold600,
    secondary = Color(0xFFD6C4A8),
    onSecondary = Color(0xFF392F1E),
    secondaryContainer = Color(0xFF4C4231),
    onSecondaryContainer = Color(0xFFF2E4CD),
    tertiary = Green300,
    onTertiary = Color(0xFF00331F),
    tertiaryContainer = Color(0xFF0E5A3E),
    onTertiaryContainer = Color(0xFFC6F2DC),
    error = Red300,
    onError = Color(0xFF5F140F),
    errorContainer = Color(0xFF7A2119),
    onErrorContainer = Color(0xFFFFDAD5),
    background = Ink900,
    onBackground = Color(0xFFEDE8E0),
    surface = Ink800,
    onSurface = Color(0xFFEDE8E0),
    surfaceVariant = Ink700,
    onSurfaceVariant = Color(0xFFCFC7B8),
    surfaceContainerLowest = Color(0xFF0E0F12),
    surfaceContainerLow = Color(0xFF17181C),
    surfaceContainer = Color(0xFF1C1E23),
    surfaceContainerHigh = Color(0xFF262830),
    surfaceContainerHighest = Color(0xFF30333C),
    outline = Color(0xFF8B8478),
    outlineVariant = Color(0xFF3A3D45),
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

/** gradient emas untuk header / ilustrasi (sama di light & dark) */
val GoldGradient: Brush
    get() = Brush.linearGradient(
        listOf(
            Color(0xFFF7E3A6),
            Color(0xFFE0BA55),
            Color(0xFFC39A2E),
        ),
    )

val GoldIllustrationGradient: Brush
    get() = Brush.linearGradient(
        listOf(
            Color(0xFFFDF1C6),
            Color(0xFFE3B94F),
            Color(0xFFAA7A1C),
        ),
    )

val HeaderInk = Color(0xFF3A2B08)

@Composable
fun CatatEmasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}

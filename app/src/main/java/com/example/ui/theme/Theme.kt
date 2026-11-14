package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarlingRose,
    onPrimary = Color.White,
    primaryContainer = DarlingDeepPlum,
    onPrimaryContainer = DarlingSoftBlush,
    secondary = DarlingViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D1B4E),
    onSecondaryContainer = DarlingSoftLavender,
    tertiary = DarlingCoral,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = StatusDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DarlingRose,
    onPrimary = Color.White,
    primaryContainer = DarlingSoftBlush,
    onPrimaryContainer = Color(0xFF5D0026),
    secondary = DarlingViolet,
    onSecondary = Color.White,
    secondaryContainer = DarlingSoftLavender,
    onSecondaryContainer = Color(0xFF2C0B6E),
    tertiary = DarlingCoral,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = StatusDanger,
    onError = Color.White
)

val DarlingShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val DarlingGradientBrush = Brush.linearGradient(
    colors = listOf(DarlingViolet, DarlingRose, DarlingCoral)
)

val StoryRingBrush = Brush.sweepGradient(
    colors = listOf(DarlingCoral, DarlingRose, DarlingViolet, DarlingRose, DarlingCoral)
)

@Composable
fun DarlingDateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DarlingShapes,
        content = content
    )
}

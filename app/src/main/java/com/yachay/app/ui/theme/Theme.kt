package com.yachay.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Tema escuro único: fundo Night, cards creme com contorno em tinta, roxo como campo da marca.
private val YachayColors = darkColorScheme(
    primary = Purple,
    onPrimary = Cream,
    primaryContainer = Purple,
    onPrimaryContainer = Cream,
    secondary = Lime,
    onSecondary = Ink,
    secondaryContainer = Lime,
    onSecondaryContainer = Ink,
    tertiary = Lilac,
    onTertiary = Ink,
    background = Night,
    onBackground = Cream,
    surface = Night,
    onSurface = Cream,
    surfaceVariant = Paper,
    onSurfaceVariant = Ink,
    surfaceContainer = Night,
    surfaceContainerLow = Night,
    surfaceContainerHigh = Paper,
    surfaceContainerHighest = Paper,
    outline = Ink,
    outlineVariant = Ink,
    error = Danger,
    onError = Ink,
)

private val YachayShapes = Shapes(
    extraSmall = RoundedCornerShape(NeoDefaults.CornerRadius),
    small = RoundedCornerShape(NeoDefaults.CornerRadius),
    medium = RoundedCornerShape(NeoDefaults.CornerRadius),
    large = RoundedCornerShape(NeoDefaults.CornerRadius),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun YachayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YachayColors,
        typography = YachayTypography,
        shapes = YachayShapes,
        content = content,
    )
}

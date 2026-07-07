package com.tessera.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TesseraWarmPaper = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE9D5),
    onPrimaryContainer = Color(0xFF06351A),
    secondary = Color(0xFF4D6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E5DB),
    onSecondaryContainer = Color(0xFF16241B),
    tertiary = Color(0xFF6B6052),
    onTertiary = Color.White,
    background = GlassBgLight,
    onBackground = GlassInk,
    surface = Color.White,
    onSurface = GlassInk,
    surfaceVariant = Color(0xFFEDE7DC),
    onSurfaceVariant = GlassInkMuted,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFFE4DCCF),
    outlineVariant = Color(0xFFEDE7DC),
)

@Composable
fun TesseraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TesseraWarmPaper,
        typography = TesseraTypography,
        content = content,
    )
}

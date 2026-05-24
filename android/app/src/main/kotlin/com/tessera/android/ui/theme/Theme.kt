package com.tessera.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TesseraLightScheme = lightColorScheme(
    primary = TesseraForest,
    onPrimary = Color.White,
    primaryContainer = TesseraForestSoft,
    onPrimaryContainer = TesseraForest,
    secondary = Neutral700,
    onSecondary = Color.White,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral900,
    tertiary = Neutral500,
    background = Color.White,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral500,
    error = StatusInvalid,
    onError = Color.White,
    errorContainer = StatusInvalidSoft,
    onErrorContainer = StatusInvalid,
    outline = Neutral200,
    outlineVariant = Neutral200,
)

@Composable
fun TesseraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TesseraLightScheme,
        typography = TesseraTypography,
        content = content,
    )
}

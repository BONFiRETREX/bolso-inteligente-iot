package com.securitybag.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandSky,
    onPrimary = BrandNavy,
    primaryContainer = BrandBlue,
    onPrimaryContainer = SurfaceLight,
    secondary = BrandSky,
    background = Ink,
    surface = ColorDarkSurface,
    onSurface = SurfaceLight,
    error = Critical,
    errorContainer = OnCriticalContainer,
    onErrorContainer = CriticalContainer
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceLight,
    primaryContainer = BrandSky,
    onPrimaryContainer = BrandNavy,
    secondary = BrandNavy,
    onSecondary = SurfaceLight,
    tertiary = Warning,
    onTertiary = SurfaceLight,
    background = Canvas,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = BrandSky,
    onSurfaceVariant = BrandNavy,
    error = Critical,
    onError = SurfaceLight,
    errorContainer = CriticalContainer,
    onErrorContainer = OnCriticalContainer
)

@Composable
fun BolsoInteligenteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Se mantiene la identidad visual de la aplicación en todos los dispositivos.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

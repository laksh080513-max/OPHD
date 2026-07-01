package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BullGreen,
    secondary = AccentOrange,
    tertiary = BearRed,
    background = BgMidnight,
    surface = SurfaceDark,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCard,
    outline = BorderNavy
)

private val LightColorScheme = lightColorScheme(
    primary = BullGreen,
    secondary = AccentOrange,
    tertiary = BearRed,
    background = BgMidnight,
    surface = SurfaceDark,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCard,
    outline = BorderNavy
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isDarkThemeEnabled.value,
    dynamicColor: Boolean = false, // Force consistent branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

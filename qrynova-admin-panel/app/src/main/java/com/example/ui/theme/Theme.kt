package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = IndigoVibrant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4338CA),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = VioletNeon,
    onTertiary = Color.White,
    background = DarkBgMain,
    onBackground = TextPrimaryDark,
    surface = DarkSurfaceCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceCardElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkSurfaceBorder,
    error = ErrorRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanGlowDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAF0F8),
    onPrimaryContainer = Color(0xFF003049),
    secondary = IndigoVibrant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = VioletNeon,
    onTertiary = Color.White,
    background = LightBgMain,
    onBackground = TextPrimaryLight,
    surface = LightSurfaceCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceCardElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightSurfaceBorder,
    error = ErrorRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek futuristic dark theme for enterprise admin
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

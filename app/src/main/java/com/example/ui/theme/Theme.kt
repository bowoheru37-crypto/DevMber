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

private val DarkColorScheme =
  darkColorScheme(
    primary = StudioCyanPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = StudioCyanDark,
    onPrimaryContainer = Color(0xFFC7F8FF),
    secondary = StudioVioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A148C),
    onSecondaryContainer = Color(0xFFEEDBFF),
    tertiary = StudioPinkTertiary,
    onTertiary = Color.White,
    background = StudioDarkBg,
    onBackground = TextPrimaryDark,
    surface = StudioDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = StudioError,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StudioLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBEBF2),
    onPrimaryContainer = Color(0xFF002025),
    secondary = StudioLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEDBFF),
    onSecondaryContainer = Color(0xFF280058),
    tertiary = StudioLightTertiary,
    onTertiary = Color.White,
    background = StudioLightBg,
    onBackground = TextPrimaryLight,
    surface = StudioLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = StudioError,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek studio dark theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


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
    primary = EmeraldPrimary,
    secondary = GoldAccent,
    tertiary = EmeraldGrey80,
    background = EmeraldDarkBackground,
    surface = EmeraldSurface,
    onPrimary = Emerald80,
    onSecondary = EmeraldDarkBackground,
    onBackground = Emerald80,
    onSurface = Emerald80
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = GoldAccent,
    tertiary = EmeraldGrey80,
    background = EmeraldDarkBackground,
    surface = EmeraldSurface,
    onPrimary = Emerald80,
    onSecondary = EmeraldDarkBackground,
    onBackground = Emerald80,
    onSurface = Emerald80
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (we disable to preserve the custom holy green/gold theme)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

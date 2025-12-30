package com.vardev.moon_phase.ui.theme

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
    primary = MoonGold,
    secondary = MoonSilver,
    tertiary = DarkGray,
    background = PureBlack,
    surface = NearBlack,
    surfaceVariant = DarkGray,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = MoonSilver,
    primaryContainer = DarkGray,
    onPrimaryContainer = MoonGold
)

private val LightColorScheme = lightColorScheme(
    primary = SoftGold,
    secondary = DaySky,
    tertiary = MidnightBlue,
    background = CloudWhite,
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E8E8),
    onPrimary = Color.White,
    onSecondary = NightSky,
    onBackground = NightSky,
    onSurface = NightSky,
    onSurfaceVariant = Color(0xFF505050),
    primaryContainer = Color(0xFFFFF8DC),
    onPrimaryContainer = SoftGold
)

@Composable
fun MoonphaseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

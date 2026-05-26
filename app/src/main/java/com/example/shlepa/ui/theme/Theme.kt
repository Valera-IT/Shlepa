package com.example.shlepa.ui.theme

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
import com.example.shlepa.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val PinkColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = PinkOnPrimary,
    background = PinkBackground,
    onBackground = PinkOnBackground,
    surface = PinkSurface,
    onSurface = PinkOnSurface,
    secondary = PinkSecondary,
    tertiary = PinkTertiary,
    surfaceVariant = PinkSurfaceVariant,
    onSurfaceVariant = PinkOnSurfaceVariant
)

private val YellowColorScheme = lightColorScheme(
    primary = YellowPrimary,
    onPrimary = YellowOnPrimary,
    secondary = YellowSecondary,
    background = YellowBackground,
    onBackground = YellowOnBackground,
    surface = YellowSurface,
    onSurface = YellowOnSurface,
    surfaceVariant = YellowSurfaceVariant,
    onSurfaceVariant = YellowOnSurfaceVariant,
    tertiary = YellowSecondary
)

@Composable
fun ShlepaTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false, // Отключим по умолчанию для кастомных тем
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.DARK -> true
        AppTheme.LIGHT, AppTheme.YELLOW, AppTheme.PINK -> false
    }

    val colorScheme = when (appTheme) {
        AppTheme.PINK -> PinkColorScheme
        AppTheme.YELLOW -> YellowColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
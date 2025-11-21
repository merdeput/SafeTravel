package com.safetravel.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// --- Original Default Theme --- //
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// --- New Bee Theme --- //
private val BeeDarkColorScheme = darkColorScheme(
    primary = BrandYellow,
    secondary = LightGrey,
    background = BrandBlack,
    surface = BrandBlack,
    onPrimary = BrandBlack,
    onSecondary = BrandBlack,
    onBackground = OffWhite,
    onSurface = OffWhite
)

private val BeeLightColorScheme = lightColorScheme(
    primary = BrandYellow,
    secondary = BrandBlack,
    background = OffWhite,
    surface = LightGrey,
    onPrimary = BrandBlack,
    onSecondary = OffWhite,
    onBackground = BrandBlack,
    onSurface = BrandBlack
)


@Composable
fun TestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Keep dynamic color enabled for the default theme
    content: @Composable () -> Unit
) {
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

@Composable
fun BeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BeeDarkColorScheme else BeeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

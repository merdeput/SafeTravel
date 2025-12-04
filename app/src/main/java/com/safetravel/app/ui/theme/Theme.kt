package com.safetravel.app.ui.theme

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

// --- Original Default Theme (Kept for reference) --- //
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
    onPrimary = BrandBlack,
    primaryContainer = BrandYellowContainer,
    onPrimaryContainer = BrandBlack,
    // Use Yellow for secondary container to make active states (like BottomNav) Yellow
    secondary = BrandYellow, 
    onSecondary = BrandBlack,
    secondaryContainer = BrandYellow, 
    onSecondaryContainer = BrandBlack, 
    tertiary = GreyVariant,
    onTertiary = BrandBlack,
    tertiaryContainer = BrandBlackContainer,
    onTertiaryContainer = LightGrey,
    background = BrandBlack,
    onBackground = OffWhite,
    surface = BrandBlack,
    onSurface = OffWhite,
    surfaceVariant = BrandBlackContainer,
    onSurfaceVariant = LightGrey,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    outline = GreyVariant
)

private val BeeLightColorScheme = lightColorScheme(
    primary = BrandYellow,
    onPrimary = BrandBlack,
    primaryContainer = BrandYellowContainer,
    onPrimaryContainer = BrandBlack,
    // Use YellowContainer for secondary container in light mode for softer active states
    secondary = BrandYellow,
    onSecondary = BrandBlack,
    secondaryContainer = BrandYellowContainer,
    onSecondaryContainer = BrandBlack,
    tertiary = DarkGrey,
    onTertiary = OffWhite,
    tertiaryContainer = LightGrey,
    onTertiaryContainer = BrandBlack,
    background = OffWhite,
    onBackground = BrandBlack,
    surface = OffWhite,
    onSurface = BrandBlack,
    surfaceVariant = LightGrey,
    onSurfaceVariant = BrandBlack,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = DarkGrey
)


@Composable
fun TestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
    // Dynamic color is turned OFF for BeeTheme to enforce branding
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BeeDarkColorScheme
        else -> BeeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

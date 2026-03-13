package com.hjw.qbremote.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hjw.qbremote.data.AppTheme

private val QbDarkColors = darkColorScheme(
    primary = Color(0xFF29E0D4),
    onPrimary = Color(0xFF001D1A),
    secondary = Color(0xFF73B7FF),
    onSecondary = Color(0xFF0A1A30),
    tertiary = Color(0xFFB7F5FF),
    onTertiary = Color(0xFF05212A),
    background = Color(0xFF05090F),
    onBackground = Color(0xFFE9F4FF),
    surface = Color(0xFF090E16),
    onSurface = Color(0xFFF1F6FF),
    surfaceVariant = Color(0xFF152132),
    onSurfaceVariant = Color(0xFFC9D8EA),
    surfaceContainer = Color(0xFF101A29),
    surfaceContainerHigh = Color(0xFF162334),
    primaryContainer = Color(0xFF0C2C31),
    secondaryContainer = Color(0xFF162A43),
    outline = Color(0xFF607792),
)

private val QbLightColors = lightColorScheme(
    primary = Color(0xFF006C73),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF1D4F8C),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF2E6D4A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F8FC),
    onBackground = Color(0xFF0E1A2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF132235),
    surfaceVariant = Color(0xFFDDE7F2),
    onSurfaceVariant = Color(0xFF34495F),
    surfaceContainer = Color(0xFFEAF1F8),
    surfaceContainerHigh = Color(0xFFE2ECF6),
    primaryContainer = Color(0xFFB8F1F3),
    secondaryContainer = Color(0xFFD5E7FF),
    outline = Color(0xFF7A90A8),
)

private val QbTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.3.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.35.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
    ),
)

data class QbThemeState(
    val appTheme: AppTheme = AppTheme.DARK,
    val customBackgroundToneIsLight: Boolean = false,
) {
    val isCustomTheme: Boolean
        get() = appTheme == AppTheme.CUSTOM

    val usesDarkPalette: Boolean
        get() = when (appTheme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.CUSTOM -> !customBackgroundToneIsLight
        }
}

val LocalQbThemeState = staticCompositionLocalOf { QbThemeState() }

@Composable
fun QBRemoteTheme(
    appTheme: AppTheme = AppTheme.DARK,
    customBackgroundToneIsLight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val themeState = QbThemeState(
        appTheme = appTheme,
        customBackgroundToneIsLight = customBackgroundToneIsLight,
    )
    val baseScheme = if (themeState.usesDarkPalette) QbDarkColors else QbLightColors
    val colorScheme = if (themeState.isCustomTheme) {
        glassColorScheme(
            base = baseScheme,
            useLightGlass = customBackgroundToneIsLight,
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalQbThemeState provides themeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QbTypography,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (themeState.isCustomTheme) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.background
                },
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}

private fun glassColorScheme(
    base: ColorScheme,
    useLightGlass: Boolean,
): ColorScheme {
    return if (useLightGlass) {
        base.copy(
            background = Color.Transparent,
            surface = Color(0x73FFFFFF),
            surfaceVariant = Color(0x5EFFFFFF),
            surfaceContainerLowest = Color(0x40FFFFFF),
            surfaceContainerLow = Color(0x54FFFFFF),
            surfaceContainer = Color(0x66FFFFFF),
            surfaceContainerHigh = Color(0x80FFFFFF),
            surfaceContainerHighest = Color(0x94FFFFFF),
            surfaceBright = Color(0xA3FFFFFF),
            surfaceDim = Color(0x52FFFFFF),
            primaryContainer = Color(0x6BFFFFFF),
            secondaryContainer = Color(0x62FFFFFF),
            tertiaryContainer = Color(0x58FFFFFF),
            outline = Color(0x82FFFFFF),
        )
    } else {
        base.copy(
            background = Color.Transparent,
            surface = Color(0x66070D16),
            surfaceVariant = Color(0x5C1A2535),
            surfaceContainerLowest = Color(0x40050A12),
            surfaceContainerLow = Color(0x520A1320),
            surfaceContainer = Color(0x64101A29),
            surfaceContainerHigh = Color(0x7A172334),
            surfaceContainerHighest = Color(0x8F1D2B3E),
            surfaceBright = Color(0x9924364C),
            surfaceDim = Color(0x4D09111B),
            primaryContainer = Color(0x5C113544),
            secondaryContainer = Color(0x54172D49),
            tertiaryContainer = Color(0x4A1A3A38),
            outline = Color(0x8CD8E6F7),
        )
    }
}

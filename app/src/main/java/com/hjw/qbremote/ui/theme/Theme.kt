package com.hjw.qbremote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

@Composable
fun QBRemoteTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) QbDarkColors else QbLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = QbTypography,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}

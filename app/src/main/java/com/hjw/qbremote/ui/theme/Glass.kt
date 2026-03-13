package com.hjw.qbremote.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun qbGlassCardColors(): CardColors {
    return CardDefaults.outlinedCardColors(
        containerColor = qbGlassContainerColor(),
    )
}

@Composable
fun qbGlassContainerColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.surfaceContainerHigh.copy(alpha = 0.86f)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.surfaceContainerHigh.copy(alpha = 0.62f)
    } else {
        colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
    }
}

@Composable
fun qbGlassStrongContainerColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
    } else {
        colorScheme.surfaceContainerHighest.copy(alpha = 0.82f)
    }
}

@Composable
fun qbGlassSubtleContainerColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.surfaceContainer.copy(alpha = 0.85f)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.surfaceContainer.copy(alpha = 0.50f)
    } else {
        colorScheme.surfaceContainer.copy(alpha = 0.66f)
    }
}

@Composable
fun qbGlassHoleColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.surfaceContainerHighest.copy(alpha = 0.78f)
    } else {
        colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
    }
}

@Composable
fun qbGlassOutlineColor(defaultAlpha: Float = 0.45f): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.outline.copy(alpha = defaultAlpha)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.outline.copy(alpha = 0.32f)
    } else {
        colorScheme.outline.copy(alpha = 0.24f)
    }
}

@Composable
fun qbGlassChipColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.primaryContainer.copy(alpha = 0.72f)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.primaryContainer.copy(alpha = 0.44f)
    } else {
        colorScheme.primaryContainer.copy(alpha = 0.56f)
    }
}

@Composable
fun qbGlassEmptyStateColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    val themeState = LocalQbThemeState.current
    if (!themeState.isCustomTheme) {
        return colorScheme.surfaceContainer.copy(alpha = 0.44f)
    }
    return if (themeState.customBackgroundToneIsLight) {
        colorScheme.surfaceContainer.copy(alpha = 0.26f)
    } else {
        colorScheme.surfaceContainer.copy(alpha = 0.38f)
    }
}

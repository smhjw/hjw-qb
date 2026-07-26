package com.hjw.qbremote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.data.AppTheme
import kotlin.math.roundToInt

private val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF060A12),
        Color(0xFF0B1422),
        Color(0xFF08131E),
        Color(0xFF060A12),
    ),
)
private val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF5FAFF),
        Color(0xFFEAF3FC),
        Color(0xFFE4F0F9),
        Color(0xFFF6FAFF),
    ),
)

@Composable
internal fun MainScreenBackdrop(
    appTheme: AppTheme,
    customBackgroundAvailable: Boolean,
    customBackgroundImagePath: String,
    customBackgroundToneIsLight: Boolean,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val backgroundTargetSizePx = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density,
    ) {
        val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
        (maxOf(widthPx, heightPx) * 1.18f).roundToInt().coerceAtLeast(1)
    }
    val effectiveAppTheme = resolveEffectiveAppTheme(
        appTheme = appTheme,
        customBackgroundAvailable = customBackgroundAvailable,
    )
    val appBackgroundBrush = when (effectiveAppTheme) {
        AppTheme.DARK -> DarkBackgroundGradient
        AppTheme.LIGHT -> LightBackgroundGradient
        AppTheme.CUSTOM -> DarkBackgroundGradient
    }
    val customBackgroundState = rememberCustomBackgroundImageState(
        path = customBackgroundImagePath,
        targetMaxDimensionPx = backgroundTargetSizePx,
    )
    val customBackgroundImage = customBackgroundState.image
    val showCustomBackgroundImage = effectiveAppTheme == AppTheme.CUSTOM && customBackgroundImage != null
    val customBackgroundScrim = if (customBackgroundToneIsLight) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }
    if (showCustomBackgroundImage) {
        customBackgroundImage?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(customBackgroundScrim),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush),
        )
    }
}

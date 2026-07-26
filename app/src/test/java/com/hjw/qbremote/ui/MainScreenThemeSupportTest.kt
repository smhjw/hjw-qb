package com.hjw.qbremote.ui

import com.hjw.qbremote.data.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.lang.reflect.Method

class MainScreenThemeSupportTest {

    @Test
    fun buildPageThemeSignature_changesWhenThemeSettingsChange() {
        val dark = invokeBuildPageThemeSignature(
            appTheme = AppTheme.DARK,
            customBackgroundToneIsLight = false,
            customBackgroundImagePath = "",
        )
        val light = invokeBuildPageThemeSignature(
            appTheme = AppTheme.LIGHT,
            customBackgroundToneIsLight = false,
            customBackgroundImagePath = "",
        )
        val custom = invokeBuildPageThemeSignature(
            appTheme = AppTheme.CUSTOM,
            customBackgroundToneIsLight = true,
            customBackgroundImagePath = "wallpaper.png",
        )

        assertNotEquals(dark, light)
        assertNotEquals(light, custom)
    }

    @Test
    fun resolveEffectiveAppTheme_fallsBackToDarkWhenCustomBackgroundUnavailable() {
        assertEquals(
            AppTheme.DARK,
            resolveEffectiveAppTheme(appTheme = AppTheme.CUSTOM, customBackgroundAvailable = false),
        )
    }

    @Test
    fun resolveEffectiveAppTheme_keepsCustomWhenBackgroundAvailable() {
        assertEquals(
            AppTheme.CUSTOM,
            resolveEffectiveAppTheme(appTheme = AppTheme.CUSTOM, customBackgroundAvailable = true),
        )
    }

    @Test
    fun resolveEffectiveAppTheme_passesThroughDarkAndLight() {
        assertEquals(
            AppTheme.DARK,
            resolveEffectiveAppTheme(appTheme = AppTheme.DARK, customBackgroundAvailable = false),
        )
        assertEquals(
            AppTheme.LIGHT,
            resolveEffectiveAppTheme(appTheme = AppTheme.LIGHT, customBackgroundAvailable = false),
        )
    }

    private fun invokeBuildPageThemeSignature(
        appTheme: AppTheme,
        customBackgroundToneIsLight: Boolean,
        customBackgroundImagePath: String,
    ): String {
        val method = findRequiredStaticMethod(
            containerClassName = "com.hjw.qbremote.ui.MainScreenSupportKt",
            functionName = "buildPageThemeSignature",
            parameterTypes = arrayOf(
                AppTheme::class.java,
                Boolean::class.javaPrimitiveType ?: Boolean::class.java,
                String::class.java,
            ),
        )
        return method.invoke(null, appTheme, customBackgroundToneIsLight, customBackgroundImagePath) as? String
            ?: error("`buildPageThemeSignature` should return String.")
    }

    private fun findRequiredStaticMethod(
        containerClassName: String,
        functionName: String,
        parameterTypes: Array<Class<*>>,
    ): Method {
        val container = Class.forName(containerClassName)
        return runCatching {
            container.getDeclaredMethod(functionName, *parameterTypes)
        }.getOrElse {
            val signature = parameterTypes.joinToString(", ") { it.simpleName }
            error("Expected `${container.simpleName}.$functionName($signature)` as planned top-level helper.")
        }.also { it.isAccessible = true }
    }
}

package com.hjw.qbremote.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.ui.theme.QBRemoteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notificationSwitch_hasSemanticActionAndMinimumTouchTargetAtLargeFont() {
        var enabled = false
        val title = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.settings_completion_notifications)

        composeRule.setContent {
            val largeFontConfiguration = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = 360
                fontScale = 2f
            }
            CompositionLocalProvider(LocalConfiguration provides largeFontConfiguration) {
                QBRemoteTheme {
                    SettingsPageContent(
                        settings = ConnectionSettings(),
                        onAppLanguageChange = {},
                        onDeleteFilesWhenNoSeedersChange = {},
                        onDeleteFilesDefaultChange = {},
                        onCompletionNotificationsChange = { enabled = it },
                        notificationPermissionDenied = false,
                        onOpenNotificationSettings = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("setting-switch:$title")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(enabled)
    }
}

package com.hjw.qbremote.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ConnectionSettingsPersistenceRoundTripTest {
    @Test
    fun `every field round trips through write and read`() {
        val settings = ConnectionSettings(
            host = "seed.example.com",
            port = 9091,
            useHttps = true,
            username = "root",
            password = "s3cret",
            serverBackendType = ServerBackendType.TRANSMISSION,
            refreshSeconds = 60,
            appLanguage = AppLanguage.ZH_CN,
            appTheme = AppTheme.LIGHT,
            customBackgroundImagePath = "/sdcard/bg.png",
            customBackgroundToneIsLight = true,
            deleteFilesDefault = false,
            deleteFilesWhenNoSeeders = true,
            completionNotificationsEnabled = true,
            homeTorrentEntryHintDismissed = true,
            hasSeenDashboardHideHint = true,
            hasSeenDashboardHiddenSnack = true,
            hasSeenServerStackReorderHint = true,
            hasSeenServerDashboardSwipeHint = true,
            hasSeenServerDashboardCardHint = true,
        )
        val target = mutablePreferencesOf()
        target[Keys.PasswordLegacy] = "plain-old-password"

        applyConnectionSettingsSnapshot(
            target = target,
            settings = settings,
            resolvedActiveProfileId = "profile-1",
            profilesJson = "[]",
        )
        applyUiPreferencePatch(
            target,
            UiPreferencePatch(
                appLanguage = settings.appLanguage,
                appTheme = settings.appTheme,
                customBackgroundImagePath = settings.customBackgroundImagePath,
                customBackgroundToneIsLight = settings.customBackgroundToneIsLight,
                deleteFilesDefault = settings.deleteFilesDefault,
                deleteFilesWhenNoSeeders = settings.deleteFilesWhenNoSeeders,
                completionNotificationsEnabled = settings.completionNotificationsEnabled,
                homeTorrentEntryHintDismissed = settings.homeTorrentEntryHintDismissed,
                hasSeenDashboardHideHint = settings.hasSeenDashboardHideHint,
                hasSeenDashboardHiddenSnack = settings.hasSeenDashboardHiddenSnack,
                hasSeenServerStackReorderHint = settings.hasSeenServerStackReorderHint,
                hasSeenServerDashboardSwipeHint = settings.hasSeenServerDashboardSwipeHint,
                hasSeenServerDashboardCardHint = settings.hasSeenServerDashboardCardHint,
            ),
        )

        assertEquals(settings, target.toSettings(settings.password))
        assertFalse(target.contains(Keys.PasswordLegacy))
        assertEquals("profile-1", target[Keys.ActiveServerProfileId])
        assertEquals("[]", target[Keys.ServerProfilesJson])
    }

    @Test
    fun `empty preferences read back as defaults`() {
        assertEquals(ConnectionSettings(), emptyPreferences().toSettings(""))
    }

    @Test
    fun `invalid enum strings fall back to defaults`() {
        val pref = mutablePreferencesOf()
        pref[Keys.AppTheme] = "GARBAGE"
        pref[Keys.AppLanguage] = "GARBAGE"
        pref[Keys.ServerBackendType] = "GARBAGE"

        val settings = pref.toSettings("")

        assertEquals(AppTheme.DARK, settings.appTheme)
        assertEquals(AppLanguage.SYSTEM, settings.appLanguage)
        assertEquals(ServerBackendType.QBITTORRENT, settings.serverBackendType)
    }

    @Test
    fun `refresh seconds clamp on read`() {
        val tooHigh = mutablePreferencesOf()
        tooHigh[Keys.RefreshSeconds] = 999
        assertEquals(120, tooHigh.toSettings("").refreshSeconds)

        val tooLow = mutablePreferencesOf()
        tooLow[Keys.RefreshSeconds] = 1
        assertEquals(5, tooLow.toSettings("").refreshSeconds)
    }
}

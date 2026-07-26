package com.hjw.qbremote.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys
import org.junit.Assert.assertEquals
import org.junit.Test

class UiPreferenceSupportTest {
    @Test
    fun `merge keeps latest non null value`() {
        val base = UiPreferencePatch(appTheme = AppTheme.DARK, deleteFilesDefault = true)
        val next = UiPreferencePatch(appTheme = AppTheme.LIGHT)

        val merged = mergeUiPreferencePatches(base, next)

        assertEquals(AppTheme.LIGHT, merged.appTheme)
        assertEquals(true, merged.deleteFilesDefault)
    }

    @Test
    fun `null fields in next patch do not clear base values`() {
        val base = UiPreferencePatch(
            customBackgroundImagePath = "/sdcard/bg.png",
            completionNotificationsEnabled = true,
            hasSeenServerStackReorderHint = true,
        )

        assertEquals(base, mergeUiPreferencePatches(base, UiPreferencePatch()))
    }

    @Test
    fun `disjoint patches merge into their union`() {
        val base = UiPreferencePatch(appLanguage = AppLanguage.ZH_CN)
        val next = UiPreferencePatch(appTheme = AppTheme.CUSTOM, hasSeenDashboardHideHint = true)

        assertEquals(
            UiPreferencePatch(
                appLanguage = AppLanguage.ZH_CN,
                appTheme = AppTheme.CUSTOM,
                hasSeenDashboardHideHint = true,
            ),
            mergeUiPreferencePatches(base, next),
        )
    }

    @Test
    fun `apply writes only keys for non null fields`() {
        val target = mutablePreferencesOf()

        applyUiPreferencePatch(
            target,
            UiPreferencePatch(appTheme = AppTheme.LIGHT, deleteFilesWhenNoSeeders = true),
        )

        assertEquals(
            setOf<Preferences.Key<*>>(Keys.AppTheme, Keys.DeleteFilesWhenNoSeeders),
            target.asMap().keys,
        )
        assertEquals("LIGHT", target[Keys.AppTheme])
        assertEquals(true, target[Keys.DeleteFilesWhenNoSeeders])
    }

    @Test
    fun `apply never touches connection identity keys`() {
        val target = mutablePreferencesOf()
        target[Keys.Host] = "nas.local"
        target[Keys.Port] = 9091
        target[Keys.Username] = "root"
        target[Keys.ServerProfilesJson] = "[]"
        target[Keys.ActiveServerProfileId] = "p1"

        applyUiPreferencePatch(
            target,
            UiPreferencePatch(
                appLanguage = AppLanguage.EN,
                appTheme = AppTheme.CUSTOM,
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
            ),
        )

        assertEquals("nas.local", target[Keys.Host])
        assertEquals(9091, target[Keys.Port])
        assertEquals("root", target[Keys.Username])
        assertEquals("[]", target[Keys.ServerProfilesJson])
        assertEquals("p1", target[Keys.ActiveServerProfileId])
        assertEquals(18, target.asMap().size)
    }
}

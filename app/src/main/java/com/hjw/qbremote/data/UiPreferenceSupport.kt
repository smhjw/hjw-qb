package com.hjw.qbremote.data

import androidx.datastore.preferences.core.MutablePreferences
import com.hjw.qbremote.data.ConnectionPreferenceKeys as Keys

data class UiPreferencePatch(
    val appLanguage: AppLanguage? = null,
    val appTheme: AppTheme? = null,
    val customBackgroundImagePath: String? = null,
    val customBackgroundToneIsLight: Boolean? = null,
    val deleteFilesDefault: Boolean? = null,
    val deleteFilesWhenNoSeeders: Boolean? = null,
    val completionNotificationsEnabled: Boolean? = null,
    val homeTorrentEntryHintDismissed: Boolean? = null,
    val hasSeenDashboardHideHint: Boolean? = null,
    val hasSeenDashboardHiddenSnack: Boolean? = null,
    val hasSeenServerStackReorderHint: Boolean? = null,
    val hasSeenServerDashboardSwipeHint: Boolean? = null,
    val hasSeenServerDashboardCardHint: Boolean? = null,
)

internal fun mergeUiPreferencePatches(
    base: UiPreferencePatch,
    next: UiPreferencePatch,
): UiPreferencePatch {
    return UiPreferencePatch(
        appLanguage = next.appLanguage ?: base.appLanguage,
        appTheme = next.appTheme ?: base.appTheme,
        customBackgroundImagePath = next.customBackgroundImagePath ?: base.customBackgroundImagePath,
        customBackgroundToneIsLight = next.customBackgroundToneIsLight ?: base.customBackgroundToneIsLight,
        deleteFilesDefault = next.deleteFilesDefault ?: base.deleteFilesDefault,
        deleteFilesWhenNoSeeders = next.deleteFilesWhenNoSeeders ?: base.deleteFilesWhenNoSeeders,
        completionNotificationsEnabled = next.completionNotificationsEnabled
            ?: base.completionNotificationsEnabled,
        homeTorrentEntryHintDismissed = next.homeTorrentEntryHintDismissed
            ?: base.homeTorrentEntryHintDismissed,
        hasSeenDashboardHideHint = next.hasSeenDashboardHideHint ?: base.hasSeenDashboardHideHint,
        hasSeenDashboardHiddenSnack = next.hasSeenDashboardHiddenSnack
            ?: base.hasSeenDashboardHiddenSnack,
        hasSeenServerStackReorderHint = next.hasSeenServerStackReorderHint
            ?: base.hasSeenServerStackReorderHint,
        hasSeenServerDashboardSwipeHint = next.hasSeenServerDashboardSwipeHint
            ?: base.hasSeenServerDashboardSwipeHint,
        hasSeenServerDashboardCardHint = next.hasSeenServerDashboardCardHint
            ?: base.hasSeenServerDashboardCardHint,
    )
}

internal fun applyUiPreferencePatch(target: MutablePreferences, patch: UiPreferencePatch) {
    patch.appLanguage?.let { target[Keys.AppLanguage] = it.name }
    patch.appTheme?.let { target[Keys.AppTheme] = it.name }
    patch.customBackgroundImagePath?.let { target[Keys.CustomBackgroundImagePath] = it }
    patch.customBackgroundToneIsLight?.let { target[Keys.CustomBackgroundToneIsLight] = it }
    patch.deleteFilesDefault?.let { target[Keys.DeleteFilesDefault] = it }
    patch.deleteFilesWhenNoSeeders?.let { target[Keys.DeleteFilesWhenNoSeeders] = it }
    patch.completionNotificationsEnabled?.let { target[Keys.CompletionNotificationsEnabled] = it }
    patch.homeTorrentEntryHintDismissed?.let { target[Keys.HomeTorrentEntryHintDismissed] = it }
    patch.hasSeenDashboardHideHint?.let { target[Keys.HasSeenDashboardHideHint] = it }
    patch.hasSeenDashboardHiddenSnack?.let { target[Keys.HasSeenDashboardHiddenSnack] = it }
    patch.hasSeenServerStackReorderHint?.let { target[Keys.HasSeenServerStackReorderHint] = it }
    patch.hasSeenServerDashboardSwipeHint?.let { target[Keys.HasSeenServerDashboardSwipeHint] = it }
    patch.hasSeenServerDashboardCardHint?.let { target[Keys.HasSeenServerDashboardCardHint] = it }
}

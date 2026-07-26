package com.hjw.qbremote.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object ConnectionPreferenceKeys {
    val Host = stringPreferencesKey("host")
    val Port = intPreferencesKey("port")
    val UseHttps = booleanPreferencesKey("use_https")
    val Username = stringPreferencesKey("username")
    val ServerBackendType = stringPreferencesKey("server_backend_type")
    val PasswordLegacy = stringPreferencesKey("password")
    val RefreshSeconds = intPreferencesKey("refresh_seconds")
    val AppLanguage = stringPreferencesKey("app_language")
    val AppTheme = stringPreferencesKey("app_theme")
    val CustomBackgroundImagePath = stringPreferencesKey("custom_background_image_path")
    val CustomBackgroundToneIsLight = booleanPreferencesKey("custom_background_tone_is_light")
    val DeleteFilesDefault = booleanPreferencesKey("delete_files_default")
    val DeleteFilesWhenNoSeeders = booleanPreferencesKey("delete_files_when_no_seeders")
    val CompletionNotificationsEnabled = booleanPreferencesKey("completion_notifications_enabled")
    val CompletionNotificationStatesJson = stringPreferencesKey("completion_notification_states_json")
    val HomeTorrentEntryHintDismissed = booleanPreferencesKey("home_torrent_entry_hint_dismissed")
    val HasSeenDashboardHideHint = booleanPreferencesKey("has_seen_dashboard_hide_hint")
    val HasSeenDashboardHiddenSnack = booleanPreferencesKey("has_seen_dashboard_hidden_snack")
    val HasSeenServerStackReorderHint = booleanPreferencesKey("has_seen_server_stack_reorder_hint")
    val HasSeenServerDashboardSwipeHint = booleanPreferencesKey("has_seen_server_dashboard_swipe_hint")
    val HasSeenServerDashboardCardHint = booleanPreferencesKey("has_seen_server_dashboard_card_hint")
    val ServerProfilesJson = stringPreferencesKey("server_profiles_json")
    val ActiveServerProfileId = stringPreferencesKey("active_server_profile_id")
    val DailyUploadTrackingJson = stringPreferencesKey("daily_upload_tracking_json")
    val DailyCountryUploadTrackingJson = stringPreferencesKey("daily_country_upload_tracking_json")
    val DashboardCacheJson = stringPreferencesKey("dashboard_cache_json")
    val DashboardServerSnapshotsJson = stringPreferencesKey("dashboard_server_snapshots_json")
    val DeprecatedDashboardTrendHistoryJson = stringPreferencesKey("dashboard_trend_history_json")
    val HomeAggregateSpeedHistoryJson = stringPreferencesKey("home_aggregate_speed_history_json")
    val ServerDashboardPreferencesJson = stringPreferencesKey("server_dashboard_preferences_json")
}

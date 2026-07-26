package com.hjw.qbremote.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.ServerBackendType

internal fun LazyListScope.settingsRootPageContent(
    state: MainUiState,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
    onCompletionNotificationsChange: (Boolean) -> Unit,
    onBackendTypeChange: (ServerBackendType) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onHttpsChange: (Boolean) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefreshSecondsChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    item {
        val context = LocalContext.current
        val notificationPermissionDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        SettingsPageContent(
            settings = state.settings,
            onAppLanguageChange = onAppLanguageChange,
            onDeleteFilesWhenNoSeedersChange = onDeleteFilesWhenNoSeedersChange,
            onDeleteFilesDefaultChange = onDeleteFilesDefaultChange,
            onCompletionNotificationsChange = onCompletionNotificationsChange,
            notificationPermissionDenied = notificationPermissionDenied,
            onOpenNotificationSettings = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                context.startActivity(intent)
            },
        )
    }
    if (state.serverProfiles.isEmpty()) {
        item {
            ConnectionCard(
                state = state,
                onBackendTypeChange = onBackendTypeChange,
                onHostChange = onHostChange,
                onPortChange = onPortChange,
                onHttpsChange = onHttpsChange,
                onUserChange = onUserChange,
                onPasswordChange = onPasswordChange,
                onRefreshSecondsChange = onRefreshSecondsChange,
                onConnect = onConnect,
            )
        }
    }
}

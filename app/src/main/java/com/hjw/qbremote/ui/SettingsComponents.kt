package com.hjw.qbremote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.connectionUsesInsecurePublicEndpoint
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor

@Composable
internal fun SettingsPanelCard(
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.35f)),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = { content() },
        )
    }
}

@Composable
internal fun SettingsPageContent(
    settings: ConnectionSettings,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
    onCompletionNotificationsChange: (Boolean) -> Unit,
    notificationPermissionDenied: Boolean,
    onOpenNotificationSettings: () -> Unit,
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var pendingAppLanguage by rememberSaveable { mutableStateOf(settings.appLanguage) }

    LaunchedEffect(settings.appLanguage) {
        pendingAppLanguage = settings.appLanguage
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsPanelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_language),
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { showLanguageMenu = true }) {
                        Text(appLanguageLabel(pendingAppLanguage))
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(appLanguageLabel(language)) },
                                onClick = {
                                    pendingAppLanguage = language
                                    showLanguageMenu = false
                                },
                            )
                        }
                    }
                }
                TextButton(
                    enabled = pendingAppLanguage != settings.appLanguage,
                    onClick = { onAppLanguageChange(pendingAppLanguage) },
                ) {
                    Text(stringResource(R.string.settings_language_save))
                }
            }
            Text(
                text = stringResource(R.string.settings_language_apply_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsPanelCard {
            SettingSwitchRow(
                title = stringResource(R.string.settings_delete_when_no_seeders),
                checked = settings.deleteFilesWhenNoSeeders,
                onCheckedChange = onDeleteFilesWhenNoSeedersChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_delete_by_default),
                checked = settings.deleteFilesDefault,
                onCheckedChange = onDeleteFilesDefaultChange,
            )
        }
        SettingsPanelCard {
            SettingSwitchRow(
                title = stringResource(R.string.settings_completion_notifications),
                checked = settings.completionNotificationsEnabled,
                onCheckedChange = onCompletionNotificationsChange,
            )
            if (notificationPermissionDenied) {
                Text(
                    text = stringResource(R.string.settings_notification_permission_denied),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onOpenNotificationSettings) {
                    Text(stringResource(R.string.settings_open_notification_settings))
                }
            }
        }
    }
}

@Composable
internal fun ConnectionCard(
    state: MainUiState,
    onBackendTypeChange: (ServerBackendType) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onHttpsChange: (Boolean) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefreshSecondsChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.connection_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.connection_backend_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ServerBackendType.QBITTORRENT to stringResource(R.string.backend_qbittorrent),
                        ServerBackendType.TRANSMISSION to stringResource(R.string.backend_transmission),
                    ).forEach { (backendType, label) ->
                        TextButton(
                            onClick = { onBackendTypeChange(backendType) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (state.settings.serverBackendType == backendType) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                            modifier = Modifier.background(
                                color = if (state.settings.serverBackendType == backendType) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
                                },
                                shape = RoundedCornerShape(12.dp),
                            ),
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.host,
                onValueChange = onHostChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_host_label)) },
                placeholder = { Text(stringResource(R.string.connection_host_hint)) },
                shape = RoundedCornerShape(14.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.width(120.dp),
                    value = if (state.settings.port == 0) "" else state.settings.port.toString(),
                    onValueChange = onPortChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_port_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(14.dp),
                )

                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.settings.refreshSeconds.toString(),
                    onValueChange = onRefreshSecondsChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_refresh_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.username,
                onValueChange = onUserChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_username_label)) },
                shape = RoundedCornerShape(14.dp),
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.password,
                onValueChange = onPasswordChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_password_label)) },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    PasswordVisibilityTrailingIcon(
                        passwordVisible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible },
                    )
                },
                shape = RoundedCornerShape(14.dp),
            )

            if (connectionUsesInsecurePublicEndpoint(state.settings)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.settings_cleartext_public_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.connection_https_label))
                Switch(
                    checked = state.settings.useHttps,
                    onCheckedChange = onHttpsChange,
                    modifier = Modifier.padding(start = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onConnect,
                        enabled = !state.isConnecting,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                            ),
                    ) {
                        Text(
                            if (state.isConnecting) {
                                stringResource(R.string.connecting)
                            } else {
                                stringResource(R.string.connect)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PasswordVisibilityTrailingIcon(
    passwordVisible: Boolean,
    onToggle: () -> Unit,
) {
    val contentDescription = if (passwordVisible) {
        stringResource(R.string.password_visibility_hide)
    } else {
        stringResource(R.string.password_visibility_show)
    }
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(
                id = if (passwordVisible) {
                    R.drawable.ic_password_visible
                } else {
                    R.drawable.ic_password_hidden
                }
            ),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .testTag("setting-switch:$title"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
internal fun appLanguageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        AppLanguage.ZH_CN -> stringResource(R.string.settings_language_zh_cn)
        AppLanguage.EN -> stringResource(R.string.settings_language_en)
    }
}

internal fun buildServerAddressText(settings: ConnectionSettings): String {
    val host = settings.host.trim().ifBlank { "-" }
    if (host.startsWith("http://", ignoreCase = true) || host.startsWith("https://", ignoreCase = true)) {
        return host
    }
    val scheme = if (settings.useHttps) "https" else "http"
    return "$scheme://$host:${settings.port}"
}

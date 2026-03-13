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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.ChartSortMode
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor

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
    onShowSpeedTotalsChange: (Boolean) -> Unit,
    onEnableServerGroupingChange: (Boolean) -> Unit,
    onShowChartPanelChange: (Boolean) -> Unit,
    onShowCountryFlowCardChange: (Boolean) -> Unit,
    onShowUploadDistributionCardChange: (Boolean) -> Unit,
    onShowCategoryDistributionCardChange: (Boolean) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
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
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_speed_totals),
                checked = settings.showSpeedTotals,
                onCheckedChange = onShowSpeedTotalsChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_enable_server_grouping),
                checked = settings.enableServerGrouping,
                onCheckedChange = onEnableServerGroupingChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_chart_panel),
                checked = settings.showChartPanel,
                onCheckedChange = onShowChartPanelChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_country_flow_card),
                checked = settings.showCountryFlowCard,
                onCheckedChange = onShowCountryFlowCardChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_upload_distribution_card),
                checked = settings.showUploadDistributionCard,
                onCheckedChange = onShowUploadDistributionCardChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.settings_show_category_distribution_card),
                checked = settings.showCategoryDistributionCard,
                onCheckedChange = onShowCategoryDistributionCardChange,
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
    }
}

@Composable
internal fun ConnectionCard(
    state: MainUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onHttpsChange: (Boolean) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefreshSecondsChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
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
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
            )

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
internal fun SettingsDialog(
    settings: ConnectionSettings,
    onDismiss: () -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowSpeedTotalsChange: (Boolean) -> Unit,
    onEnableServerGroupingChange: (Boolean) -> Unit,
    onShowChartPanelChange: (Boolean) -> Unit,
    onChartShowSiteNameChange: (Boolean) -> Unit,
    onChartSortModeChange: (ChartSortMode) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showChartSortMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PanelShape,
        containerColor = qbGlassStrongContainerColor(),
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                            Text(appLanguageLabel(settings.appLanguage))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                        ) {
                            AppLanguage.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(appLanguageLabel(language)) },
                                    onClick = {
                                        onAppLanguageChange(language)
                                        showLanguageMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_language_apply_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_speed_totals),
                    checked = settings.showSpeedTotals,
                    onCheckedChange = onShowSpeedTotalsChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_enable_server_grouping),
                    checked = settings.enableServerGrouping,
                    onCheckedChange = onEnableServerGroupingChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_chart_panel),
                    checked = settings.showChartPanel,
                    onCheckedChange = onShowChartPanelChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_site_name),
                    checked = settings.chartShowSiteName,
                    onCheckedChange = onChartShowSiteNameChange,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_chart_sort_mode),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { showChartSortMenu = true }) {
                            Text(chartSortModeLabel(settings.chartSortMode))
                        }
                        DropdownMenu(
                            expanded = showChartSortMenu,
                            onDismissRequest = { showChartSortMenu = false },
                        ) {
                            ChartSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(chartSortModeLabel(mode)) },
                                    onClick = {
                                        onChartSortModeChange(mode)
                                        showChartSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
    )
}

@Composable
internal fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun chartSortModeLabel(mode: ChartSortMode): String {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> stringResource(R.string.chart_sort_total_speed)
        ChartSortMode.DOWNLOAD_SPEED -> stringResource(R.string.chart_sort_download_speed)
        ChartSortMode.UPLOAD_SPEED -> stringResource(R.string.chart_sort_upload_speed)
        ChartSortMode.TORRENT_COUNT -> stringResource(R.string.chart_sort_torrent_count)
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

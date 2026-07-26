package com.hjw.qbremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hjw.qbremote.R
import com.hjw.qbremote.data.GlobalSpeedLimits
import com.hjw.qbremote.data.ScheduleDayPreset
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor

private data class GlobalSpeedLimitDraft(
    val downloadLimitKb: String,
    val uploadLimitKb: String,
    val alternativeDownloadLimitKb: String,
    val alternativeUploadLimitKb: String,
    val schedulerEnabled: Boolean,
    val scheduleStartHour: String,
    val scheduleStartMinute: String,
    val scheduleEndHour: String,
    val scheduleEndMinute: String,
    val scheduleDayPreset: ScheduleDayPreset,
) {
    companion object {
        fun from(limits: GlobalSpeedLimits): GlobalSpeedLimitDraft {
            return GlobalSpeedLimitDraft(
                downloadLimitKb = limits.downloadLimitKb.toString(),
                uploadLimitKb = limits.uploadLimitKb.toString(),
                alternativeDownloadLimitKb = limits.alternativeDownloadLimitKb.toString(),
                alternativeUploadLimitKb = limits.alternativeUploadLimitKb.toString(),
                schedulerEnabled = limits.schedulerEnabled,
                scheduleStartHour = (limits.scheduleStartMinutes / 60).toString(),
                scheduleStartMinute = (limits.scheduleStartMinutes % 60).toString(),
                scheduleEndHour = (limits.scheduleEndMinutes / 60).toString(),
                scheduleEndMinute = (limits.scheduleEndMinutes % 60).toString(),
                scheduleDayPreset = limits.scheduleDayPreset,
            )
        }
    }
}

@Composable
internal fun GlobalSpeedLimitDialog(
    profiles: List<ServerProfile>,
    selectedProfileId: String,
    limits: GlobalSpeedLimits?,
    isLoading: Boolean,
    isSaving: Boolean,
    loadFailed: Boolean,
    onProfileSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (GlobalSpeedLimits) -> Unit,
) {
    val drafts = remember { mutableStateMapOf<String, GlobalSpeedLimitDraft>() }
    LaunchedEffect(selectedProfileId, limits) {
        if (selectedProfileId.isNotBlank() && limits != null && selectedProfileId !in drafts) {
            drafts[selectedProfileId] = GlobalSpeedLimitDraft.from(limits)
        }
    }
    val currentDraft = drafts[selectedProfileId]
    fun updateDraft(transform: (GlobalSpeedLimitDraft) -> GlobalSpeedLimitDraft) {
        drafts[selectedProfileId]?.let { draft -> drafts[selectedProfileId] = transform(draft) }
    }
    val draft = buildGlobalSpeedLimitsOrNull(
        downloadLimitKb = currentDraft?.downloadLimitKb.orEmpty(),
        uploadLimitKb = currentDraft?.uploadLimitKb.orEmpty(),
        alternativeDownloadLimitKb = currentDraft?.alternativeDownloadLimitKb.orEmpty(),
        alternativeUploadLimitKb = currentDraft?.alternativeUploadLimitKb.orEmpty(),
        schedulerEnabled = currentDraft?.schedulerEnabled == true,
        scheduleStartHour = currentDraft?.scheduleStartHour.orEmpty(),
        scheduleStartMinute = currentDraft?.scheduleStartMinute.orEmpty(),
        scheduleEndHour = currentDraft?.scheduleEndHour.orEmpty(),
        scheduleEndMinute = currentDraft?.scheduleEndMinute.orEmpty(),
        scheduleDayPreset = currentDraft?.scheduleDayPreset ?: ScheduleDayPreset.EVERY_DAY,
        originalScheduleStartMinutes = limits?.scheduleStartMinutes ?: 0,
        originalScheduleEndMinutes = limits?.scheduleEndMinutes ?: 0,
        originalScheduleDayPreset = limits?.scheduleDayPreset ?: ScheduleDayPreset.EVERY_DAY,
    )

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .heightIn(min = 420.dp, max = 760.dp),
            shape = RoundedCornerShape(28.dp),
            color = qbGlassStrongContainerColor(),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.global_speed_limit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                ServerSelectorField(
                    profiles = profiles,
                    selectedProfileId = selectedProfileId,
                    enabled = !isSaving,
                    onProfileSelected = onProfileSelected,
                )

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when {
                    currentDraft == null && isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp)
                                .padding(vertical = 48.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    (loadFailed && currentDraft == null) || limits == null || currentDraft == null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.global_speed_limit_load_failed),
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = onRetry, enabled = !isSaving) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }

                    else -> {
                        if (loadFailed) {
                            Text(
                                text = stringResource(R.string.global_speed_limit_load_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        SpeedLimitSection(
                            title = stringResource(R.string.global_speed_limit_normal_section),
                            downloadValue = currentDraft.downloadLimitKb,
                            uploadValue = currentDraft.uploadLimitKb,
                            enabled = !isSaving,
                            onDownloadValueChange = { value ->
                                updateDraft { it.copy(downloadLimitKb = value) }
                            },
                            onUploadValueChange = { value ->
                                updateDraft { it.copy(uploadLimitKb = value) }
                            },
                        )
                        HorizontalDivider()
                        SpeedLimitSection(
                            title = stringResource(R.string.global_speed_limit_alternative_section),
                            downloadValue = currentDraft.alternativeDownloadLimitKb,
                            uploadValue = currentDraft.alternativeUploadLimitKb,
                            enabled = !isSaving,
                            onDownloadValueChange = { value ->
                                updateDraft { it.copy(alternativeDownloadLimitKb = value) }
                            },
                            onUploadValueChange = { value ->
                                updateDraft { it.copy(alternativeUploadLimitKb = value) }
                            },
                            alternative = true,
                        )
                        Text(
                            text = stringResource(R.string.global_speed_limit_unlimited_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(
                                when (limits.alternativeModeEnabled) {
                                    true -> R.string.global_speed_limit_mode_alternative
                                    false -> R.string.global_speed_limit_mode_normal
                                    null -> R.string.global_speed_limit_mode_unknown
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSaving) {
                                    updateDraft { it.copy(schedulerEnabled = !it.schedulerEnabled) }
                                },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.global_speed_limit_schedule_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.global_speed_limit_schedule_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = currentDraft.schedulerEnabled,
                                onCheckedChange = { value ->
                                    updateDraft { it.copy(schedulerEnabled = value) }
                                },
                                enabled = !isSaving,
                            )
                        }

                        if (currentDraft.schedulerEnabled) {
                            ScheduleTimeRow(
                                label = stringResource(R.string.global_speed_limit_schedule_start),
                                hour = currentDraft.scheduleStartHour,
                                minute = currentDraft.scheduleStartMinute,
                                enabled = !isSaving,
                                onHourChanged = { value ->
                                    updateDraft { it.copy(scheduleStartHour = value) }
                                },
                                onMinuteChanged = { value ->
                                    updateDraft { it.copy(scheduleStartMinute = value) }
                                },
                            )
                            ScheduleTimeRow(
                                label = stringResource(R.string.global_speed_limit_schedule_end),
                                hour = currentDraft.scheduleEndHour,
                                minute = currentDraft.scheduleEndMinute,
                                enabled = !isSaving,
                                onHourChanged = { value ->
                                    updateDraft { it.copy(scheduleEndHour = value) }
                                },
                                onMinuteChanged = { value ->
                                    updateDraft { it.copy(scheduleEndMinute = value) }
                                },
                            )
                            ScheduleDaySelector(
                                selected = currentDraft.scheduleDayPreset,
                                enabled = !isSaving,
                                onSelected = { value ->
                                    updateDraft { it.copy(scheduleDayPreset = value) }
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { draft?.let(onSave) },
                        enabled = limits != null && !isLoading && !isSaving && draft != null,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.server_save_action))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSelectorField(
    profiles: List<ServerProfile>,
    selectedProfileId: String,
    enabled: Boolean,
    onProfileSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedProfileId } ?: profiles.firstOrNull()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled && profiles.isNotEmpty()) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selected?.displayName().orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.global_speed_limit_server_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.displayName()) },
                    onClick = {
                        expanded = false
                        if (profile.id != selectedProfileId) onProfileSelected(profile.id)
                    },
                )
            }
        }
    }
}

private fun ServerProfile.displayName(): String = name.ifBlank {
    when (backendType) {
        ServerBackendType.QBITTORRENT -> "qBittorrent"
        ServerBackendType.TRANSMISSION -> "Transmission"
    }
}

@Composable
private fun SpeedLimitSection(
    title: String,
    downloadValue: String,
    uploadValue: String,
    enabled: Boolean,
    onDownloadValueChange: (String) -> Unit,
    onUploadValueChange: (String) -> Unit,
    alternative: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        GlobalSpeedLimitField(
            value = downloadValue,
            label = stringResource(
                if (alternative) {
                    R.string.global_speed_limit_alternative_download_label
                } else {
                    R.string.global_speed_limit_download_label
                },
            ),
            enabled = enabled,
            onValueChange = onDownloadValueChange,
        )
        GlobalSpeedLimitField(
            value = uploadValue,
            label = stringResource(
                if (alternative) {
                    R.string.global_speed_limit_alternative_upload_label
                } else {
                    R.string.global_speed_limit_upload_label
                },
            ),
            enabled = enabled,
            onValueChange = onUploadValueChange,
        )
    }
}

@Composable
private fun GlobalSpeedLimitField(
    value: String,
    label: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val valid = isValidGlobalSpeedLimitKbInput(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = !valid,
        supportingText = if (!valid) {
            { Text(stringResource(R.string.error_speed_limit_invalid)) }
        } else {
            null
        },
    )
}

@Composable
private fun ScheduleTimeRow(
    label: String,
    hour: String,
    minute: String,
    enabled: Boolean,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScheduleTimeField(
                value = hour,
                label = stringResource(R.string.global_speed_limit_schedule_hour),
                valid = isValidScheduleHourInput(hour),
                enabled = enabled,
                onValueChange = onHourChanged,
                modifier = Modifier.weight(1f),
            )
            Text(":", style = MaterialTheme.typography.titleLarge)
            ScheduleTimeField(
                value = minute,
                label = stringResource(R.string.global_speed_limit_schedule_minute),
                valid = isValidScheduleMinuteInput(minute),
                enabled = enabled,
                onValueChange = onMinuteChanged,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScheduleTimeField(
    value: String,
    label: String,
    valid: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = !valid,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDaySelector(
    selected: ScheduleDayPreset,
    enabled: Boolean,
    onSelected: (ScheduleDayPreset) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = stringResource(selected.labelRes()),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.global_speed_limit_schedule_day)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ScheduleDayPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(stringResource(preset.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelected(preset)
                    },
                )
            }
        }
    }
}

private fun ScheduleDayPreset.labelRes(): Int = when (this) {
    ScheduleDayPreset.EVERY_DAY -> R.string.global_speed_limit_day_every_day
    ScheduleDayPreset.WEEKDAYS -> R.string.global_speed_limit_day_weekdays
    ScheduleDayPreset.WEEKENDS -> R.string.global_speed_limit_day_weekends
    ScheduleDayPreset.MONDAY -> R.string.global_speed_limit_day_monday
    ScheduleDayPreset.TUESDAY -> R.string.global_speed_limit_day_tuesday
    ScheduleDayPreset.WEDNESDAY -> R.string.global_speed_limit_day_wednesday
    ScheduleDayPreset.THURSDAY -> R.string.global_speed_limit_day_thursday
    ScheduleDayPreset.FRIDAY -> R.string.global_speed_limit_day_friday
    ScheduleDayPreset.SATURDAY -> R.string.global_speed_limit_day_saturday
    ScheduleDayPreset.SUNDAY -> R.string.global_speed_limit_day_sunday
}

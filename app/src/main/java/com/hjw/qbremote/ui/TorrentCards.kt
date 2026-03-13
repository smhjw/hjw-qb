package com.hjw.qbremote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.hjw.qbremote.R
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor

@Composable
internal fun TorrentCard(
    torrent: TorrentInfo,
    crossSeedCount: Int,
    isPending: Boolean,
    onOpenDetails: () -> Unit,
) {
    val effectiveState = effectiveTorrentState(torrent)
    val stateLabel = localizedTorrentStateLabel(effectiveState)
    val categoryText = normalizeCategoryLabel(
        category = torrent.category,
        noCategoryText = stringResource(R.string.no_category),
    )
    val tagsText = compactTagsLabel(
        tags = torrent.tags,
        noTagsText = stringResource(R.string.no_tags),
    )
    val activeAgoText = formatActiveAgo(torrent.lastActivity)
    val addedOnText = formatAddedOn(torrent.addedOn)
    val savePathText = torrent.savePath.ifBlank { "-" }
    val stateStyle = torrentStateStyle(effectiveState)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPending) { onOpenDetails() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, stateStyle.borderColor.copy(alpha = 0.58f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassSubtleContainerColor(),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = torrent.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                )
            }

            TorrentMetaHeaderRow(
                tagsText = tagsText,
                crossSeedCount = crossSeedCount,
                stateLabel = stateLabel,
                stateStyle = stateStyle,
                addedOnText = addedOnText,
                activeAgoText = activeAgoText,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LinearProgressIndicator(
                    progress = { torrent.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f),
                    color = stateStyle.progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = formatPercent(torrent.progress),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = stateStyle.progressColor,
                )
            }

            TorrentQuickStatsRow(
                torrent = torrent,
                categoryText = categoryText,
                savePathText = savePathText,
                minHeight = 96.dp,
            )
        }
    }
}

@Composable
internal fun TorrentOperationDetailCard(
    torrent: TorrentInfo,
    crossSeedCount: Int,
    isPending: Boolean,
    detailLoading: Boolean,
    detailProperties: TorrentProperties?,
    detailFiles: List<TorrentFileInfo>,
    detailTrackers: List<com.hjw.qbremote.data.model.TorrentTracker>,
    categoryOptions: List<String>,
    tagOptions: List<String>,
    deleteFilesDefault: Boolean,
    deleteFilesWhenNoSeeders: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onSetLocation: (String) -> Unit,
    onSetCategory: (String) -> Unit,
    onSetTags: (String, String) -> Unit,
    onSetSpeedLimit: (String, String) -> Unit,
    onSetShareRatio: (String) -> Unit,
) {
    var showDeleteDialog by remember(torrent.hash) { mutableStateOf(false) }
    var deleteFilesChecked by remember(torrent.hash) { mutableStateOf(false) }

    var renameText by remember(torrent.hash) { mutableStateOf(torrent.name) }
    var locationText by remember(torrent.hash) {
        mutableStateOf(detailProperties?.savePath?.takeIf { it.isNotBlank() } ?: torrent.savePath)
    }
    var categoryTextInput by remember(torrent.hash) { mutableStateOf(torrent.category) }
    var tagsTextInput by remember(torrent.hash) { mutableStateOf(torrent.tags) }
    var downloadLimitText by remember(torrent.hash) { mutableStateOf("") }
    var uploadLimitText by remember(torrent.hash) { mutableStateOf("") }
    var ratioText by remember(torrent.hash) { mutableStateOf(formatRatio(torrent.ratio)) }

    var selectedTab by remember(torrent.hash) { mutableIntStateOf(0) }

    LaunchedEffect(torrent.hash, detailProperties?.downloadLimit, detailProperties?.uploadLimit) {
        val dl = detailProperties?.downloadLimit ?: 0L
        val up = detailProperties?.uploadLimit ?: 0L
        downloadLimitText = if (dl > 0L) (dl / 1024L).toString() else ""
        uploadLimitText = if (up > 0L) (up / 1024L).toString() else ""
    }
    LaunchedEffect(torrent.hash, detailProperties?.shareRatio) {
        val ratio = detailProperties?.shareRatio
        if (ratio != null && ratio >= 0.0 && ratio.isFinite()) {
            ratioText = formatRatio(ratio)
        }
    }

    val effectiveState = effectiveTorrentState(torrent)
    val paused = isPausedState(effectiveState)
    val stateLabel = localizedTorrentStateLabel(effectiveState)
    val stateStyle = torrentStateStyle(effectiveState)
    val tagsText = compactTagsLabel(
        tags = torrent.tags,
        noTagsText = stringResource(R.string.no_tags),
    )
    val addedOnText = formatAddedOn(torrent.addedOn)
    val activeAgoText = formatActiveAgo(torrent.lastActivity)
    val categoryText = normalizeCategoryLabel(
        category = torrent.category,
        noCategoryText = stringResource(R.string.no_category),
    )
    val savePathText = torrent.savePath.ifBlank { "-" }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.42f)),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = torrent.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            TabRow(selectedTabIndex = selectedTab) {
                listOf(
                    stringResource(R.string.tab_info),
                    stringResource(R.string.tab_trackers),
                    stringResource(R.string.tab_peers),
                    stringResource(R.string.tab_files),
                ).forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TorrentMetaHeaderRow(
                            tagsText = tagsText,
                            crossSeedCount = crossSeedCount,
                            stateLabel = stateLabel,
                            stateStyle = stateStyle,
                            addedOnText = addedOnText,
                            activeAgoText = activeAgoText,
                        )
                        TorrentInfoCell(
                            text = formatPercent(torrent.progress),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LinearProgressIndicator(
                            progress = { torrent.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = stateStyle.progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        TorrentQuickStatsRow(
                            torrent = torrent,
                            categoryText = categoryText,
                            savePathText = savePathText,
                            minHeight = 84.dp,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        Text(
                            stringResource(R.string.detail_section_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ActionInputRow(
                            label = stringResource(R.string.detail_new_name_label),
                            value = renameText,
                            onValueChange = { renameText = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onRename(renameText.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_path),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.detail_set_path_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ActionInputRow(
                            label = stringResource(R.string.detail_save_path_label),
                            value = locationText,
                            onValueChange = { locationText = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onSetLocation(locationText.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_category),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (categoryOptions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                items(categoryOptions, key = { it }) { option ->
                                    TorrentMetaChip(
                                        text = option,
                                        containerColor = if (option == categoryTextInput) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                        contentColor = Color(0xFFEAF0FF),
                                        onClick = { categoryTextInput = option },
                                    )
                                }
                            }
                        }
                        ActionInputRow(
                            label = stringResource(R.string.detail_category_label),
                            value = categoryTextInput,
                            onValueChange = { categoryTextInput = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onSetCategory(categoryTextInput.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_tags),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (tagOptions.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                items(tagOptions, key = { it }) { option ->
                                    val selected = parseTags(tagsTextInput).contains(option)
                                    TorrentMetaChip(
                                        text = option,
                                        containerColor = if (selected) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                                        contentColor = Color(0xFFEAF0FF),
                                        onClick = { tagsTextInput = toggleTag(tagsTextInput, option) },
                                    )
                                }
                            }
                        }
                        ActionInputRow(
                            label = stringResource(R.string.detail_tags_label),
                            value = tagsTextInput,
                            onValueChange = { tagsTextInput = it },
                            actionText = stringResource(R.string.detail_action_change),
                            enabled = !isPending,
                            onAction = { onSetTags(torrent.tags, tagsTextInput.trim()) },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_speed_limit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = downloadLimitText,
                                onValueChange = { downloadLimitText = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.detail_download_kb_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isPending,
                            )
                            OutlinedTextField(
                                value = uploadLimitText,
                                onValueChange = { uploadLimitText = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.detail_upload_kb_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isPending,
                            )
                            TextButton(
                                onClick = { onSetSpeedLimit(downloadLimitText, uploadLimitText) },
                                enabled = !isPending,
                                modifier = Modifier.background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                ),
                            ) {
                                Text(stringResource(R.string.detail_action_apply))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        Text(
                            stringResource(R.string.detail_section_ratio),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ActionInputRow(
                            label = stringResource(R.string.detail_ratio_label),
                            value = ratioText,
                            onValueChange = { ratioText = it },
                            actionText = stringResource(R.string.detail_action_apply),
                            enabled = !isPending,
                            onAction = { onSetShareRatio(ratioText.trim()) },
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    if (paused) onResume() else onPause()
                                },
                                enabled = !isPending,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                            ) {
                                Text(
                                    if (paused) {
                                        stringResource(R.string.resume)
                                    } else {
                                        stringResource(R.string.pause)
                                    }
                                )
                            }
                            TextButton(
                                onClick = {
                                    deleteFilesChecked = deleteFilesDefault ||
                                        (deleteFilesWhenNoSeeders && torrent.seeders <= 0)
                                    showDeleteDialog = true
                                },
                                enabled = !isPending,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                            ) {
                                Text(stringResource(R.string.delete))
                            }
                        }
                    }
                }

                1 -> {
                    if (detailLoading) {
                        Text(
                            stringResource(R.string.loading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TorrentMetaChip(
                            text = "🌐 ${detailTrackers.size}",
                            containerColor = Color(0xFF6C3FD3),
                            contentColor = Color.White,
                        )
                    }
                    if (detailTrackers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_tracker_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detailTrackers.forEach { tracker ->
                            TrackerInfoCard(tracker = tracker)
                        }
                    }
                }

                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_seed_count_fmt, torrent.seeders, torrent.numComplete),
                            modifier = Modifier.weight(1f),
                        )
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_peer_count_fmt, torrent.leechers, torrent.numIncomplete),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_cross_seed_chip_fmt, crossSeedCount),
                            modifier = Modifier.weight(1f),
                        )
                        TorrentInfoCell(
                            text = stringResource(R.string.torrent_ratio_fmt, formatRatio(torrent.ratio)),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    TorrentInfoCell(
                        text = stringResource(
                            R.string.recent_activity_fmt,
                            formatActiveAgo(torrent.lastActivity),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                3 -> {
                    if (detailLoading) {
                        Text(
                            stringResource(R.string.loading_files),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (detailFiles.isEmpty()) {
                        Text(
                            stringResource(R.string.no_file_details),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detailFiles.take(120).forEach { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = file.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatBytes(file.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = formatPercent(file.progress),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = PanelShape,
            containerColor = qbGlassStrongContainerColor(),
            title = { Text(stringResource(R.string.delete_torrent_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_torrent_desc))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = deleteFilesChecked,
                            onCheckedChange = { deleteFilesChecked = it },
                        )
                        Text(stringResource(R.string.delete_files))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(deleteFilesChecked)
                    },
                    enabled = !isPending,
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun ActionInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    actionText: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(label) },
            enabled = enabled,
        )
        TextButton(
            onClick = onAction,
            enabled = enabled,
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
            ),
        ) {
            Text(actionText)
        }
    }
}

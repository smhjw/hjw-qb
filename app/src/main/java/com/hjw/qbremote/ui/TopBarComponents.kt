package com.hjw.qbremote.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hjw.qbremote.R
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassChipColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor

@Composable
internal fun DrawerThemeItem(
    settings: ConnectionSettings,
    onThemeChange: (AppTheme) -> Unit,
    onApplyCustomTheme: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val currentThemeLabel = when (settings.appTheme) {
        AppTheme.DARK -> stringResource(R.string.theme_dark)
        AppTheme.LIGHT -> stringResource(R.string.theme_light)
        AppTheme.CUSTOM -> stringResource(R.string.theme_custom)
    }
    val saveFailedText = stringResource(R.string.theme_custom_save_failed)
    val savedImageValid = remember(settings.customBackgroundImagePath) {
        isCustomBackgroundFileValid(settings.customBackgroundImagePath)
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var customEditorVisible by rememberSaveable { mutableStateOf(settings.appTheme == AppTheme.CUSTOM) }
    var pendingImageUriText by rememberSaveable { mutableStateOf("") }
    var pendingImageName by rememberSaveable { mutableStateOf("") }
    var saveErrorText by rememberSaveable { mutableStateOf("") }
    val pendingImageUri = remember(pendingImageUriText) {
        pendingImageUriText.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImageUriText = uri.toString()
        pendingImageName = readDisplayName(context, uri)
        saveErrorText = ""
        customEditorVisible = true
    }

    LaunchedEffect(settings.appTheme) {
        if (settings.appTheme == AppTheme.CUSTOM) {
            customEditorVisible = true
        }
    }

    val statusText = when {
        pendingImageUri != null -> stringResource(R.string.theme_custom_pending)
        savedImageValid -> stringResource(R.string.theme_custom_saved)
        settings.customBackgroundImagePath.isNotBlank() -> stringResource(R.string.theme_custom_invalid)
        else -> stringResource(R.string.theme_custom_none)
    }
    val statusColor = if (
        settings.customBackgroundImagePath.isNotBlank() &&
        !savedImageValid &&
        pendingImageUri == null
    ) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.menu_theme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = currentThemeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (expanded) "^" else "v",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DrawerThemeChoiceItem(
                    label = stringResource(R.string.theme_dark),
                    selected = settings.appTheme == AppTheme.DARK,
                    onClick = {
                        saveErrorText = ""
                        onThemeChange(AppTheme.DARK)
                    },
                )
                DrawerThemeChoiceItem(
                    label = stringResource(R.string.theme_light),
                    selected = settings.appTheme == AppTheme.LIGHT,
                    onClick = {
                        saveErrorText = ""
                        onThemeChange(AppTheme.LIGHT)
                    },
                )
                DrawerThemeChoiceItem(
                    label = stringResource(R.string.theme_custom),
                    selected = settings.appTheme == AppTheme.CUSTOM,
                    onClick = {
                        customEditorVisible = true
                    },
                )

                if (customEditorVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .border(
                                width = 1.dp,
                                color = qbGlassOutlineColor(defaultAlpha = 0.24f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .background(
                                color = qbGlassSubtleContainerColor(),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                        )
                        if (pendingImageName.isNotBlank()) {
                            Text(
                                text = pendingImageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                onClick = {
                                    launcher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                            ) {
                                Text(
                                    if (savedImageValid || pendingImageUri != null) {
                                        stringResource(R.string.theme_custom_replace)
                                    } else {
                                        stringResource(R.string.theme_custom_upload)
                                    },
                                )
                            }
                            TextButton(
                                enabled = pendingImageUri != null || savedImageValid,
                                onClick = {
                                    saveErrorText = ""
                                    if (pendingImageUri != null) {
                                        runCatching {
                                            saveCustomBackgroundSelection(
                                                context = context,
                                                sourceUri = pendingImageUri,
                                            )
                                        }.onSuccess { saved ->
                                            pendingImageUriText = ""
                                            pendingImageName = ""
                                            onApplyCustomTheme(saved.filePath, saved.toneIsLight)
                                        }.onFailure { error ->
                                            saveErrorText = error.message
                                                ?.takeIf { it.isNotBlank() }
                                                ?: saveFailedText
                                        }
                                    } else if (savedImageValid) {
                                        onApplyCustomTheme(
                                            settings.customBackgroundImagePath,
                                            settings.customBackgroundToneIsLight,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                            ) {
                                Text(stringResource(R.string.theme_custom_save_apply))
                            }
                        }
                        if (saveErrorText.isNotBlank()) {
                            Text(
                                text = saveErrorText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerThemeChoiceItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        qbGlassChipColor()
    } else {
        qbGlassSubtleContainerColor()
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    } else {
        qbGlassOutlineColor(defaultAlpha = 0.24f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
internal fun TopBrandTitle() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f))
        )
        Text(
            text = "qbitremote",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.96f),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
internal fun NeedConnectionCard(
    onOpenConnection: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.connect_first_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onOpenConnection,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                ) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }

        }
    }
}

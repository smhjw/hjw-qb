package com.hjw.qbremote.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hjw.qbremote.R
import com.hjw.qbremote.data.GlobalSpeedLimits
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile

@Composable
internal fun MainScreenDialogs(
    pendingDeleteProfile: ServerProfile?,
    activeServerProfileId: String?,
    onDismissDeleteProfile: () -> Unit,
    onConfirmDeleteProfile: (ServerProfile) -> Unit,
    pendingBackendRepair: PendingBackendRepair?,
    onConfirmPendingBackendRepair: () -> Unit,
    onDismissPendingBackendRepair: () -> Unit,
    globalSpeedLimitDialogVisible: Boolean,
    serverProfiles: List<ServerProfile>,
    globalSpeedLimitProfileId: String,
    globalSpeedLimits: GlobalSpeedLimits?,
    globalSpeedLimitLoading: Boolean,
    globalSpeedLimitSaving: Boolean,
    globalSpeedLimitLoadFailed: Boolean,
    onGlobalSpeedLimitProfileSelected: (String) -> Unit,
    onRetryGlobalSpeedLimitLoad: () -> Unit,
    onDismissGlobalSpeedLimitDialog: () -> Unit,
    onSaveGlobalSpeedLimits: (GlobalSpeedLimits) -> Unit,
) {
    if (pendingDeleteProfile != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteProfile,
            title = { Text(stringResource(R.string.server_delete_title)) },
            text = {
                Text(
                    if (pendingDeleteProfile.id == activeServerProfileId) {
                        stringResource(
                            R.string.server_delete_desc_active,
                            pendingDeleteProfile.name,
                        )
                    } else {
                        stringResource(
                            R.string.server_delete_desc,
                            pendingDeleteProfile.name,
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteProfile(pendingDeleteProfile) },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteProfile) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingBackendRepair?.let { pendingRepair ->
        val detectedBackendLabel = when (pendingRepair.detectedBackend) {
            ServerBackendType.QBITTORRENT -> "qBittorrent"
            ServerBackendType.TRANSMISSION -> "Transmission"
        }
        AlertDialog(
            onDismissRequest = onDismissPendingBackendRepair,
            title = { Text(stringResource(R.string.server_backend_repair_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.server_backend_repair_desc,
                        pendingRepair.profileName,
                        detectedBackendLabel,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmPendingBackendRepair) {
                    Text(stringResource(R.string.server_backend_repair_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPendingBackendRepair) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (globalSpeedLimitDialogVisible) {
        GlobalSpeedLimitDialog(
            profiles = serverProfiles,
            selectedProfileId = globalSpeedLimitProfileId,
            limits = globalSpeedLimits,
            isLoading = globalSpeedLimitLoading,
            isSaving = globalSpeedLimitSaving,
            loadFailed = globalSpeedLimitLoadFailed,
            onProfileSelected = onGlobalSpeedLimitProfileSelected,
            onRetry = onRetryGlobalSpeedLimitLoad,
            onDismiss = onDismissGlobalSpeedLimitDialog,
            onSave = onSaveGlobalSpeedLimits,
        )
    }
}

package com.hjw.qbremote.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.AddTorrentFile
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenSheets(
    showServerProfileSheet: Boolean,
    serverProfileSheetState: SheetState,
    serverProfiles: List<ServerProfile>,
    activeServerProfileId: String?,
    serverSheetEditingProfileId: String,
    onDismissServerProfileSheet: () -> Unit,
    onSwitchServerProfile: (String) -> Unit,
    onAddServerProfile: (
        name: String,
        backendType: ServerBackendType,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ) -> Unit,
    onUpdateServerProfile: (
        profileId: String,
        name: String,
        backendType: ServerBackendType,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ) -> Unit,
    onRequestDeleteServerProfile: (String) -> Unit,
    showAddTorrentSheet: Boolean,
    addTorrentSheetState: SheetState,
    addTorrentCapabilities: ServerCapabilities,
    categoryOptionsForAdd: List<String>,
    tagOptionsForAdd: List<String>,
    pathOptionsForAdd: List<String>,
    addTorrentInitialUrls: String,
    onDismissAddTorrentSheet: () -> Unit,
    onCancelAddTorrent: () -> Unit,
    onAddTorrent: (
        urls: String,
        files: List<AddTorrentFile>,
        autoTmm: Boolean,
        category: String,
        tags: String,
        savePath: String,
        paused: Boolean,
        skipChecking: Boolean,
        sequentialDownload: Boolean,
        firstLastPiecePrio: Boolean,
        uploadLimitKb: String,
        downloadLimitKb: String,
    ) -> Unit,
    showDashboardCardManagerSheet: Boolean,
    dashboardCardManagerSheetState: SheetState,
    cardManagerProfileId: String?,
    availableDashboardCards: List<DashboardChartCard>,
    displayDashboardPreferences: ServerDashboardPreferences,
    onToggleDashboardCard: (String, DashboardChartCard, Boolean) -> Unit,
    onResetDashboardCards: (String) -> Unit,
    onDismissCardManagerSheet: () -> Unit,
) {
    if (showServerProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissServerProfileSheet,
            sheetState = serverProfileSheetState,
            containerColor = qbGlassStrongContainerColor(),
            shape = PanelShape,
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        ) {
            ServerProfileSheet(
                profiles = serverProfiles,
                activeProfileId = activeServerProfileId,
                initialEditingProfileId = serverSheetEditingProfileId.ifBlank { null },
                onSwitchProfile = onSwitchServerProfile,
                onAddProfile = onAddServerProfile,
                onUpdateProfile = onUpdateServerProfile,
                onRequestDeleteProfile = onRequestDeleteServerProfile,
                onCancel = onDismissServerProfileSheet,
            )
        }
    }

    if (showAddTorrentSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissAddTorrentSheet,
            sheetState = addTorrentSheetState,
            containerColor = qbGlassStrongContainerColor(),
            shape = PanelShape,
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        ) {
            AddTorrentSheet(
                context = LocalContext.current,
                capabilities = addTorrentCapabilities,
                categoryOptions = categoryOptionsForAdd,
                tagOptions = tagOptionsForAdd,
                pathOptions = pathOptionsForAdd,
                initialUrls = addTorrentInitialUrls,
                onCancel = onCancelAddTorrent,
                onAdd = onAddTorrent,
            )
        }
    }

    if (showDashboardCardManagerSheet && cardManagerProfileId != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissCardManagerSheet,
            sheetState = dashboardCardManagerSheetState,
            containerColor = qbGlassStrongContainerColor(),
            shape = PanelShape,
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        ) {
            ServerDashboardCardManagerSheet(
                availableCards = availableDashboardCards,
                preferences = displayDashboardPreferences,
                onToggleCard = { card, visible ->
                    onToggleDashboardCard(cardManagerProfileId, card, visible)
                },
                onReset = { onResetDashboardCards(cardManagerProfileId) },
                onDismiss = onDismissCardManagerSheet,
            )
        }
    }
}

package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ServerDashboardDisplaySelectorSupportTest {

    @Test
    fun buildServerDashboardDisplayInput_ignoresUnrelatedUiStateFields() {
        val base = MainUiState(
            settings = ConnectionSettings(serverBackendType = ServerBackendType.QBITTORRENT),
            serverProfiles = listOf(
                ServerProfile("alpha", "Alpha", ServerBackendType.QBITTORRENT, "alpha", 8080, false, "admin", 5),
            ),
            activeServerProfileId = "alpha",
            selectedDashboardProfileId = "alpha",
            dashboardServerSnapshots = listOf(
                CachedDashboardServerSnapshot(profileId = "alpha"),
            ),
            serverDashboardPreferences = mapOf(
                "alpha" to ServerDashboardPreferences(cardOrder = "country_flow"),
            ),
        )

        val changed = base.copy(
            errorMessage = UiMessage.Text("network error"),
            detailHash = "hash-1",
            pendingActionKeys = setOf("alpha|hash-1"),
            isConnecting = true,
        )

        assertEquals(
            buildServerDashboardDisplayInput(base),
            buildServerDashboardDisplayInput(changed),
        )
    }

    @Test
    fun buildServerDashboardDisplayInput_tracksDashboardRelevantState() {
        val base = MainUiState(
            settings = ConnectionSettings(serverBackendType = ServerBackendType.QBITTORRENT),
            serverProfiles = listOf(
                ServerProfile("alpha", "Alpha", ServerBackendType.QBITTORRENT, "alpha", 8080, false, "admin", 5),
                ServerProfile("beta", "Beta", ServerBackendType.TRANSMISSION, "beta", 9091, false, "admin", 5),
            ),
            activeServerProfileId = "alpha",
            selectedDashboardProfileId = "alpha",
            dashboardServerSnapshots = listOf(
                CachedDashboardServerSnapshot(profileId = "alpha"),
            ),
            serverDashboardPreferences = mapOf(
                "alpha" to ServerDashboardPreferences(cardOrder = "country_flow"),
            ),
        )

        val changed = base.copy(
            selectedDashboardProfileId = "beta",
        )

        assertNotEquals(
            buildServerDashboardDisplayInput(base),
            buildServerDashboardDisplayInput(changed),
        )
    }
}

package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerRefreshSchedulingSupportTest {

    @Test
    fun selectDueServerRefreshProfileIds_selectsDueAndMissingEntries() {
        val result = selectDueServerRefreshProfileIds(
            profiles = listOf(profile("due"), profile("missing")),
            nextRefreshAtByProfileId = mapOf("due" to 1_000L),
            inFlightProfileIds = emptySet(),
            heldProfileId = null,
            holdAllProfiles = false,
            now = 1_000L,
        )

        assertEquals(listOf("due", "missing"), result)
    }

    @Test
    fun selectDueServerRefreshProfileIds_skipsProfilesNotYetDue() {
        val result = selectDueServerRefreshProfileIds(
            profiles = listOf(profile("due"), profile("pending")),
            nextRefreshAtByProfileId = mapOf("due" to 5_000L, "pending" to 5_001L),
            inFlightProfileIds = emptySet(),
            heldProfileId = null,
            holdAllProfiles = false,
            now = 5_000L,
        )

        assertEquals(listOf("due"), result)
    }

    @Test
    fun selectDueServerRefreshProfileIds_skipsInFlightProfiles() {
        val result = selectDueServerRefreshProfileIds(
            profiles = listOf(profile("alpha"), profile("beta")),
            nextRefreshAtByProfileId = emptyMap(),
            inFlightProfileIds = setOf("alpha"),
            heldProfileId = null,
            holdAllProfiles = false,
            now = 10_000L,
        )

        assertEquals(listOf("beta"), result)
    }

    @Test
    fun selectDueServerRefreshProfileIds_skipsOnlyHeldProfile() {
        val result = selectDueServerRefreshProfileIds(
            profiles = listOf(profile("alpha"), profile("beta"), profile("gamma")),
            nextRefreshAtByProfileId = emptyMap(),
            inFlightProfileIds = emptySet(),
            heldProfileId = "beta",
            holdAllProfiles = false,
            now = 10_000L,
        )

        assertEquals(listOf("alpha", "gamma"), result)
    }

    @Test
    fun selectDueServerRefreshProfileIds_holdAllProfilesSkipsEverything() {
        val result = selectDueServerRefreshProfileIds(
            profiles = listOf(profile("alpha"), profile("beta")),
            nextRefreshAtByProfileId = emptyMap(),
            inFlightProfileIds = emptySet(),
            heldProfileId = null,
            holdAllProfiles = true,
            now = 10_000L,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun selectDueServerRefreshProfileIds_preservesProfileOrder() {
        val result = selectDueServerRefreshProfileIds(
            profiles = listOf(profile("gamma"), profile("alpha"), profile("beta")),
            nextRefreshAtByProfileId = emptyMap(),
            inFlightProfileIds = emptySet(),
            heldProfileId = null,
            holdAllProfiles = false,
            now = 10_000L,
        )

        assertEquals(listOf("gamma", "alpha", "beta"), result)
    }

    @Test
    fun nextServerRefreshDueAt_clampsRefreshSecondsBetween5And120() {
        assertEquals(10_000L + 5_000L, nextServerRefreshDueAt(now = 10_000L, refreshSeconds = 1))
        assertEquals(10_000L + 60_000L, nextServerRefreshDueAt(now = 10_000L, refreshSeconds = 60))
        assertEquals(10_000L + 120_000L, nextServerRefreshDueAt(now = 10_000L, refreshSeconds = 999))
    }

    private fun profile(id: String): ServerProfile {
        return ServerProfile(
            id = id,
            name = id,
            backendType = ServerBackendType.QBITTORRENT,
            host = "$id.local",
            port = 8080,
            useHttps = false,
            username = "admin",
            refreshSeconds = 5,
        )
    }
}

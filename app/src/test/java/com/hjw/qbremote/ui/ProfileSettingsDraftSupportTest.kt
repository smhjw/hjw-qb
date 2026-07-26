package com.hjw.qbremote.ui

import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsDraftSupportTest {

    @Test
    fun buildProfileSettingsDraft_normalizesFieldsAndClampsRefreshSeconds() {
        val draft = buildProfileSettingsDraft(
            baseSettings = ConnectionSettings(appTheme = AppTheme.LIGHT),
            backendType = ServerBackendType.QBITTORRENT,
            host = "  nas.local  ",
            port = "8081",
            useHttps = false,
            username = "  admin  ",
            password = "secret",
            refreshSeconds = "999",
        )

        assertEquals("nas.local", draft.host)
        assertEquals(8081, draft.port)
        assertFalse(draft.useHttps)
        assertEquals("admin", draft.username)
        assertEquals("secret", draft.password)
        assertEquals(ServerBackendType.QBITTORRENT, draft.serverBackendType)
        assertEquals(120, draft.refreshSeconds)
        assertEquals(AppTheme.LIGHT, draft.appTheme)
    }

    @Test
    fun buildProfileSettingsDraft_hostUrlHintsOverridePortAndScheme() {
        val draft = buildProfileSettingsDraft(
            baseSettings = ConnectionSettings(),
            backendType = ServerBackendType.QBITTORRENT,
            host = "https://nas.example.com:9443",
            port = "8080",
            useHttps = false,
            username = "admin",
            password = "",
            refreshSeconds = "5",
        )

        assertEquals("https://nas.example.com:9443", draft.host)
        assertTrue(draft.useHttps)
        assertEquals(9443, draft.port)
    }

    @Test
    fun buildProfileSettingsDraft_blankPortFallsBackToBackendDefault() {
        val qb = buildProfileSettingsDraft(
            baseSettings = ConnectionSettings(),
            backendType = ServerBackendType.QBITTORRENT,
            host = "qb.local",
            port = "",
            useHttps = false,
            username = "admin",
            password = "",
            refreshSeconds = "5",
        )
        val transmission = buildProfileSettingsDraft(
            baseSettings = ConnectionSettings(),
            backendType = ServerBackendType.TRANSMISSION,
            host = "tr.local",
            port = "",
            useHttps = false,
            username = "admin",
            password = "",
            refreshSeconds = "5",
        )

        assertEquals(8080, qb.port)
        assertEquals(9091, transmission.port)
    }

    @Test
    fun buildProfileSettingsDraft_clampsPortAndDefaultsInvalidRefreshSeconds() {
        val draft = buildProfileSettingsDraft(
            baseSettings = ConnectionSettings(),
            backendType = ServerBackendType.QBITTORRENT,
            host = "qb.local",
            port = "70000",
            useHttps = false,
            username = "admin",
            password = "",
            refreshSeconds = "abc",
        )

        assertEquals(65535, draft.port)
        assertEquals(5, draft.refreshSeconds)
    }

    @Test
    fun buildProfileSettingsDraft_rejectsBlankHost() {
        assertThrows(IllegalArgumentException::class.java) {
            buildProfileSettingsDraft(
                baseSettings = ConnectionSettings(),
                backendType = ServerBackendType.QBITTORRENT,
                host = "   ",
                port = "8080",
                useHttps = false,
                username = "admin",
                password = "",
                refreshSeconds = "5",
            )
        }
    }

    @Test
    fun buildProfileSettingsDraft_rejectsBlankUsername() {
        assertThrows(IllegalArgumentException::class.java) {
            buildProfileSettingsDraft(
                baseSettings = ConnectionSettings(),
                backendType = ServerBackendType.QBITTORRENT,
                host = "qb.local",
                port = "8080",
                useHttps = false,
                username = "   ",
                password = "",
                refreshSeconds = "5",
            )
        }
    }
}

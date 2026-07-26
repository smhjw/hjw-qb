package com.hjw.qbremote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerProfilesJsonParsingTest {
    @Test
    fun `corrupted json returns empty list`() {
        assertTrue(parseProfiles("{not-json").isEmpty())
        assertTrue(parseProfiles("[{]]").isEmpty())
        assertTrue(parseProfiles("{}").isEmpty())
        assertTrue(parseProfiles("null").isEmpty())
        assertTrue(parseProfiles(null).isEmpty())
        assertTrue(parseProfiles("   ").isEmpty())
    }

    @Test
    fun `entries missing id or host are dropped`() {
        val json = """[
            {"id":"a"},
            {"host":"h1"},
            {"id":"","host":"h1"},
            {"id":"b","host":"  "},
            {"id":"c","host":"h2"}
        ]"""
        assertEquals(listOf("c"), parseProfiles(json).map { it.id })
    }

    @Test
    fun `duplicate ids keep the first entry`() {
        val json = """[
            {"id":"a","host":"first"},
            {"id":"a","host":"second"}
        ]"""
        val profiles = parseProfiles(json)
        assertEquals(1, profiles.size)
        assertEquals("first", profiles.single().host)
    }

    @Test
    fun `invalid or blank backend type falls back to qbittorrent`() {
        val json = """[
            {"id":"a","host":"h1","backendType":"FOO"},
            {"id":"b","host":"h2","backendType":""}
        ]"""
        assertEquals(
            listOf(ServerBackendType.QBITTORRENT, ServerBackendType.QBITTORRENT),
            parseProfiles(json).map { it.backendType },
        )
    }

    @Test
    fun `numeric fields clamp and blank fields fall back`() {
        val clamped = parseProfiles(
            """[{"id":"a","host":" nas.local ","name":"  ","username":"","port":99999,"refreshSeconds":1}]"""
        ).single()
        assertEquals("nas.local", clamped.host)
        assertEquals("nas.local", clamped.name)
        assertEquals("admin", clamped.username)
        assertEquals(65535, clamped.port)
        assertEquals(5, clamped.refreshSeconds)

        val clampedLow = parseProfiles(
            """[{"id":"b","host":"h","port":0,"refreshSeconds":999}]"""
        ).single()
        assertEquals(1, clampedLow.port)
        assertEquals(120, clampedLow.refreshSeconds)

        val defaults = parseProfiles("""[{"id":"c","host":"h"}]""").single()
        assertEquals(8080, defaults.port)
        assertEquals(5, defaults.refreshSeconds)
        assertEquals(false, defaults.useHttps)
        assertEquals("admin", defaults.username)
    }
}

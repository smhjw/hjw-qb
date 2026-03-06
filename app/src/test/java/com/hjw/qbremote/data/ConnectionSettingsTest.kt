package com.hjw.qbremote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionSettingsTest {

    @Test
    fun baseUrlCandidates_buildsHttpUrlFromHostAndPort() {
        val settings = ConnectionSettings(
            host = "192.168.1.12",
            port = 8080,
            useHttps = false,
        )

        assertEquals(listOf("http://192.168.1.12:8080/"), settings.baseUrlCandidates())
    }

    @Test
    fun baseUrlCandidates_buildsHttpsUrlWithPathAndFallbackPort() {
        val settings = ConnectionSettings(
            host = "https://qb.example.com/webui/",
            port = 8080,
            useHttps = false,
        )

        val candidates = settings.baseUrlCandidates()
        assertEquals("https://qb.example.com:8080/webui/", candidates[0])
        assertEquals("https://qb.example.com:443/webui/", candidates[1])
        assertEquals(2, candidates.size)
    }

    @Test
    fun baseUrlCandidates_rejectsUnsupportedScheme() {
        val settings = ConnectionSettings(
            host = "ftp://qb.example.com",
            port = 8080,
            useHttps = false,
        )

        val error = runCatching { settings.baseUrlCandidates() }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("http/https") == true)
    }
}

package com.hjw.qbremote.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostClassificationTest {
    @Test
    fun `private hosts are classified private`() {
        listOf(
            "localhost",
            "LOCALHOST",
            "nas.local",
            "router.lan",
            "svc.internal",
            "gw.home.arpa",
            "127.0.0.1",
            "10.0.0.5",
            "172.16.0.1",
            "172.31.255.255",
            "192.168.1.1",
            "169.254.10.10",
            "::1",
            "[::1]",
            "fe80::1",
            "fc00::1234",
            "fd12:3456::1",
        ).forEach { host ->
            assertTrue(host, isLikelyPrivateHost(host))
        }
    }

    @Test
    fun `public or invalid hosts are not classified private`() {
        listOf(
            "",
            "   ",
            "example.com",
            "mylocal.com",
            "8.8.8.8",
            "11.0.0.1",
            "172.15.0.1",
            "172.32.0.1",
            "193.168.1.1",
            "256.1.1.1",
            "1.2.3",
            "2001:4860:4860::8888",
        ).forEach { host ->
            assertFalse(host, isLikelyPrivateHost(host))
        }
    }

    @Test
    fun `plain http to public host is insecure`() {
        assertTrue(
            connectionUsesInsecurePublicEndpoint(
                ConnectionSettings(host = "example.com", useHttps = false)
            )
        )
    }

    @Test
    fun `https to public host is not flagged`() {
        assertFalse(
            connectionUsesInsecurePublicEndpoint(
                ConnectionSettings(host = "example.com", useHttps = true)
            )
        )
    }

    @Test
    fun `plain http to private host is not flagged`() {
        assertFalse(
            connectionUsesInsecurePublicEndpoint(
                ConnectionSettings(host = "192.168.1.10", useHttps = false)
            )
        )
    }

    @Test
    fun `invalid host is not flagged`() {
        assertFalse(connectionUsesInsecurePublicEndpoint(ConnectionSettings(host = "")))
    }
}

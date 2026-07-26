package com.hjw.qbremote.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackendContractTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `qB login contract accepts session cookie and preserves WebUI headers`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "SID=test-session; HttpOnly")
                .setBody("Ok.")
        )
        val repository = QbRepository()

        val result = repository.connect(testSettings(ServerBackendType.QBITTORRENT))

        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("/api/v2/auth/login", request.path)
        assertNotNull(request.getHeader("Origin"))
        assertNotNull(request.getHeader("Referer"))
    }

    @Test
    fun `repository reuses an unchanged profile session without logging in again`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "SID=reused-session; HttpOnly")
                .setBody("Ok.")
        )
        val repository = TorrentRepository()
        val settings = testSettings(ServerBackendType.QBITTORRENT)

        assertTrue(repository.connect("profile", settings).isSuccess)
        assertTrue(repository.connect("profile", settings).isSuccess)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `Transmission 409 handshake retries once with returned session id`() = runBlocking {
        var requestCount = 0
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestCount += 1
                return if (requestCount == 1) {
                    MockResponse()
                        .setResponseCode(409)
                        .addHeader("X-Transmission-Session-Id", "session-123")
                } else {
                    MockResponse().setResponseCode(200).setBody(
                        """{"arguments":{"version":"4.0.6","rpc-version":17,"download-dir":"/tmp"},"result":"success"}"""
                    )
                }
            }
        }
        val backend = TransmissionBackend()

        val result = backend.connect(testSettings(ServerBackendType.TRANSMISSION))

        assertTrue(result.isSuccess)
        assertEquals(2, requestCount)
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertEquals("/transmission/rpc", first.path)
        assertEquals("session-123", second.getHeader("X-Transmission-Session-Id"))
    }

    @Test
    fun `Transmission does not loop forever when refreshed session id is rejected`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse()
                    .setResponseCode(409)
                    .addHeader("X-Transmission-Session-Id", "still-invalid")
            }
        }
        val backend = TransmissionBackend()

        val result = backend.connect(testSettings(ServerBackendType.TRANSMISSION))

        assertTrue(result.isFailure)
        assertEquals(8, server.requestCount)
    }

    private fun testSettings(backendType: ServerBackendType): ConnectionSettings {
        return ConnectionSettings(
            host = server.hostName,
            port = server.port,
            useHttps = false,
            username = "admin",
            password = "secret",
            serverBackendType = backendType,
        )
    }
}

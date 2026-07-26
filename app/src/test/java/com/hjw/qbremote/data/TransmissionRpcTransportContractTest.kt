package com.hjw.qbremote.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransmissionRpcTransportContractTest {
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
    fun `redirect on rpc post is not followed and probing moves to next candidate`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/transmission/rpc" -> MockResponse()
                        .setResponseCode(301)
                        .addHeader("Location", "/transmission/web/")
                    "/rpc" ->
                        if (request.getHeader("X-Transmission-Session-Id") != "session-a") {
                            MockResponse()
                                .setResponseCode(409)
                                .addHeader("X-Transmission-Session-Id", "session-a")
                        } else {
                            MockResponse().setResponseCode(200).setBody(SESSION_GET_BODY)
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val backend = TransmissionBackend()

        backend.connect(testSettings()).getOrThrow()

        assertEquals("4.0.6", backend.fetchServerVersion().getOrThrow())
        val redirectProbe = server.takeRequest()
        assertEquals("/transmission/rpc", redirectProbe.path)
        assertEquals("POST", redirectProbe.method)
        val fallbackProbe = server.takeRequest()
        assertEquals("/rpc", fallbackProbe.path)
    }

    @Test
    fun `all candidates redirecting surfaces redirect summary instead of html confusion`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "/transmission/web/")
            }
        }
        val backend = TransmissionBackend()

        val result = backend.connect(testSettings())

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("Redirected to"))
        assertTrue(message, message.contains("/transmission/web/"))
    }

    @Test
    fun `bom prefixed json body still parses`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/transmission/rpc" ->
                        if (request.getHeader("X-Transmission-Session-Id") != "session-a") {
                            MockResponse()
                                .setResponseCode(409)
                                .addHeader("X-Transmission-Session-Id", "session-a")
                        } else {
                            MockResponse().setResponseCode(200).setBody("\uFEFF" + SESSION_GET_BODY)
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val backend = TransmissionBackend()

        backend.connect(testSettings()).getOrThrow()

        assertEquals("4.0.6", backend.fetchServerVersion().getOrThrow())
    }

    @Test
    fun `empty 2xx body reports empty response instead of non-json`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/transmission/rpc" ->
                        if (request.getHeader("X-Transmission-Session-Id") != "session-a") {
                            MockResponse()
                                .setResponseCode(409)
                                .addHeader("X-Transmission-Session-Id", "session-a")
                        } else {
                            MockResponse().setResponseCode(200).setBody("")
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val backend = TransmissionBackend()

        val result = backend.connect(testSettings())

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("Empty response body"))
    }

    private fun testSettings(): ConnectionSettings {
        return ConnectionSettings(
            host = server.hostName,
            port = server.port,
            useHttps = false,
            username = "admin",
            password = "secret",
            serverBackendType = ServerBackendType.TRANSMISSION,
        )
    }

    private companion object {
        const val SESSION_GET_BODY =
            """{"result":"success","arguments":{"version":"4.0.6","download-dir":"/downloads"}}"""
    }
}

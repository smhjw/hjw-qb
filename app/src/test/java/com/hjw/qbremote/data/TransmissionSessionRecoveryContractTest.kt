package com.hjw.qbremote.data

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransmissionSessionRecoveryContractTest {
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
    fun `mid-session 409 refreshes session id and keeps it for later calls`() = runBlocking {
        val validSessionId = AtomicReference("session-a")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.getHeader("X-Transmission-Session-Id") != validSessionId.get()) {
                    MockResponse()
                        .setResponseCode(409)
                        .addHeader("X-Transmission-Session-Id", validSessionId.get())
                } else {
                    MockResponse().setResponseCode(200).setBody(SESSION_GET_BODY)
                }
            }
        }
        val backend = TransmissionBackend()
        backend.connect(testSettings()).getOrThrow()
        validSessionId.set("session-b")

        assertEquals("4.0.6", backend.fetchServerVersion().getOrThrow())
        assertEquals("4.0.6", backend.fetchServerVersion().getOrThrow())

        assertEquals(5, server.requestCount)
        assertNull(server.takeRequest().getHeader("X-Transmission-Session-Id"))
        assertEquals("session-a", server.takeRequest().getHeader("X-Transmission-Session-Id"))
        assertEquals("session-a", server.takeRequest().getHeader("X-Transmission-Session-Id"))
        assertEquals("session-b", server.takeRequest().getHeader("X-Transmission-Session-Id"))
        assertEquals("session-b", server.takeRequest().getHeader("X-Transmission-Session-Id"))
    }

    @Test
    fun `401 after connect maps to AuthFailed`() = runBlocking {
        val unauthorized = AtomicBoolean(false)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (unauthorized.get()) {
                    MockResponse().setResponseCode(401)
                } else {
                    MockResponse().setResponseCode(200).setBody(SESSION_GET_BODY)
                }
            }
        }
        val backend = TransmissionBackend()
        backend.connect(testSettings()).getOrThrow()
        unauthorized.set(true)

        val result = backend.fetchServerVersion()

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is BackendConnectionError.AuthFailed)
        assertTrue(error!!.message!!.contains("401"))
    }

    @Test
    fun `rpc error result surfaces the error summary`() = runBlocking {
        val failing = AtomicBoolean(false)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (failing.get()) {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("""{"result":"invalid argument","arguments":{}}""")
                } else {
                    MockResponse().setResponseCode(200).setBody(SESSION_GET_BODY)
                }
            }
        }
        val backend = TransmissionBackend()
        backend.connect(testSettings()).getOrThrow()
        failing.set(true)

        val result = backend.fetchServerVersion()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("invalid argument"))
    }

    @Test
    fun `rpc path falls back to next candidate and promotes it, all misses map to RpcPathNotFound`() = runBlocking {
        val allNotFound = AtomicBoolean(false)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (!allNotFound.get() && request.path == "/rpc") {
                    MockResponse().setResponseCode(200).setBody(SESSION_GET_BODY)
                } else {
                    MockResponse().setResponseCode(404)
                }
            }
        }
        val backend = TransmissionBackend()
        backend.connect(testSettings()).getOrThrow()
        backend.fetchServerVersion().getOrThrow()

        assertEquals(3, server.requestCount)
        assertEquals("/transmission/rpc", server.takeRequest().path)
        assertEquals("/rpc", server.takeRequest().path)
        assertEquals("/rpc", server.takeRequest().path)

        allNotFound.set(true)
        val result = TransmissionBackend().connect(testSettings())

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as BackendConnectionError.RpcPathNotFound
        assertTrue(error.attempts.contains(rpcUrl("/transmission/rpc")))
        assertTrue(error.attempts.contains(rpcUrl("/rpc")))
        assertTrue(error.failureSummary.contains(rpcUrl("/transmission/rpc")))
        assertTrue(error.failureSummary.contains(rpcUrl("/rpc")))
        assertTrue(error.failureSummary.contains("HTTP 404"))
    }

    @Test
    fun `fetchGlobalSpeedLimits maps session-get fields`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setResponseCode(200).setBody(SPEED_SESSION_BODY)
            }
        }
        val backend = TransmissionBackend()
        backend.connect(testSettings()).getOrThrow()

        val limits = backend.fetchGlobalSpeedLimits().getOrThrow()

        assertEquals(0L, limits.downloadLimitKb)
        assertEquals(512L, limits.uploadLimitKb)
        assertEquals(1024L, limits.alternativeDownloadLimitKb)
        assertEquals(2048L, limits.alternativeUploadLimitKb)
        assertTrue(limits.schedulerEnabled)
        assertEquals(510, limits.scheduleStartMinutes)
        assertEquals(1395, limits.scheduleEndMinutes)
        assertEquals(ScheduleDayPreset.WEEKDAYS, limits.scheduleDayPreset)
        assertEquals(true, limits.alternativeModeEnabled)
    }

    private fun rpcUrl(path: String): String {
        return "http://${server.hostName}:${server.port}$path"
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
        const val SPEED_SESSION_BODY =
            """{"result":"success","arguments":{"version":"4.0.6","download-dir":"/downloads","speed-limit-down-enabled":false,"speed-limit-down":3072,"speed-limit-up-enabled":true,"speed-limit-up":512,"alt-speed-down":1024,"alt-speed-up":2048,"alt-speed-enabled":true,"alt-speed-time-enabled":true,"alt-speed-time-begin":510,"alt-speed-time-end":1395,"alt-speed-time-day":62}}"""
    }
}

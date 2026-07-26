package com.hjw.qbremote.data

import java.util.concurrent.atomic.AtomicInteger
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
import retrofit2.HttpException

class QbRepositoryRetryContractTest {
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
    fun `403 on read endpoint triggers relogin and retry`() = runBlocking {
        val infoCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/transfer/info" ->
                        if (infoCalls.incrementAndGet() == 1) {
                            MockResponse().setResponseCode(403)
                        } else {
                            MockResponse().setResponseCode(200).setBody(TRANSFER_INFO_BODY)
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.fetchTransferInfo()

        assertTrue(result.isSuccess)
        assertEquals(123L, result.getOrThrow().downloadSpeed)
        val paths = (1..4).map { server.takeRequest().path }
        assertEquals(
            listOf(
                "/api/v2/auth/login",
                "/api/v2/transfer/info",
                "/api/v2/auth/login",
                "/api/v2/transfer/info",
            ),
            paths,
        )
    }

    @Test
    fun `5xx is retried with backoff until success`() = runBlocking {
        val infoCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/transfer/info" ->
                        if (infoCalls.incrementAndGet() == 1) {
                            MockResponse().setResponseCode(500)
                        } else {
                            MockResponse().setResponseCode(200).setBody(TRANSFER_INFO_BODY)
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.fetchTransferInfo()

        assertTrue(result.isSuccess)
        assertEquals(2, infoCalls.get())
    }

    @Test
    fun `non-retryable 4xx fails immediately without retry`() = runBlocking {
        val infoCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/transfer/info" -> {
                        infoCalls.incrementAndGet()
                        MockResponse().setResponseCode(400)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.fetchTransferInfo()

        assertTrue(result.isFailure)
        assertEquals(400, (result.exceptionOrNull() as HttpException).code())
        assertEquals(1, infoCalls.get())
    }

    @Test
    fun `persistent 5xx gives up after three attempts`() = runBlocking {
        val infoCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/transfer/info" -> {
                        infoCalls.incrementAndGet()
                        MockResponse().setResponseCode(500)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.fetchTransferInfo()

        assertTrue(result.isFailure)
        assertEquals(500, (result.exceptionOrNull() as HttpException).code())
        assertEquals(3, infoCalls.get())
    }

    @Test
    fun `write endpoint 401 triggers relogin and retry`() = runBlocking {
        val loginCalls = AtomicInteger(0)
        val deleteCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> {
                        loginCalls.incrementAndGet()
                        loginOkResponse()
                    }
                    "/api/v2/torrents/delete" ->
                        if (deleteCalls.incrementAndGet() == 1) {
                            MockResponse().setResponseCode(401)
                        } else {
                            MockResponse().setResponseCode(200)
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.deleteTorrent("hash-1", deleteFiles = false)

        assertTrue(result.isSuccess)
        assertEquals(2, loginCalls.get())
        assertEquals(2, deleteCalls.get())
    }

    @Test
    fun `stop 404 falls back to legacy pause endpoint`() = runBlocking {
        val stopCalls = AtomicInteger(0)
        val pauseCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/torrents/stop" -> {
                        stopCalls.incrementAndGet()
                        MockResponse().setResponseCode(404)
                    }
                    "/api/v2/torrents/pause" -> {
                        pauseCalls.incrementAndGet()
                        MockResponse().setResponseCode(200)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.pauseTorrent("hash-1")

        assertTrue(result.isSuccess)
        assertEquals(1, stopCalls.get())
        assertEquals(1, pauseCalls.get())
    }

    @Test
    fun `start 405 falls back to legacy resume endpoint`() = runBlocking {
        val startCalls = AtomicInteger(0)
        val resumeCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/torrents/start" -> {
                        startCalls.incrementAndGet()
                        MockResponse().setResponseCode(405)
                    }
                    "/api/v2/torrents/resume" -> {
                        resumeCalls.incrementAndGet()
                        MockResponse().setResponseCode(200)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.resumeTorrent("hash-1")

        assertTrue(result.isSuccess)
        assertEquals(1, startCalls.get())
        assertEquals(1, resumeCalls.get())
    }

    private fun loginOkResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie", "SID=test-session; HttpOnly")
            .setBody("Ok.")
    }

    private fun testSettings(): ConnectionSettings {
        return ConnectionSettings(
            host = server.hostName,
            port = server.port,
            useHttps = false,
            username = "admin",
            password = "secret",
            serverBackendType = ServerBackendType.QBITTORRENT,
        )
    }

    private companion object {
        const val TRANSFER_INFO_BODY =
            """{"dl_info_speed":123,"up_info_speed":456,"dl_info_data":1000,"up_info_data":2000,"free_space_on_disk":5000,"dht_nodes":3}"""
    }
}

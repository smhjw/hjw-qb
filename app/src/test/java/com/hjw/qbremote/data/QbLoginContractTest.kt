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

class QbLoginContractTest {
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
    fun `v2 login 404 falls back to legacy login endpoint`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> MockResponse().setResponseCode(404)
                    "/login" -> MockResponse()
                        .setResponseCode(200)
                        .addHeader("Set-Cookie", "SID=legacy-session; HttpOnly")
                        .setBody("Ok.")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()

        val result = repository.connect(testSettings())

        assertTrue(result.isSuccess)
        assertEquals("/api/v2/auth/login", server.takeRequest().path)
        assertEquals("/login", server.takeRequest().path)
    }

    @Test
    fun `Fails response is reported as credential rejection`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setResponseCode(200).setBody("Fails.")
            }
        }
        val repository = QbRepository()

        val result = repository.connect(testSettings())

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()!!.message!!.contains("Credential rejected")
        )
    }

    @Test
    fun `ip ban fails fast without trying more candidates`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse()
                    .setResponseCode(403)
                    .setBody("Your IP address has been banned after too many failed authentication attempts.")
            }
        }
        val repository = QbRepository()

        val result = repository.connect(testSettings())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("封禁"))
        assertEquals(1, server.requestCount)
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
}

package com.hjw.qbremote.data

import com.google.gson.JsonParser
import java.net.URLDecoder
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

class QbGlobalSpeedLimitContractTest {
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
    fun `fetch converts limit bytes to KiB and composes schedule minutes`() = runBlocking {
        server.dispatcher = dispatcherWithSpeedLimitsMode(
            MockResponse().setResponseCode(200).setBody("1")
        )
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val limits = repository.fetchGlobalSpeedLimits().getOrThrow()

        assertEquals(3072L, limits.downloadLimitKb)
        assertEquals(0L, limits.uploadLimitKb)
        assertEquals(1024L, limits.alternativeDownloadLimitKb)
        assertEquals(2048L, limits.alternativeUploadLimitKb)
        assertTrue(limits.schedulerEnabled)
        assertEquals(510, limits.scheduleStartMinutes)
        assertEquals(1395, limits.scheduleEndMinutes)
        assertEquals(ScheduleDayPreset.WEEKDAYS, limits.scheduleDayPreset)
        assertEquals(true, limits.alternativeModeEnabled)
    }

    @Test
    fun `speedLimitsMode failure maps alternative mode to null but fetch still succeeds`() = runBlocking {
        server.dispatcher = dispatcherWithSpeedLimitsMode(
            MockResponse().setResponseCode(500)
        )
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        val result = repository.fetchGlobalSpeedLimits()

        assertTrue(result.isSuccess)
        val limits = result.getOrThrow()
        assertNull(limits.alternativeModeEnabled)
        assertEquals(3072L, limits.downloadLimitKb)
    }

    @Test
    fun `set writes limits back as bytes and splits schedule minutes`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/app/setPreferences" -> MockResponse().setResponseCode(200)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = QbRepository()
        repository.connect(testSettings()).getOrThrow()

        repository.setGlobalSpeedLimits(
            GlobalSpeedLimits(
                downloadLimitKb = 3072L,
                uploadLimitKb = 256L,
                alternativeDownloadLimitKb = 1024L,
                alternativeUploadLimitKb = 2048L,
                schedulerEnabled = true,
                scheduleStartMinutes = 510,
                scheduleEndMinutes = 1395,
                scheduleDayPreset = ScheduleDayPreset.WEEKDAYS,
            )
        ).getOrThrow()

        server.takeRequest()
        val setRequest = server.takeRequest()
        assertEquals("/api/v2/app/setPreferences", setRequest.path)
        val encodedJson = setRequest.body.readUtf8().substringAfter("json=")
        val payload = JsonParser.parseString(URLDecoder.decode(encodedJson, "UTF-8")).asJsonObject
        assertEquals(3145728L, payload.get("dl_limit").asLong)
        assertEquals(262144L, payload.get("up_limit").asLong)
        assertEquals(1048576L, payload.get("alt_dl_limit").asLong)
        assertEquals(2097152L, payload.get("alt_up_limit").asLong)
        assertTrue(payload.get("scheduler_enabled").asBoolean)
        assertEquals(8, payload.get("schedule_from_hour").asInt)
        assertEquals(30, payload.get("schedule_from_min").asInt)
        assertEquals(23, payload.get("schedule_to_hour").asInt)
        assertEquals(15, payload.get("schedule_to_min").asInt)
        assertEquals(ScheduleDayPreset.WEEKDAYS.qbSchedulerDays, payload.get("scheduler_days").asInt)
    }

    private fun dispatcherWithSpeedLimitsMode(modeResponse: MockResponse): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v2/auth/login" -> loginOkResponse()
                    "/api/v2/app/preferences" -> MockResponse()
                        .setResponseCode(200)
                        .setBody(PREFERENCES_BODY)
                    "/api/v2/transfer/speedLimitsMode" -> modeResponse
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
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
        const val PREFERENCES_BODY =
            """{"dl_limit":3145728,"up_limit":0,"alt_dl_limit":1048576,"alt_up_limit":2097152,"scheduler_enabled":true,"schedule_from_hour":8,"schedule_from_min":30,"schedule_to_hour":23,"schedule_to_min":15,"scheduler_days":1}"""
    }
}

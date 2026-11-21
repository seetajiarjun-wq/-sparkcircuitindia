package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.EspNetworkClient
import com.example.sensors.EspButtonState
import com.example.sensors.EspConnectionStatus
import com.example.sensors.SensorMonitorRepository
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Esp8266IntegrationTest {

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private val sensorJsonPayload = AtomicReference(
        """
        {
          "distance": 152,
          "water": 1024,
          "waterDetected": false,
          "vib": false,
          "obstacleAlert": false,
          "alert": "NONE",
          "button": "NONE",
          "lcd": "System Start Ho Gaya",
          "connected": true
        }
        """.trimIndent()
    )
    private val clearButtonCalled = AtomicBoolean(false)
    private val lastAnswerQuery = AtomicReference<String?>(null)

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/sensors") { exchange ->
            val bytes = sensorJsonPayload.get().toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/clearButton") { exchange ->
            clearButtonCalled.set(true)
            val bytes = "OK".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/answer") { exchange ->
            lastAnswerQuery.set(exchange.requestURI.query)
            val bytes = "OK".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}/"
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `repository polls live ESP8266 JSON and parses all 9 fields`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SensorMonitorRepository(EspNetworkClient(context))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        repo.updateConfiguration(baseUrl = baseUrl, intervalMs = 500L, simulatorMode = false)
        repo.onForegroundChanged(scope, true)

        waitUntil(timeoutMs = 4000L) {
            repo.connectionStatus.value == EspConnectionStatus.CONNECTED &&
                repo.sensorData.value.distanceCm == 152
        }

        val data = repo.sensorData.value
        assertEquals(EspConnectionStatus.CONNECTED, repo.connectionStatus.value)
        assertEquals(152, data.distanceCm)
        assertEquals(1024, data.waterRaw)
        assertFalse(data.waterDetected)
        assertFalse(data.vibrationActive)
        assertFalse(data.obstacleAlert)
        assertEquals("NONE", data.alertMessage)
        assertEquals(EspButtonState.NONE, data.lastButton)
        assertEquals("System Start Ho Gaya", data.lcdMessage)
        assertTrue(data.deviceConnectedFlag)

        // Update payload with obstacle + water + CHECK button and verify live dashboard update + clearButton
        sensorJsonPayload.set(
            """
            {
              "distance": 34.6,
              "water": 280,
              "waterDetected": true,
              "vib": true,
              "obstacleAlert": true,
              "alert": "OBSTACLE_WATER",
              "button": "CHECK",
              "lcd": "Rukawat 35cm",
              "connected": true
            }
            """.trimIndent()
        )

        waitUntil(timeoutMs = 4000L) {
            repo.sensorData.value.distanceCm == 35 && clearButtonCalled.get()
        }

        val updated = repo.sensorData.value
        assertEquals(35, updated.distanceCm)
        assertEquals(280, updated.waterRaw)
        assertTrue(updated.waterDetected)
        assertTrue(updated.vibrationActive)
        assertTrue(updated.obstacleAlert)
        assertEquals("OBSTACLE_WATER", updated.alertMessage)
        assertEquals(EspButtonState.CHECK, updated.lastButton)
        assertEquals("Rukawat 35cm", updated.lcdMessage)
        assertTrue(clearButtonCalled.get())

        val sent = repo.sendAnswerToEsp("Saamne darwaza hai")
        assertTrue(sent)
        assertTrue(lastAnswerQuery.get()?.contains("Saamne") == true)

        repo.onForegroundChanged(scope, false)
        scope.cancel()
    }

    @Test
    fun `repository does not pretend ESP is connected when connected=false or host unreachable`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SensorMonitorRepository(EspNetworkClient(context))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        sensorJsonPayload.set(
            """
            {
              "distance": 120,
              "water": 1000,
              "waterDetected": false,
              "vib": false,
              "obstacleAlert": false,
              "alert": "NONE",
              "button": "NONE",
              "lcd": "Sensor Wait",
              "connected": false
            }
            """.trimIndent()
        )

        repo.updateConfiguration(baseUrl = baseUrl, intervalMs = 500L, simulatorMode = false)
        repo.onForegroundChanged(scope, true)

        waitUntil(timeoutMs = 4000L) {
            repo.errorMessageHindi.value != null
        }
        assertEquals(EspConnectionStatus.DISCONNECTED, repo.connectionStatus.value)
        assertFalse(repo.sensorData.value.deviceConnectedFlag)

        // Stop server to simulate unreachable 192.168.4.1
        server.stop(0)
        repo.reconnectNow()

        waitUntil(timeoutMs = 4000L) {
            repo.connectionStatus.value == EspConnectionStatus.DISCONNECTED &&
                repo.sensorData.value.distanceCm == -1
        }
        assertNotNull(repo.errorMessageHindi.value)
        assertEquals(-1, repo.sensorData.value.distanceCm)

        repo.onForegroundChanged(scope, false)
        scope.cancel()
    }

    private suspend fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Timed out after ${timeoutMs}ms waiting for condition")
            }
            delay(50L)
        }
    }
}

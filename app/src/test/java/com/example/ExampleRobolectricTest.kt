package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.location.LocationRepository
import com.example.sensors.EspButtonState
import com.example.sensors.EspSensorData
import com.example.sensors.EspSensorResponse
import com.example.voice.HindiVoiceAssistant
import com.example.voice.VoiceCommandIntent
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Andha Helper`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Andha Helper", appName)
    }

    @Test
    fun `esp8266 json payload parses accurately`() {
        val sampleJson = """
            {
              "distance": 152,
              "water": 1024,
              "waterDetected": false,
              "vib": false,
              "obstacleAlert": false,
              "alert": "NONE",
              "button": "ASK",
              "lcd": "System Start Ho Gaya",
              "connected": true
            }
        """.trimIndent()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(EspSensorResponse::class.java)
        val parsed = adapter.fromJson(sampleJson)!!

        assertEquals(152, parsed.distanceInt)
        assertEquals(1024, parsed.waterInt)
        assertEquals(false, parsed.waterDetected)
        assertEquals("System Start Ho Gaya", parsed.lcd)
        assertEquals(EspButtonState.ASK, EspButtonState.fromRaw(parsed.button))
    }

    @Test
    fun `hindi voice commands map to correct intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assistant = HindiVoiceAssistant(context)
        assertEquals(
            VoiceCommandIntent.DESCRIBE_SCENE_CAMERA,
            assistant.classifyCommand("मेरे सामने क्या है?")
        )
        assertEquals(
            VoiceCommandIntent.READ_SENSOR_STATUS,
            assistant.classifyCommand("सेंसर स्टेटस बताओ")
        )
        assertEquals(
            VoiceCommandIntent.TRIGGER_SOS,
            assistant.classifyCommand("बचाओ SOS मदद")
        )
        assertEquals(
            VoiceCommandIntent.TRIGGER_CALL,
            assistant.classifyCommand("आपातकालीन कॉल करें")
        )
        assistant.release()
    }

    @Test
    fun `maps url and settings url normalization work`() {
        assertEquals(
            "https://maps.google.com/?q=28.6139,77.209",
            LocationRepository.buildGoogleMapsUrl(28.6139, 77.209)
        )
        assertEquals(
            "http://192.168.4.1/",
            SettingsRepository.normalizeBaseUrl("192.168.4.1")
        )
        val data = EspSensorData(distanceCm = 42, obstacleAlert = true)
        assertTrue(data.toHindiSummary().contains("42"))
    }
}

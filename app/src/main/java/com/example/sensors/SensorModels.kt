package com.example.sensors

import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

enum class EspButtonState(val rawValue: String, val hindiLabel: String) {
    ASK("ASK", "पूछें (ASK)"),
    CALL("CALL", "कॉल (CALL)"),
    SOS("SOS", "आपातकाल (SOS)"),
    CHECK("CHECK", "जाँच (CHECK)"),
    NONE("NONE", "कोई नहीं (NONE)");

    companion object {
        fun fromRaw(value: String?): EspButtonState {
            val normalized = value?.trim()?.uppercase() ?: "NONE"
            return entries.firstOrNull { it.rawValue == normalized } ?: NONE
        }
    }
}

enum class EspConnectionStatus(val hindiLabel: String) {
    CONNECTED("जुड़ा है (Connected)"),
    CONNECTING("जुड़ रहा है…"),
    DISCONNECTED("डिस्कनेक्ट है"),
    SIMULATED("सिम्युलेशन मोड (टेस्ट)")
}

@JsonClass(generateAdapter = true)
data class EspSensorResponse(
    val distance: Double = -1.0,
    val water: Double = 1024.0,
    val waterDetected: Boolean = false,
    val vib: Boolean = false,
    val obstacleAlert: Boolean = false,
    val alert: String? = "NONE",
    val button: String? = "NONE",
    val lcd: String? = "",
    val connected: Boolean = false
) {
    val distanceInt: Int get() = if (distance < 0) -1 else distance.roundToInt()
    val waterInt: Int get() = water.roundToInt()
}

data class EspSensorData(
    val distanceCm: Int = -1,
    val waterRaw: Int = 1024,
    val waterDetected: Boolean = false,
    val vibrationActive: Boolean = false,
    val obstacleAlert: Boolean = false,
    val alertMessage: String = "NONE",
    val lastButton: EspButtonState = EspButtonState.NONE,
    val lcdMessage: String = "प्रतीक्षा में…",
    val deviceConnectedFlag: Boolean = false,
    val updatedAtMillis: Long = 0L
) {
    fun toHindiSummary(): String {
        val distPart = if (distanceCm >= 0) {
            "सामने दूरी $distanceCm सेंटीमीटर है।"
        } else {
            "दूरी सेंसर डेटा उपलब्ध नहीं है।"
        }
        val obstaclePart = if (obstacleAlert) "सावधान, आगे बाधा है।" else "कोई बाधा चेतावनी नहीं है।"
        val waterPart = if (waterDetected) "पानी का अलर्ट सक्रिय है।" else "पानी नहीं मिला।"
        val vibPart = if (vibrationActive) "वाइब्रेशन चालू है।" else "वाइब्रेशन बंद है।"
        return "$distPart $obstaclePart $waterPart $vibPart"
    }
}

data class WifiStatusInfo(
    val isWifiConnected: Boolean = false,
    val ssid: String = "अज्ञात Wi-Fi",
    val isBoundToWifi: Boolean = false,
    val isAndhaHelperAp: Boolean = false
)

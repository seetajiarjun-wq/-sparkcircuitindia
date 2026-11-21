package com.example.data

enum class AiProviderMode(val label: String, val hindiDescription: String) {
    FIREBASE_AI(
        label = "Firebase AI Logic (Gemini)",
        hindiDescription = "क्लाउड Gemini विज़न (बिना API कुंजी, सुरक्षित App Check)"
    ),
    CUSTOM_BACKEND(
        label = "Custom Backend Proxy",
        hindiDescription = "आपका सुरक्षित बैकएंड/प्रॉक्सी URL (API कुंजी सर्वर पर)"
    ),
    DEMO_MOCK(
        label = "Demo / Offline Mock Mode",
        hindiDescription = "ऑफ़लाइन टेस्ट के लिए स्पष्ट रूप से चिह्नित डेमो उत्तर"
    )
}

enum class VoiceLanguageOption(val localeTag: String, val displayLabel: String) {
    HINDI("hi-IN", "हिंदी (Hindi - भारत)"),
    BILINGUAL("hi-IN", "हिंदी + सरल अंग्रेज़ी"),
    ENGLISH("en-IN", "English (India)")
}

data class EmergencyContact(
    val name: String = "",
    val phone: String = ""
) {
    val isConfigured: Boolean get() = phone.isNotBlank()
}

data class AppSettings(
    val primaryContact: EmergencyContact = EmergencyContact(),
    val secondaryContact: EmergencyContact = EmergencyContact(),
    val emergencySmsMessage: String = "आपातकाल! मुझे तुरंत मदद चाहिए। मेरी लोकेशन:",
    val espBaseUrl: String = "http://192.168.4.1/",
    val pollingIntervalMs: Long = 1500L,
    val aiProviderMode: AiProviderMode = AiProviderMode.FIREBASE_AI,
    val customAiBackendUrl: String = "",
    val voiceLanguage: VoiceLanguageOption = VoiceLanguageOption.HINDI,
    val speechSpeed: Float = 0.95f,
    val autoSpeakAlerts: Boolean = true,
    val highContrastMode: Boolean = true,
    val largeTextMode: Boolean = false,
    val testModeEnabled: Boolean = true,
    val hardwareSimulatorEnabled: Boolean = false
)

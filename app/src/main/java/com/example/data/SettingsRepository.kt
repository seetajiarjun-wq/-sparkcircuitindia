package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "andha_helper_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PRIMARY_NAME = stringPreferencesKey("primary_contact_name")
        val PRIMARY_PHONE = stringPreferencesKey("primary_contact_phone")
        val SECONDARY_NAME = stringPreferencesKey("secondary_contact_name")
        val SECONDARY_PHONE = stringPreferencesKey("secondary_contact_phone")
        val EMERGENCY_SMS = stringPreferencesKey("emergency_sms_message")
        val ESP_BASE_URL = stringPreferencesKey("esp_base_url")
        val POLL_INTERVAL_MS = longPreferencesKey("poll_interval_ms")
        val AI_PROVIDER_MODE = stringPreferencesKey("ai_provider_mode")
        val CUSTOM_AI_BACKEND_URL = stringPreferencesKey("custom_ai_backend_url")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val AUTO_SPEAK_ALERTS = booleanPreferencesKey("auto_speak_alerts")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_mode")
        val LARGE_TEXT = booleanPreferencesKey("large_text_mode")
        val TEST_MODE = booleanPreferencesKey("test_mode_enabled")
        val HARDWARE_SIMULATOR = booleanPreferencesKey("hardware_simulator_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val aiModeName = prefs[Keys.AI_PROVIDER_MODE] ?: AiProviderMode.FIREBASE_AI.name
        val aiMode = AiProviderMode.entries.firstOrNull { it.name == aiModeName }
            ?: AiProviderMode.FIREBASE_AI

        val langName = prefs[Keys.VOICE_LANGUAGE] ?: VoiceLanguageOption.HINDI.name
        val lang = VoiceLanguageOption.entries.firstOrNull { it.name == langName }
            ?: VoiceLanguageOption.HINDI

        AppSettings(
            primaryContact = EmergencyContact(
                name = prefs[Keys.PRIMARY_NAME] ?: "",
                phone = prefs[Keys.PRIMARY_PHONE] ?: ""
            ),
            secondaryContact = EmergencyContact(
                name = prefs[Keys.SECONDARY_NAME] ?: "",
                phone = prefs[Keys.SECONDARY_PHONE] ?: ""
            ),
            emergencySmsMessage = prefs[Keys.EMERGENCY_SMS]
                ?: "आपातकाल! मुझे तुरंत मदद चाहिए। मेरी लोकेशन:",
            espBaseUrl = normalizeBaseUrl(prefs[Keys.ESP_BASE_URL] ?: "http://192.168.4.1/"),
            pollingIntervalMs = (prefs[Keys.POLL_INTERVAL_MS] ?: 1500L).coerceIn(500L, 10000L),
            aiProviderMode = aiMode,
            customAiBackendUrl = prefs[Keys.CUSTOM_AI_BACKEND_URL] ?: "",
            voiceLanguage = lang,
            speechSpeed = (prefs[Keys.SPEECH_SPEED] ?: 0.95f).coerceIn(0.6f, 1.6f),
            autoSpeakAlerts = prefs[Keys.AUTO_SPEAK_ALERTS] ?: true,
            highContrastMode = prefs[Keys.HIGH_CONTRAST] ?: true,
            largeTextMode = prefs[Keys.LARGE_TEXT] ?: false,
            testModeEnabled = prefs[Keys.TEST_MODE] ?: true,
            hardwareSimulatorEnabled = prefs[Keys.HARDWARE_SIMULATOR] ?: false
        )
    }

    suspend fun updateContacts(
        primary: EmergencyContact,
        secondary: EmergencyContact,
        smsMessage: String
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.PRIMARY_NAME] = primary.name.trim()
            prefs[Keys.PRIMARY_PHONE] = sanitizePhone(primary.phone)
            prefs[Keys.SECONDARY_NAME] = secondary.name.trim()
            prefs[Keys.SECONDARY_PHONE] = sanitizePhone(secondary.phone)
            prefs[Keys.EMERGENCY_SMS] = smsMessage.trim().ifEmpty {
                "आपातकाल! मुझे तुरंत मदद चाहिए। मेरी लोकेशन:"
            }
        }
    }

    suspend fun updateEspSettings(baseUrl: String, pollingIntervalMs: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ESP_BASE_URL] = normalizeBaseUrl(baseUrl)
            prefs[Keys.POLL_INTERVAL_MS] = pollingIntervalMs.coerceIn(500L, 10000L)
        }
    }

    suspend fun updateAiSettings(mode: AiProviderMode, customBackendUrl: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER_MODE] = mode.name
            prefs[Keys.CUSTOM_AI_BACKEND_URL] = customBackendUrl.trim()
        }
    }

    suspend fun updateVoiceAndAccessibility(
        language: VoiceLanguageOption,
        speechSpeed: Float,
        autoSpeakAlerts: Boolean,
        highContrastMode: Boolean,
        largeTextMode: Boolean
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VOICE_LANGUAGE] = language.name
            prefs[Keys.SPEECH_SPEED] = speechSpeed.coerceIn(0.6f, 1.6f)
            prefs[Keys.AUTO_SPEAK_ALERTS] = autoSpeakAlerts
            prefs[Keys.HIGH_CONTRAST] = highContrastMode
            prefs[Keys.LARGE_TEXT] = largeTextMode
        }
    }

    suspend fun setTestMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.TEST_MODE] = enabled
        }
    }

    suspend fun setHardwareSimulator(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HARDWARE_SIMULATOR] = enabled
        }
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().ifEmpty { "http://192.168.4.1/" }
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }

        fun sanitizePhone(raw: String): String {
            return raw.trim().filter { it.isDigit() || it == '+' }
        }
    }
}

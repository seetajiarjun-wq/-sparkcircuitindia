package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.VoiceLanguageOption
import com.example.sensors.EspButtonState
import com.example.sensors.EspConnectionStatus
import com.example.sensors.EspSensorData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "VoiceAssistantManager"

enum class VoiceCommandIntent {
    DESCRIBE_SCENE_CAMERA,
    READ_SENSOR_STATUS,
    TRIGGER_SOS,
    TRIGGER_CALL,
    GENERAL_QUERY
}

data class VoiceAssistantState(
    val ttsReady: Boolean = false,
    val hindiVoiceAvailable: Boolean = true,
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val lastSpokenInput: String = "",
    val lastAssistantResponse: String = "",
    val voiceError: String? = null
)

class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var currentSpeechRate: Float = 0.95f
    private var currentLanguage: VoiceLanguageOption = VoiceLanguageOption.HINDI

    private val _state = MutableStateFlow(VoiceAssistantState())
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private val _recognizedCommands = MutableSharedFlow<Pair<VoiceCommandIntent, String>>(
        extraBufferCapacity = 4
    )
    val recognizedCommands: SharedFlow<Pair<VoiceCommandIntent, String>> =
        _recognizedCommands.asSharedFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hiLocale = Locale.forLanguageTag("hi-IN")
            val result = tts?.setLanguage(hiLocale)
            val hindiSupported = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!hindiSupported) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(currentSpeechRate)
            _state.value = _state.value.copy(
                ttsReady = true,
                hindiVoiceAvailable = hindiSupported
            )
        } else {
            _state.value = _state.value.copy(
                ttsReady = false,
                voiceError = "टेक्स्ट-टू-स्पीच इंजन प्रारंभ नहीं हो सका।"
            )
        }
    }

    fun updateSettings(language: VoiceLanguageOption, speechSpeed: Float) {
        currentLanguage = language
        currentSpeechRate = speechSpeed.coerceIn(0.6f, 1.6f)
        tts?.setSpeechRate(currentSpeechRate)
        val locale = Locale.forLanguageTag(language.localeTag)
        val res = tts?.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.getDefault())
        }
    }

    fun speak(text: String, flushQueue: Boolean = true) {
        if (text.isBlank()) return
        _state.value = _state.value.copy(lastAssistantResponse = text)
        if (_state.value.ttsReady) {
            val mode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, mode, null, "andha_utt_${System.currentTimeMillis()}")
        }
    }

    fun speakConnectionStatus(connectionStatus: EspConnectionStatus, wifiSsid: String) {
        val msg = when (connectionStatus) {
            EspConnectionStatus.CONNECTED ->
                "ESP8266 डिवाइस $wifiSsid से जुड़ा हुआ है।"
            EspConnectionStatus.SIMULATED ->
                "सिम्युलेशन टेस्ट मोड सक्रिय है।"
            EspConnectionStatus.CONNECTING ->
                "ESP8266 से कनेक्ट करने का प्रयास जारी है।"
            EspConnectionStatus.DISCONNECTED ->
                "ESP8266 डिवाइस डिस्कनेक्ट है। कृपया ANDHA-HELPER Wi-Fi जांचें।"
        }
        speak(msg)
    }

    fun speakSensorStatus(
        data: EspSensorData,
        connectionStatus: EspConnectionStatus,
        errorMessageHindi: String? = null
    ) {
        val msg = when (connectionStatus) {
            EspConnectionStatus.DISCONNECTED ->
                "ESP8266 जुड़ा नहीं है। ${errorMessageHindi ?: "कृपया Wi-Fi जांचें।"}"
            else -> {
                val buttonNote = if (data.lastButton != EspButtonState.NONE) {
                    " अंतिम बटन: ${data.lastButton.hindiLabel}।"
                } else ""
                "${connectionStatus.hindiLabel}। ${data.toHindiSummary()}$buttonNote"
            }
        }
        speak(msg)
    }

    fun speakObstacleAlert(distanceCm: Int, obstacleAlert: Boolean) {
        val msg = when {
            obstacleAlert && distanceCm >= 0 ->
                "चेतावनी: सामने $distanceCm सेंटीमीटर पर बाधा है।"
            distanceCm >= 0 ->
                "रास्ता साफ है, सामने दूरी $distanceCm सेंटीमीटर है।"
            else ->
                "बाधा सेंसर डेटा अभी उपलब्ध नहीं है।"
        }
        speak(msg)
    }

    fun speakWaterAlert(waterRaw: Int, waterDetected: Boolean) {
        val msg = if (waterDetected) {
            "पानी का अलर्ट! आगे पानी या नमी पाई गई है। सेंसर मान $waterRaw है।"
        } else {
            "कोई पानी नहीं मिला। वाटर सेंसर मान $waterRaw सामान्य है।"
        }
        speak(msg)
    }

    fun speakButtonStatus(button: EspButtonState) {
        val msg = if (button == EspButtonState.NONE) {
            "अभी कोई हार्डवेयर बटन दबाया नहीं गया है।"
        } else {
            "अंतिम दबाया गया बटन: ${button.hindiLabel}।"
        }
        speak(msg)
    }

    fun speakCameraDescription(description: String) {
        speak(description)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val msg = "इस डिवाइस पर स्पीच रिकॉग्निशन सेवा उपलब्ध नहीं है।"
            _state.value = _state.value.copy(voiceError = msg, isListening = false)
            speak(msg)
            return
        }

        stopSpeaking()
        _state.value = _state.value.copy(
            isListening = true,
            partialTranscript = "",
            voiceError = null
        )

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = _state.value.copy(isListening = true, voiceError = null)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _state.value = _state.value.copy(isListening = false)
                    }

                    override fun onError(error: Int) {
                        val errorText = mapSpeechErrorToHindi(error)
                        _state.value = _state.value.copy(
                            isListening = false,
                            voiceError = errorText
                        )
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()?.trim().orEmpty()
                        _state.value = _state.value.copy(
                            isListening = false,
                            lastSpokenInput = spoken,
                            partialTranscript = spoken
                        )
                        if (spoken.isNotBlank()) {
                            val intent = classifyCommand(spoken)
                            _recognizedCommands.tryEmit(intent to spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            _state.value = _state.value.copy(partialTranscript = partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _state.value = _state.value.copy(
                isListening = false,
                voiceError = "माइक्रोफ़ोन शुरू नहीं हो सका: ${e.localizedMessage ?: "त्रुटि"}"
            )
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = _state.value.copy(isListening = false)
    }

    fun processManualCommand(text: String) {
        if (text.isBlank()) return
        _state.value = _state.value.copy(
            lastSpokenInput = text,
            voiceError = null
        )
        _recognizedCommands.tryEmit(classifyCommand(text) to text)
    }

    fun classifyCommand(text: String): VoiceCommandIntent {
        val lower = text.lowercase()
        val hasExplicitSos = listOf("sos", "बचाओ", "खतरा").any { lower.contains(it) }
        val hasCallKeyword = listOf("कॉल", "फोन", "call", "contact").any { lower.contains(it) }
        val hasGenericEmergency = listOf("मदद", "आपात", "emergency", "help").any { lower.contains(it) }

        return when {
            hasExplicitSos -> VoiceCommandIntent.TRIGGER_SOS
            hasCallKeyword -> VoiceCommandIntent.TRIGGER_CALL
            hasGenericEmergency -> VoiceCommandIntent.TRIGGER_SOS

            listOf("सामने", "क्या है", "तस्वीर", "देखो", "कैमरा", "वस्तु", "दृश्य", "camera", "front", "see").any {
                lower.contains(it)
            } -> VoiceCommandIntent.DESCRIBE_SCENE_CAMERA

            listOf("स्टेटस", "स्थिति", "जांच", "सेंसर", "दूरी", "पानी", "कनेक्शन", "status", "sensor", "check").any {
                lower.contains(it)
            } -> VoiceCommandIntent.READ_SENSOR_STATUS

            else -> VoiceCommandIntent.GENERAL_QUERY
        }
    }

    private fun mapSpeechErrorToHindi(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "आवाज़ स्पष्ट समझ नहीं आई। कृपया फिर से बोलें।"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "कोई आवाज़ नहीं सुनाई दी। कृपया माइक बटन दबाकर बोलें।"
            SpeechRecognizer.ERROR_AUDIO -> "माइक्रोफ़ोन रिकॉर्डिंग में समस्या आई।"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "वॉइस पहचान के लिए इंटरनेट आवश्यक हो सकता है (ESP Wi-Fi पर ऑफ़लाइन हो सकता है)।"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "कृपया माइक्रोफ़ोन की अनुमति (Microphone Permission) दें।"
            else -> "वॉइस त्रुटि कोड: $error"
        }
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup error: ${e.message}")
        }
    }
}

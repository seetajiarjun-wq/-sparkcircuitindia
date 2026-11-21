package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.VisionAiRepository
import com.example.ai.VisionUiState
import com.example.data.AiProviderMode
import com.example.data.AppSettings
import com.example.data.EmergencyContact
import com.example.data.SettingsRepository
import com.example.data.VoiceLanguageOption
import com.example.emergency.SosManager
import com.example.emergency.SosWorkflowState
import com.example.location.LocationRepository
import com.example.location.LocationResult
import com.example.network.EspNetworkClient
import com.example.sensors.EspButtonState
import com.example.sensors.EspConnectionStatus
import com.example.sensors.EspSensorData
import com.example.sensors.SensorMonitorRepository
import com.example.sensors.WifiStatusInfo
import com.example.voice.HindiVoiceAssistant
import com.example.voice.VoiceAssistantState
import com.example.voice.VoiceCommandIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppNavigationEvent {
    data class OpenCameraAndCapture(val prompt: String) : AppNavigationEvent
    data object OpenVoiceAssistant : AppNavigationEvent
}

class AndhaHelperViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepository = SettingsRepository(appContext)
    private val networkClient = EspNetworkClient(appContext)
    private val sensorRepository = SensorMonitorRepository(networkClient)
    private val voiceAssistant = HindiVoiceAssistant(appContext)
    private val visionRepository = VisionAiRepository()
    private val locationRepository = LocationRepository(appContext)
    private val sosManager = SosManager(appContext, locationRepository)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val sensorData: StateFlow<EspSensorData> = sensorRepository.sensorData
    val connectionStatus: StateFlow<EspConnectionStatus> = sensorRepository.connectionStatus
    val wifiStatus: StateFlow<WifiStatusInfo> = sensorRepository.wifiStatus
    val sensorErrorHindi: StateFlow<String?> = sensorRepository.errorMessageHindi
    val voiceState: StateFlow<VoiceAssistantState> = voiceAssistant.state
    val sosState: StateFlow<SosWorkflowState> = sosManager.state

    private val _visionState = MutableStateFlow<VisionUiState>(VisionUiState.Idle)
    val visionState: StateFlow<VisionUiState> = _visionState.asStateFlow()

    private val _selectedCameraPrompt = MutableStateFlow("मेरे सामने क्या है?")
    val selectedCameraPrompt: StateFlow<String> = _selectedCameraPrompt.asStateFlow()

    private val _pendingAutoCapturePrompt = MutableStateFlow<String?>(null)
    val pendingAutoCapturePrompt: StateFlow<String?> = _pendingAutoCapturePrompt.asStateFlow()

    private val _lastCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val lastCapturedBitmap: StateFlow<Bitmap?> = _lastCapturedBitmap.asStateFlow()

    private val _locationTestResult = MutableStateFlow<LocationResult?>(null)
    val locationTestResult: StateFlow<LocationResult?> = _locationTestResult.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AppNavigationEvent>(extraBufferCapacity = 4)
    val navigationEvents: SharedFlow<AppNavigationEvent> = _navigationEvents.asSharedFlow()

    private var prevObstacleAlert = false
    private var prevWaterDetected = false
    private var lastAlertSpokenAt = 0L

    init {
        observeSettings()
        observeHardwareButtons()
        observeSensorAlerts()
        observeVoiceCommands()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settings.collectLatest { current ->
                sensorRepository.updateConfiguration(
                    baseUrl = current.espBaseUrl,
                    intervalMs = current.pollingIntervalMs,
                    simulatorMode = current.hardwareSimulatorEnabled
                )
                voiceAssistant.updateSettings(
                    language = current.voiceLanguage,
                    speechSpeed = current.speechSpeed
                )
            }
        }
    }

    private fun observeHardwareButtons() {
        viewModelScope.launch {
            sensorRepository.buttonEvents.collect { button ->
                vibrateShort()
                when (button) {
                    EspButtonState.ASK -> {
                        val prompt = "मेरे सामने क्या है?"
                        voiceAssistant.speak("कैमरा खुल रहा है, सामने का दृश्य देख रहे हैं।")
                        _selectedCameraPrompt.value = prompt
                        _pendingAutoCapturePrompt.value = prompt
                        _navigationEvents.tryEmit(AppNavigationEvent.OpenCameraAndCapture(prompt))
                    }

                    EspButtonState.CALL -> {
                        initiateCallToPrimaryContact()
                    }

                    EspButtonState.SOS -> {
                        triggerSosWorkflow(triggerSource = "ESP8266 SOS बटन")
                    }

                    EspButtonState.CHECK -> {
                        speakFullSensorAndConnectionStatus()
                    }

                    EspButtonState.NONE -> Unit
                }
            }
        }
    }

    private fun observeSensorAlerts() {
        viewModelScope.launch {
            combine(sensorData, settings) { data, cfg -> data to cfg }.collect { (data, cfg) ->
                val now = System.currentTimeMillis()
                val obstacleTurnedOn = data.obstacleAlert && !prevObstacleAlert
                val waterTurnedOn = data.waterDetected && !prevWaterDetected
                prevObstacleAlert = data.obstacleAlert
                prevWaterDetected = data.waterDetected

                if (cfg.autoSpeakAlerts && (obstacleTurnedOn || waterTurnedOn) && now - lastAlertSpokenAt > 4000L) {
                    lastAlertSpokenAt = now
                    vibrateShort()
                    val msg = buildString {
                        if (obstacleTurnedOn) {
                            append("सावधान! आगे ${data.distanceCm} सेंटीमीटर पर बाधा है। ")
                        }
                        if (waterTurnedOn) {
                            append("सावधान! आगे पानी का पता चला है।")
                        }
                    }.trim()
                    if (msg.isNotEmpty()) {
                        voiceAssistant.speak(msg, flushQueue = false)
                    }
                }
            }
        }
    }

    private fun observeVoiceCommands() {
        viewModelScope.launch {
            voiceAssistant.recognizedCommands.collect { (intent, rawText) ->
                when (intent) {
                    VoiceCommandIntent.DESCRIBE_SCENE_CAMERA -> {
                        _selectedCameraPrompt.value = rawText
                        _pendingAutoCapturePrompt.value = rawText
                        voiceAssistant.speak("कैमरा खोलकर देख रहा हूँ।")
                        _navigationEvents.tryEmit(AppNavigationEvent.OpenCameraAndCapture(rawText))
                    }

                    VoiceCommandIntent.READ_SENSOR_STATUS -> {
                        speakFullSensorAndConnectionStatus()
                    }

                    VoiceCommandIntent.TRIGGER_SOS -> {
                        triggerSosWorkflow(triggerSource = "हिंदी वॉइस कमांड")
                    }

                    VoiceCommandIntent.TRIGGER_CALL -> {
                        initiateCallToPrimaryContact()
                    }

                    VoiceCommandIntent.GENERAL_QUERY -> {
                        val reply = "आपने कहा: $rawText। कैमरा विवरण के लिए 'मेरे सामने क्या है' या स्थिति के लिए 'स्टेटस बताओ' बोलें।"
                        voiceAssistant.speak(reply)
                    }
                }
            }
        }
    }

    fun onAppForegroundChanged(active: Boolean) {
        sensorRepository.onForegroundChanged(viewModelScope, active)
    }

    fun reconnectEspNow() {
        sensorRepository.reconnectNow()
    }

    fun speakText(text: String) {
        voiceAssistant.speak(text)
    }

    fun stopSpeaking() {
        voiceAssistant.stopSpeaking()
    }

    fun speakConnectionStatus() {
        val conn = connectionStatus.value
        val wifi = wifiStatus.value
        val msg = when (conn) {
            EspConnectionStatus.CONNECTED ->
                "ESP8266 डिवाइस ${wifi.ssid} से जुड़ा हुआ है।"
            EspConnectionStatus.SIMULATED ->
                "सिम्युलेशन टेस्ट मोड सक्रिय है।"
            EspConnectionStatus.CONNECTING ->
                "ESP8266 से कनेक्ट करने का प्रयास जारी है।"
            EspConnectionStatus.DISCONNECTED ->
                "ESP8266 डिवाइस डिस्कनेक्ट है। कृपया ANDHA-HELPER Wi-Fi जांचें।"
        }
        voiceAssistant.speak(msg)
    }

    fun speakFullSensorAndConnectionStatus() {
        val conn = connectionStatus.value
        val data = sensorData.value
        val msg = when (conn) {
            EspConnectionStatus.DISCONNECTED ->
                "ESP8266 जुड़ा नहीं है। ${sensorErrorHindi.value ?: "कृपया Wi-Fi जांचें।"}"
            else -> {
                val buttonNote = if (data.lastButton != EspButtonState.NONE) {
                    " अंतिम बटन: ${data.lastButton.hindiLabel}।"
                } else ""
                "${conn.hindiLabel}। ${data.toHindiSummary()}$buttonNote"
            }
        }
        voiceAssistant.speak(msg)
    }

    fun speakObstacleStatus() {
        val data = sensorData.value
        val msg = if (data.obstacleAlert) {
            "चेतावनी: सामने ${data.distanceCm} सेंटीमीटर पर बाधा है।"
        } else if (data.distanceCm >= 0) {
            "रास्ता साफ है, सामने दूरी ${data.distanceCm} सेंटीमीटर है।"
        } else {
            "बाधा सेंसर डेटा अभी उपलब्ध नहीं है।"
        }
        voiceAssistant.speak(msg)
    }

    fun speakWaterStatus() {
        val data = sensorData.value
        val msg = if (data.waterDetected) {
            "पानी का अलर्ट! आगे पानी या नमी पाई गई है। सेंसर मान ${data.waterRaw} है।"
        } else {
            "कोई पानी नहीं मिला। वाटर सेंसर मान ${data.waterRaw} सामान्य है।"
        }
        voiceAssistant.speak(msg)
    }

    fun startVoiceListening() {
        voiceAssistant.startListening()
    }

    fun stopVoiceListening() {
        voiceAssistant.stopListening()
    }

    fun submitTextVoiceCommand(command: String) {
        voiceAssistant.processManualCommand(command)
    }

    fun selectCameraPrompt(prompt: String) {
        _selectedCameraPrompt.value = prompt
    }

    fun consumePendingAutoCapturePrompt(): String? {
        val pending = _pendingAutoCapturePrompt.value
        _pendingAutoCapturePrompt.value = null
        return pending
    }

    fun setCameraCapturingState() {
        _visionState.value = VisionUiState.Capturing
    }

    fun onCameraError(messageHindi: String) {
        _visionState.value = VisionUiState.Error(messageHindi)
        voiceAssistant.speak(messageHindi)
    }

    fun analyzeCapturedImage(bitmap: Bitmap, promptOverride: String? = null) {
        val prompt = (promptOverride ?: _selectedCameraPrompt.value).ifBlank { "मेरे सामने क्या है?" }
        val cfg = settings.value
        _lastCapturedBitmap.value = bitmap
        _visionState.value = VisionUiState.Analyzing
        voiceAssistant.speak("तस्वीर का विश्लेषण हो रहा है, कृपया प्रतीक्षा करें।")

        viewModelScope.launch {
            val result = visionRepository.analyzeImage(
                bitmap = bitmap,
                userPrompt = prompt,
                providerMode = cfg.aiProviderMode,
                customBackendUrl = cfg.customAiBackendUrl
            )
            result.onSuccess { description ->
                _visionState.value = VisionUiState.Success(
                    descriptionHindi = description,
                    promptUsed = prompt,
                    providerLabel = cfg.aiProviderMode.label,
                    isDemoResponse = cfg.aiProviderMode == AiProviderMode.DEMO_MOCK
                )
                voiceAssistant.speak(description)
                // Mirror concise response to ESP8266 LCD/answer endpoint if connected
                sensorRepository.sendAnswerToEsp(description)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "AI विश्लेषण विफल रहा।"
                _visionState.value = VisionUiState.Error(msg)
                voiceAssistant.speak(msg)
            }
        }
    }

    fun retryLastVisionAnalysis() {
        val bmp = _lastCapturedBitmap.value
        if (bmp != null) {
            analyzeCapturedImage(bmp, _selectedCameraPrompt.value)
        } else {
            _visionState.value = VisionUiState.Idle
        }
    }

    fun triggerSosWorkflow(triggerSource: String = "ऐप SOS बटन") {
        vibrateShort()
        sosManager.triggerSos(
            scope = viewModelScope,
            settings = settings.value,
            triggerSource = triggerSource,
            onSpeak = { voiceAssistant.speak(it) }
        )
    }

    fun cancelSosWorkflow() {
        sosManager.cancelSos(onSpeak = { voiceAssistant.speak(it) })
    }

    fun dismissSosStatus() {
        sosManager.dismissStatus()
    }

    fun initiateCallToPrimaryContact() {
        val cfg = settings.value
        val primary = cfg.primaryContact
        val target = if (primary.isConfigured) primary else cfg.secondaryContact
        val (ok, msg) = sosManager.openDialer(target.phone, cfg.testModeEnabled)
        _statusBannerMessage.value = msg
        voiceAssistant.speak(msg)
    }

    fun openSmsForSos(phone: String, body: String) {
        val ok = sosManager.openSmsComposer(phone, body)
        if (!ok) {
            val msg = "SMS ऐप नहीं खुल सका या नंबर अमान्य है।"
            _statusBannerMessage.value = msg
            voiceAssistant.speak(msg)
        }
    }

    fun openDialerForSos(phone: String) {
        val (_, msg) = sosManager.openDialer(phone, settings.value.testModeEnabled)
        _statusBannerMessage.value = msg
        voiceAssistant.speak(msg)
    }

    fun testGpsLocation() {
        viewModelScope.launch {
            val res = locationRepository.getCurrentLocation()
            _locationTestResult.value = res
            when (res) {
                is LocationResult.Success ->
                    voiceAssistant.speak("लोकेशन प्राप्त हुई। सटीकता ${res.accuracyMeters.toInt()} मीटर है।")
                LocationResult.PermissionDenied ->
                    voiceAssistant.speak("लोकेशन अनुमति आवश्यक है।")
                LocationResult.GpsDisabled ->
                    voiceAssistant.speak("कृपया फोन का GPS चालू करें।")
                is LocationResult.Error ->
                    voiceAssistant.speak(res.messageHindi)
            }
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    // Settings mutations
    fun saveEmergencyContacts(
        primaryName: String,
        primaryPhone: String,
        secondaryName: String,
        secondaryPhone: String,
        smsTemplate: String
    ) {
        viewModelScope.launch {
            settingsRepository.updateContacts(
                primary = EmergencyContact(primaryName, primaryPhone),
                secondary = EmergencyContact(secondaryName, secondaryPhone),
                smsMessage = smsTemplate
            )
            _statusBannerMessage.value = "आपातकालीन संपर्क सुरक्षित हो गए।"
            voiceAssistant.speak("आपातकालीन संपर्क सुरक्षित कर दिए गए हैं।")
        }
    }

    fun saveEspSettings(baseUrl: String, intervalMs: Long) {
        viewModelScope.launch {
            settingsRepository.updateEspSettings(baseUrl, intervalMs)
            _statusBannerMessage.value = "ESP8266 सेटिंग्स अपडेट हो गईं।"
        }
    }

    fun saveAiSettings(mode: AiProviderMode, customBackendUrl: String) {
        viewModelScope.launch {
            settingsRepository.updateAiSettings(mode, customBackendUrl)
            _statusBannerMessage.value = "AI प्रदाता सेटिंग्स सुरक्षित हो गईं।"
        }
    }

    fun saveVoiceAndAccessibility(
        language: VoiceLanguageOption,
        speechSpeed: Float,
        autoSpeakAlerts: Boolean,
        highContrast: Boolean,
        largeText: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateVoiceAndAccessibility(
                language = language,
                speechSpeed = speechSpeed,
                autoSpeakAlerts = autoSpeakAlerts,
                highContrastMode = highContrast,
                largeTextMode = largeText
            )
            _statusBannerMessage.value = "आवाज़ और एक्सेसिबिलिटी सेटिंग्स लागू हो गईं।"
        }
    }

    fun setTestMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTestMode(enabled)
            val msg = if (enabled) "टेस्ट मोड चालू किया गया।" else "लाइव मोड चालू किया गया। सावधानी से उपयोग करें।"
            _statusBannerMessage.value = msg
            voiceAssistant.speak(msg)
        }
    }

    fun setHardwareSimulator(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHardwareSimulator(enabled)
        }
    }

    // Simulator triggers
    fun simulateDistance(cm: Int) = sensorRepository.simulateDistance(cm)
    fun simulateWater(detected: Boolean) = sensorRepository.simulateWaterAlert(detected)
    fun simulateButton(button: EspButtonState) = sensorRepository.simulateButtonPress(button)

    @Suppress("DEPRECATION")
    private fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                val v = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    v?.vibrate(120L)
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorRepository.stopPolling()
        voiceAssistant.release()
    }
}

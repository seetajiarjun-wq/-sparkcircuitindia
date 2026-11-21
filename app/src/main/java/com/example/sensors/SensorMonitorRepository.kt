package com.example.sensors

import android.util.Log
import com.example.network.EspNetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "SensorMonitorRepo"

class SensorMonitorRepository(
    private val networkClient: EspNetworkClient
) {
    private val _sensorData = MutableStateFlow(EspSensorData())
    val sensorData: StateFlow<EspSensorData> = _sensorData.asStateFlow()

    private val _connectionStatus = MutableStateFlow(EspConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<EspConnectionStatus> = _connectionStatus.asStateFlow()

    private val _wifiStatus = MutableStateFlow(WifiStatusInfo())
    val wifiStatus: StateFlow<WifiStatusInfo> = _wifiStatus.asStateFlow()

    private val _errorMessageHindi = MutableStateFlow<String?>(null)
    val errorMessageHindi: StateFlow<String?> = _errorMessageHindi.asStateFlow()

    private val _buttonEvents = MutableSharedFlow<EspButtonState>(extraBufferCapacity = 4)
    val buttonEvents: SharedFlow<EspButtonState> = _buttonEvents.asSharedFlow()

    private var pollingJob: Job? = null
    private var activePollingScope: CoroutineScope? = null
    private var currentBaseUrl: String = "http://192.168.4.1/"
    private var currentIntervalMs: Long = 1500L
    private var isSimulatorMode: Boolean = false
    private var isAppForeground: Boolean = false

    private var lastHandledButton: EspButtonState = EspButtonState.NONE
    private var lastButtonHandledAt: Long = 0L

    private var simulatedState = EspSensorData(
        distanceCm = 152,
        waterRaw = 1024,
        waterDetected = false,
        vibrationActive = false,
        obstacleAlert = false,
        alertMessage = "NONE",
        lastButton = EspButtonState.NONE,
        lcdMessage = "System Start Ho Gaya (Test)",
        deviceConnectedFlag = true,
        updatedAtMillis = System.currentTimeMillis()
    )

    fun updateConfiguration(
        baseUrl: String,
        intervalMs: Long,
        simulatorMode: Boolean
    ) {
        val changed = currentBaseUrl != baseUrl ||
            currentIntervalMs != intervalMs ||
            isSimulatorMode != simulatorMode
        val leftSimulator = isSimulatorMode && !simulatorMode

        currentBaseUrl = baseUrl
        currentIntervalMs = intervalMs
        isSimulatorMode = simulatorMode

        if (leftSimulator) {
            // Never retain simulated values when switching back to real ESP8266 polling
            _sensorData.value = EspSensorData()
            _connectionStatus.value = EspConnectionStatus.CONNECTING
            _errorMessageHindi.value = null
        }

        if (changed && isAppForeground) {
            restartPollingIfNeeded()
        }
    }

    fun onForegroundChanged(scope: CoroutineScope, active: Boolean) {
        isAppForeground = active
        activePollingScope = if (active) scope else null
        if (active) {
            startPolling(scope)
        } else {
            stopPolling()
        }
    }

    fun reconnectNow() {
        val scope = activePollingScope ?: return
        if (!isSimulatorMode) {
            _connectionStatus.value = EspConnectionStatus.CONNECTING
            _errorMessageHindi.value = null
        }
        startPolling(scope)
    }

    fun startPolling(scope: CoroutineScope) {
        activePollingScope = scope
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            var consecutiveFailures = 0
            while (isActive) {
                _wifiStatus.value = networkClient.getWifiStatus()

                if (isSimulatorMode) {
                    _connectionStatus.value = EspConnectionStatus.SIMULATED
                    _errorMessageHindi.value = null
                    _sensorData.value = simulatedState.copy(updatedAtMillis = System.currentTimeMillis())
                    delay(currentIntervalMs)
                    continue
                }

                if (_connectionStatus.value == EspConnectionStatus.DISCONNECTED && consecutiveFailures == 0) {
                    _connectionStatus.value = EspConnectionStatus.CONNECTING
                }

                try {
                    val response = networkClient.getApi(currentBaseUrl).getSensors()
                    consecutiveFailures = 0

                    val mappedButton = EspButtonState.fromRaw(response.button)
                    val parsed = EspSensorData(
                        distanceCm = response.distanceInt,
                        waterRaw = response.waterInt,
                        waterDetected = response.waterDetected,
                        vibrationActive = response.vib,
                        obstacleAlert = response.obstacleAlert,
                        alertMessage = response.alert?.ifBlank { "NONE" } ?: "NONE",
                        lastButton = mappedButton,
                        lcdMessage = response.lcd?.ifBlank { "कोई संदेश नहीं" } ?: "कोई संदेश नहीं",
                        deviceConnectedFlag = response.connected,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    _sensorData.value = parsed

                    if (response.connected) {
                        _connectionStatus.value = EspConnectionStatus.CONNECTED
                        _errorMessageHindi.value = null
                    } else {
                        // Never pretend the ESP is connected if the device JSON reports connected=false
                        _connectionStatus.value = EspConnectionStatus.DISCONNECTED
                        _errorMessageHindi.value = "ESP8266 से उत्तर मिला, परंतु डिवाइस ने connected=false बताया।"
                    }

                    if (mappedButton != EspButtonState.NONE) {
                        handleHardwareButton(mappedButton)
                    } else {
                        lastHandledButton = EspButtonState.NONE
                    }

                    delay(currentIntervalMs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.w(TAG, "ESP8266 poll failed ($consecutiveFailures): ${e.message}")
                    _connectionStatus.value = EspConnectionStatus.DISCONNECTED
                    _errorMessageHindi.value = mapNetworkErrorToHindi(e)
                    // Reset live sensor telemetry when unreachable so UI never shows stale data as live
                    _sensorData.value = EspSensorData(
                        lcdMessage = "ESP8266 ऑफ़लाइन",
                        deviceConnectedFlag = false
                    )
                    // Backoff cleanly up to 5 seconds when disconnected
                    val backoffMs = (currentIntervalMs * consecutiveFailures.coerceAtMost(3)).coerceAtMost(5000L)
                    delay(backoffMs)
                }
            }
        }
    }

    private fun restartPollingIfNeeded() {
        val scope = activePollingScope ?: return
        startPolling(scope)
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun handleHardwareButton(button: EspButtonState) {
        val now = System.currentTimeMillis()
        val isDuplicateBurst = (button == lastHandledButton) && (now - lastButtonHandledAt < 4000L)
        if (!isDuplicateBurst) {
            lastHandledButton = button
            lastButtonHandledAt = now
            _buttonEvents.tryEmit(button)
        }
        try {
            networkClient.getApi(currentBaseUrl).clearButton()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clearButton on ESP8266: ${e.message}")
        }
    }

    suspend fun sendAnswerToEsp(text: String): Boolean {
        if (isSimulatorMode) {
            simulatedState = simulatedState.copy(lcdMessage = text.take(32))
            _sensorData.value = simulatedState
            return true
        }
        return try {
            val response = networkClient.getApi(currentBaseUrl).sendAnswer(text.take(120))
            response.isSuccessful
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "sendAnswer failed: ${e.message}")
            false
        }
    }

    // Simulator helpers for safe testing without physical ESP8266 attached
    fun simulateDistance(distanceCm: Int) {
        val obstacle = distanceCm in 1..65
        simulatedState = simulatedState.copy(
            distanceCm = distanceCm,
            obstacleAlert = obstacle,
            vibrationActive = obstacle,
            alertMessage = if (obstacle) "OBSTACLE_NEAR" else "NONE",
            lcdMessage = if (obstacle) "Rukawat: ${distanceCm}cm" else "Rasta Saaf: ${distanceCm}cm",
            updatedAtMillis = System.currentTimeMillis()
        )
        _sensorData.value = simulatedState
    }

    fun simulateWaterAlert(detected: Boolean) {
        simulatedState = simulatedState.copy(
            waterDetected = detected,
            waterRaw = if (detected) 320 else 1024,
            vibrationActive = detected || simulatedState.obstacleAlert,
            alertMessage = if (detected) "WATER_DETECTED" else "NONE",
            lcdMessage = if (detected) "Paani Alert!" else "System Normal",
            updatedAtMillis = System.currentTimeMillis()
        )
        _sensorData.value = simulatedState
    }

    fun simulateButtonPress(button: EspButtonState) {
        simulatedState = simulatedState.copy(
            lastButton = button,
            lcdMessage = "Button: ${button.rawValue}",
            updatedAtMillis = System.currentTimeMillis()
        )
        _sensorData.value = simulatedState
        if (button != EspButtonState.NONE) {
            _buttonEvents.tryEmit(button)
        }
    }

    private fun mapNetworkErrorToHindi(e: Exception): String {
        return when (e) {
            is SocketTimeoutException ->
                "ESP8266 से जवाब आने में समय सीमा समाप्त हुई। कृपया ANDHA-HELPER Wi-Fi जांचें।"
            is ConnectException, is UnknownHostException ->
                "ESP8266 (${currentBaseUrl.removeSuffix("/")}) उपलब्ध नहीं है। सुनिश्चित करें कि आप ANDHA-HELPER Wi-Fi से जुड़े हैं।"
            else ->
                "सेंसर कनेक्शन त्रुटि: ${e.localizedMessage ?: "नेटवर्क अनुपलब्ध"}"
        }
    }
}

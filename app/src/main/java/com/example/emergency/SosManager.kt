package com.example.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.AppSettings
import com.example.data.EmergencyContact
import com.example.location.LocationRepository
import com.example.location.LocationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SosWorkflowState {
    data object Idle : SosWorkflowState

    data class Countdown(
        val secondsRemaining: Int,
        val isTestMode: Boolean,
        val triggerSource: String
    ) : SosWorkflowState

    data class PreparingLocation(
        val isTestMode: Boolean
    ) : SosWorkflowState

    data class ReadyOrDispatched(
        val isTestMode: Boolean,
        val primaryContact: EmergencyContact,
        val secondaryContact: EmergencyContact,
        val preparedSmsText: String,
        val mapsLink: String?,
        val locationStatusHindi: String,
        val accuracyMeters: Float?,
        val statusSummaryHindi: String
    ) : SosWorkflowState

    data class Cancelled(val messageHindi: String) : SosWorkflowState
}

class SosManager(
    private val context: Context,
    private val locationRepository: LocationRepository
) {
    private val _state = MutableStateFlow<SosWorkflowState>(SosWorkflowState.Idle)
    val state: StateFlow<SosWorkflowState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var lastTriggerAtMillis: Long = 0L

    private val blockedPublicEmergencyNumbers = setOf("112", "100", "101", "102", "108", "911", "999")

    fun triggerSos(
        scope: CoroutineScope,
        settings: AppSettings,
        triggerSource: String,
        onSpeak: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        // Prevent duplicate/repeated activations within 8 seconds
        if (_state.value is SosWorkflowState.Countdown ||
            _state.value is SosWorkflowState.PreparingLocation ||
            (now - lastTriggerAtMillis < 8000L)
        ) {
            return
        }
        lastTriggerAtMillis = now

        countdownJob?.cancel()
        countdownJob = scope.launch {
            try {
                val modePrefix = if (settings.testModeEnabled) "टेस्ट मोड में " else ""
                onSpeak("${modePrefix}आपातकालीन SOS 5 सेकंड में सक्रिय होगा। रद्द करने के लिए कैंसल बटन दबाएं।")

                for (sec in 5 downTo 1) {
                    _state.value = SosWorkflowState.Countdown(
                        secondsRemaining = sec,
                        isTestMode = settings.testModeEnabled,
                        triggerSource = triggerSource
                    )
                    delay(1000L)
                }

                executeSosWorkflow(settings, onSpeak)
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun cancelSos(onSpeak: (String) -> Unit) {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = SosWorkflowState.Cancelled("आपातकालीन SOS रद्द कर दिया गया है।")
        onSpeak("आपातकालीन SOS रद्द कर दिया गया है।")
    }

    fun dismissStatus() {
        if (_state.value !is SosWorkflowState.Countdown &&
            _state.value !is SosWorkflowState.PreparingLocation
        ) {
            _state.value = SosWorkflowState.Idle
        }
    }

    private suspend fun executeSosWorkflow(
        settings: AppSettings,
        onSpeak: (String) -> Unit
    ) {
        _state.value = SosWorkflowState.PreparingLocation(isTestMode = settings.testModeEnabled)
        onSpeak("लोकेशन प्राप्त की जा रही है और आपातकालीन संदेश तैयार हो रहा है।")

        val locResult = locationRepository.getCurrentLocation()
        val (mapsUrl, locStatus, accuracy) = when (locResult) {
            is LocationResult.Success -> Triple(
                locResult.mapsUrl,
                "GPS लोकेशन प्राप्त हुई (सटीकता: ${locResult.accuracyMeters.toInt()} मीटर)",
                locResult.accuracyMeters
            )
            LocationResult.PermissionDenied -> Triple(
                null,
                "लोकेशन अनुमति नहीं मिली — संदेश बिना GPS लिंक के तैयार किया गया।",
                null
            )
            LocationResult.GpsDisabled -> Triple(
                null,
                "फोन का GPS बंद है — संदेश बिना GPS लिंक के तैयार किया गया।",
                null
            )
            is LocationResult.Error -> Triple(
                null,
                locResult.messageHindi,
                null
            )
        }

        val smsBody = buildString {
            append(settings.emergencySmsMessage.trim())
            if (mapsUrl != null) {
                append(" ")
                append(mapsUrl)
            } else {
                append(" (GPS लोकेशन अनुपलब्ध)")
            }
        }

        val primary = settings.primaryContact
        val secondary = settings.secondaryContact

        val summary = when {
            settings.testModeEnabled ->
                "टेस्ट मोड: कोई वास्तविक कॉल या SMS नहीं भेजा गया। संदेश और लोकेशन लिंक तैयार हैं।"
            !primary.isConfigured ->
                "कोई प्राथमिक आपातकालीन संपर्क सेट नहीं है। कृपया सेटिंग्स में नंबर जोड़ें।"
            isPublicEmergencyNumber(primary.phone) ->
                "सुरक्षा नियम: सार्वजनिक आपातकालीन नंबर (${primary.phone}) पर स्वचालित कॉल अवरुद्ध है।"
            else ->
                "सिस्टम SMS और डायलर तैयार किए जा रहे हैं। सुरक्षा नीति के अनुसार कृपया भेजें/कॉल बटन दबाएं।"
        }

        _state.value = SosWorkflowState.ReadyOrDispatched(
            isTestMode = settings.testModeEnabled,
            primaryContact = primary,
            secondaryContact = secondary,
            preparedSmsText = smsBody,
            mapsLink = mapsUrl,
            locationStatusHindi = locStatus,
            accuracyMeters = accuracy,
            statusSummaryHindi = summary
        )

        onSpeak(summary)

        // In Live mode (not Test Mode), launch the Android-compliant system SMS interface if primary contact exists
        if (!settings.testModeEnabled && primary.isConfigured && !isPublicEmergencyNumber(primary.phone)) {
            openSmsComposer(primary.phone, smsBody)
        }
    }

    fun openSmsComposer(phone: String, messageBody: String): Boolean {
        val cleanPhone = phone.filter { it.isDigit() || it == '+' }
        if (cleanPhone.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openDialer(phone: String, isTestMode: Boolean): Pair<Boolean, String> {
        val cleanPhone = phone.filter { it.isDigit() || it == '+' }
        if (cleanPhone.isBlank()) {
            return false to "कोई आपातकालीन नंबर कॉन्फ़िगर नहीं है। सेटिंग्स में नंबर जोड़ें।"
        }
        if (isTestMode) {
            return true to "टेस्ट मोड सक्रिय है: $cleanPhone पर वास्तविक कॉल नहीं किया गया।"
        }
        if (isPublicEmergencyNumber(cleanPhone)) {
            return false to "सुरक्षा नीति: स्वचालित कॉल सार्वजनिक आपातकालीन नंबर ($cleanPhone) के लिए बंद है।"
        }
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true to "$cleanPhone के लिए डायलर खोला जा रहा है।"
        } catch (_: Exception) {
            false to "फोन डायलर ऐप नहीं खुल सका।"
        }
    }

    private fun isPublicEmergencyNumber(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits in blockedPublicEmergencyNumbers
    }
}

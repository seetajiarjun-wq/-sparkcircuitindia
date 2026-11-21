package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AiProviderMode
import com.example.data.AppSettings
import com.example.data.VoiceLanguageOption
import com.example.location.LocationResult
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SonarCyan
import com.example.ui.theme.SosCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    locationTestResult: LocationResult?,
    onSaveContacts: (String, String, String, String, String) -> Unit,
    onSaveEspSettings: (String, Long) -> Unit,
    onSaveAiSettings: (AiProviderMode, String) -> Unit,
    onSaveVoiceAndAccessibility: (VoiceLanguageOption, Float, Boolean, Boolean, Boolean) -> Unit,
    onToggleTestMode: (Boolean) -> Unit,
    onToggleHardwareSimulator: (Boolean) -> Unit,
    onTestLocation: () -> Unit,
    onNavigateGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var primaryName by remember(settings.primaryContact) { mutableStateOf(settings.primaryContact.name) }
    var primaryPhone by remember(settings.primaryContact) { mutableStateOf(settings.primaryContact.phone) }
    var secondaryName by remember(settings.secondaryContact) { mutableStateOf(settings.secondaryContact.name) }
    var secondaryPhone by remember(settings.secondaryContact) { mutableStateOf(settings.secondaryContact.phone) }
    var smsTemplate by remember(settings.emergencySmsMessage) { mutableStateOf(settings.emergencySmsMessage) }

    var espUrl by remember(settings.espBaseUrl) { mutableStateOf(settings.espBaseUrl) }
    var pollIntervalSec by remember(settings.pollingIntervalMs) {
        mutableFloatStateOf(settings.pollingIntervalMs / 1000f)
    }

    var selectedAiMode by remember(settings.aiProviderMode) { mutableStateOf(settings.aiProviderMode) }
    var customAiUrl by remember(settings.customAiBackendUrl) { mutableStateOf(settings.customAiBackendUrl) }

    var selectedVoiceLang by remember(settings.voiceLanguage) { mutableStateOf(settings.voiceLanguage) }
    var speechSpeed by remember(settings.speechSpeed) { mutableFloatStateOf(settings.speechSpeed) }
    var autoSpeak by remember(settings.autoSpeakAlerts) { mutableStateOf(settings.autoSpeakAlerts) }
    var highContrast by remember(settings.highContrastMode) { mutableStateOf(settings.highContrastMode) }
    var largeText by remember(settings.largeTextMode) { mutableStateOf(settings.largeTextMode) }

    var cameraGranted by remember { mutableStateOf(false) }
    var micGranted by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) { refreshPermissions() }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "सेटिंग्स और सुरक्षा",
            style = MaterialTheme.typography.headlineMedium,
            color = SonarCyan
        )

        // 1. Test Mode & Hardware Simulator
        SettingsSectionCard(title = "टेस्ट मोड और सिम्युलेशन (Safe Testing)") {
            RowSwitch(
                title = "SOS टेस्ट मोड (सुरक्षित परीक्षण)",
                subtitle = "चालू होने पर वास्तविक कॉल या SMS नहीं भेजा जाएगा",
                checked = settings.testModeEnabled,
                onCheckedChange = onToggleTestMode,
                tag = "settings_test_mode_switch"
            )
            RowSwitch(
                title = "ESP8266 हार्डवेयर सिम्युलेटर",
                subtitle = "बिना डिवाइस के डैशबोर्ड पर सेंसर व बटन टेस्ट करें",
                checked = settings.hardwareSimulatorEnabled,
                onCheckedChange = onToggleHardwareSimulator,
                tag = "settings_simulator_switch"
            )
        }

        // 2. Emergency Contacts & SMS
        SettingsSectionCard(title = "आपातकालीन संपर्क (Emergency Contacts)") {
            OutlinedTextField(
                value = primaryName,
                onValueChange = { primaryName = it },
                label = { Text("प्राथमिक संपर्क नाम") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("primary_contact_name_input")
            )
            OutlinedTextField(
                value = primaryPhone,
                onValueChange = { primaryPhone = it },
                label = { Text("प्राथमिक फ़ोन नंबर (+91...)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("primary_contact_phone_input")
            )
            OutlinedTextField(
                value = secondaryName,
                onValueChange = { secondaryName = it },
                label = { Text("द्वितीयक संपर्क नाम (वैकल्पिक)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = secondaryPhone,
                onValueChange = { secondaryPhone = it },
                label = { Text("द्वितीयक फ़ोन नंबर") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = smsTemplate,
                onValueChange = { smsTemplate = it },
                label = { Text("आपातकालीन SMS संदेश") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emergency_sms_input")
            )
            Button(
                onClick = {
                    onSaveContacts(
                        primaryName,
                        primaryPhone,
                        secondaryName,
                        secondaryPhone,
                        smsTemplate
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_contacts_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("संपर्क सुरक्षित करें")
            }
        }

        // 3. ESP8266 Wi-Fi & Polling Config
        SettingsSectionCard(title = "ESP8266 कनेक्शन और पोलिंग") {
            OutlinedTextField(
                value = espUrl,
                onValueChange = { espUrl = it },
                label = { Text("ESP8266 URL (डिफ़ॉल्ट: http://192.168.4.1/)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("esp_url_input")
            )
            Text(
                text = "पोलिंग अंतराल: ${(pollIntervalSec * 1000).toInt()} ms",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = pollIntervalSec,
                onValueChange = { pollIntervalSec = it },
                valueRange = 0.8f..5.0f
            )
            Button(
                onClick = { onSaveEspSettings(espUrl, (pollIntervalSec * 1000).toLong()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_esp_settings_button")
            ) {
                Text("ESP सेटिंग्स लागू करें")
            }
        }

        // 4. AI Vision Provider Settings
        SettingsSectionCard(title = "AI विज़न प्रदाता (API सुरक्षा)") {
            Text(
                text = "सुरक्षा नियम: APK में कोई गुप्त API कुंजी नहीं रखी जाती। Firebase AI Logic (App Check) या आपका कस्टम सर्वर प्रॉक्सी उपयोग करें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiProviderMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedAiMode == mode,
                        onClick = { selectedAiMode = mode },
                        label = { Text(mode.label) }
                    )
                }
            }
            Text(
                text = selectedAiMode.hindiDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = SonarCyan
            )
            if (selectedAiMode == AiProviderMode.CUSTOM_BACKEND) {
                OutlinedTextField(
                    value = customAiUrl,
                    onValueChange = { customAiUrl = it },
                    label = { Text("कस्टम बैकएंड URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = { onSaveAiSettings(selectedAiMode, customAiUrl) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_ai_settings_button")
            ) {
                Text("AI सेटिंग्स सहेजें")
            }
        }

        // 5. Voice & Accessibility Settings
        SettingsSectionCard(title = "आवाज़ और एक्सेसिबिलिटी") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceLanguageOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedVoiceLang == option,
                        onClick = { selectedVoiceLang = option },
                        label = { Text(option.displayLabel) }
                    )
                }
            }
            Text(
                text = "बोलने की गति (Speech Speed): ${String.format("%.2fx", speechSpeed)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = speechSpeed,
                onValueChange = { speechSpeed = it },
                valueRange = 0.6f..1.5f
            )
            RowSwitch(
                title = "बाधा/पानी अलर्ट अपने आप बोलें",
                subtitle = "खतरा मिलने पर तुरंत हिंदी वॉइस चेतावनी",
                checked = autoSpeak,
                onCheckedChange = { autoSpeak = it },
                tag = "auto_speak_switch"
            )
            RowSwitch(
                title = "हाई कॉन्ट्रास्ट बॉर्डर",
                subtitle = "कम दृष्टि वाले उपयोगकर्ताओं के लिए स्पष्ट बॉर्डर",
                checked = highContrast,
                onCheckedChange = { highContrast = it },
                tag = "high_contrast_switch"
            )
            RowSwitch(
                title = "अतिरिक्त बड़ा टेक्स्ट (Extra-Large Text)",
                subtitle = "स्क्रीन के सभी अक्षरों का आकार 15% बढ़ाएं",
                checked = largeText,
                onCheckedChange = { largeText = it },
                tag = "large_text_switch"
            )
            Button(
                onClick = {
                    onSaveVoiceAndAccessibility(
                        selectedVoiceLang,
                        speechSpeed,
                        autoSpeak,
                        highContrast,
                        largeText
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_accessibility_button")
            ) {
                Text("आवाज़ और डिस्प्ले सहेजें")
            }
        }

        // 6. Permissions & GPS Verification
        SettingsSectionCard(title = "अनुमतियाँ और GPS जांच") {
            PermissionRow("कैमरा (CameraX AI)", cameraGranted)
            PermissionRow("माइक्रोफ़ोन (हिंदी आवाज़)", micGranted)
            PermissionRow("लोकेशन (SOS GPS लिंक)", locationGranted)

            Button(
                onClick = {
                    multiPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("request_all_permissions_button")
            ) {
                Text("आवश्यक अनुमतियाँ दें")
            }

            OutlinedButton(
                onClick = onTestLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("test_gps_button")
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GPS लोकेशन और Maps लिंक जांचें")
            }

            if (locationTestResult != null) {
                val summary = when (locationTestResult) {
                    is LocationResult.Success ->
                        "GPS सक्रिय (${locationTestResult.accuracyMeters.toInt()}m सटीकता)\n${locationTestResult.mapsUrl}"
                    LocationResult.PermissionDenied -> "लोकेशन अनुमति नहीं दी गई है।"
                    LocationResult.GpsDisabled -> "फोन की Location/GPS सेवा बंद है।"
                    is LocationResult.Error -> locationTestResult.messageHindi
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SonarCyan
                )
            }
        }

        // 7. Guide & About
        SettingsSectionCard(title = "सेटअप गाइड और जानकारी") {
            Text(
                text = "ANDHA HELPER v1.0 • ESP8266 NodeMCU सहायक साथी ऐप\nWi-Fi SSID: ANDHA-HELPER | पासवर्ड: 12345678 | IP: 192.168.4.1",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = onNavigateGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_guide_from_settings_button")
            ) {
                Text("हार्डवेयर पिनआउट, Wi-Fi और समस्या निवारण गाइड खोलें")
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = SonarCyan
            )
            content()
        }
    }
}

@Composable
private fun RowSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) SafeEmerald else CautionAmber
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (granted) "अनुमति प्राप्त" else "लंबित",
                color = if (granted) SafeEmerald else CautionAmber,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

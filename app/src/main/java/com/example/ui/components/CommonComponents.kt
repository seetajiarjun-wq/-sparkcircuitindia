package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.emergency.SosWorkflowState
import com.example.sensors.EspButtonState
import com.example.sensors.EspConnectionStatus
import com.example.sensors.WifiStatusInfo
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SonarCyan
import com.example.ui.theme.SosCrimson

@Composable
fun ConnectionStatusCard(
    connectionStatus: EspConnectionStatus,
    wifiStatus: WifiStatusInfo,
    endpointUrl: String,
    deviceConnectedFlag: Boolean,
    errorMessageHindi: String?,
    isTestMode: Boolean,
    onSpeakStatus: () -> Unit,
    onReconnectNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            EspConnectionStatus.CONNECTED -> SafeEmerald
            EspConnectionStatus.SIMULATED -> SonarCyan
            EspConnectionStatus.CONNECTING -> CautionAmber
            EspConnectionStatus.DISCONNECTED -> SosCrimson
        },
        label = "connAccent"
    )

    Card(
        onClick = onSpeakStatus,
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_status_card")
            .semantics {
                contentDescription = "ESP8266 स्थिति: ${connectionStatus.hindiLabel}, Wi-Fi: ${wifiStatus.ssid}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (connectionStatus) {
                            EspConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                            EspConnectionStatus.SIMULATED -> Icons.Default.Science
                            EspConnectionStatus.CONNECTING -> Icons.Default.Sync
                            EspConnectionStatus.DISCONNECTED -> Icons.Default.WifiOff
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = connectionStatus.hindiLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wi-Fi: ${wifiStatus.ssid}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "API: $endpointUrl • JSON connected=$deviceConnectedFlag",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTestMode) {
                        Surface(
                            color = CautionAmber.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, CautionAmber)
                        ) {
                            Text(
                                text = "TEST MODE",
                                style = MaterialTheme.typography.labelMedium,
                                color = CautionAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onReconnectNow,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("esp_reconnect_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "ESP8266 से पुनः जुड़ें",
                            tint = SonarCyan
                        )
                    }
                }
            }

            if (!errorMessageHindi.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessageHindi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HardwareSimulatorCard(
    simulatorEnabled: Boolean,
    onToggleSimulator: (Boolean) -> Unit,
    currentDistance: Int,
    waterDetected: Boolean,
    onSimulateDistance: (Int) -> Unit,
    onSimulateWater: (Boolean) -> Unit,
    onSimulateButton: (EspButtonState) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            1.dp,
            if (simulatorEnabled) SonarCyan.copy(alpha = 0.7f) else CautionAmber.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = if (simulatorEnabled) SonarCyan else CautionAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "हार्डवेयर सिम्युलेटर (TEST MODE)",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (simulatorEnabled) SonarCyan else CautionAmber
                        )
                        Text(
                            text = if (simulatorEnabled) "सिम्युलेटेड सेंसर चालू हैं"
                            else "बंद: वास्तविक ESP8266 (192.168.4.1) पोल हो रहा है",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = simulatorEnabled,
                    onCheckedChange = onToggleSimulator,
                    modifier = Modifier.testTag("dashboard_simulator_switch")
                )
            }

            if (simulatorEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                FilterChip(
                    selected = currentDistance in 1..60,
                    onClick = { onSimulateDistance(38) },
                    label = { Text("बाधा 38 cm") },
                    modifier = Modifier.testTag("sim_obstacle_chip")
                )
                FilterChip(
                    selected = currentDistance > 100,
                    onClick = { onSimulateDistance(152) },
                    label = { Text("साफ 152 cm") },
                    modifier = Modifier.testTag("sim_clear_chip")
                )
                FilterChip(
                    selected = waterDetected,
                    onClick = { onSimulateWater(!waterDetected) },
                    label = { Text(if (waterDetected) "पानी: चालू" else "पानी अलर्ट") },
                    modifier = Modifier.testTag("sim_water_chip")
                )
                FilterChip(
                    selected = false,
                    onClick = { onSimulateButton(EspButtonState.ASK) },
                    label = { Text("बटन ASK") },
                    modifier = Modifier.testTag("sim_btn_ask")
                )
                FilterChip(
                    selected = false,
                    onClick = { onSimulateButton(EspButtonState.CHECK) },
                    label = { Text("बटन CHECK") },
                    modifier = Modifier.testTag("sim_btn_check")
                )
                FilterChip(
                    selected = false,
                    onClick = { onSimulateButton(EspButtonState.CALL) },
                    label = { Text("बटन CALL") },
                    modifier = Modifier.testTag("sim_btn_call")
                )
                FilterChip(
                    selected = false,
                    onClick = { onSimulateButton(EspButtonState.SOS) },
                    label = { Text("बटन SOS") },
                    modifier = Modifier.testTag("sim_btn_sos")
                )
                }
            }
        }
    }
}

@Composable
fun SosWorkflowDialog(
    sosState: SosWorkflowState,
    onCancelSos: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSms: (String, String) -> Unit,
    onOpenDialer: (String) -> Unit
) {
    if (sosState is SosWorkflowState.Idle) return

    AlertDialog(
        onDismissRequest = {
            if (sosState !is SosWorkflowState.Countdown &&
                sosState !is SosWorkflowState.PreparingLocation
            ) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "आपातकालीन SOS चेतावनी",
                tint = SosCrimson,
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                text = when (sosState) {
                    is SosWorkflowState.Countdown ->
                        "आपातकालीन SOS (${sosState.secondsRemaining}s)"
                    is SosWorkflowState.PreparingLocation ->
                        "GPS लोकेशन प्राप्त हो रही है…"
                    is SosWorkflowState.ReadyOrDispatched ->
                        if (sosState.isTestMode) "SOS टेस्ट मोड परिणाम" else "आपातकालीन SOS तैयार है"
                    is SosWorkflowState.Cancelled ->
                        "SOS रद्द किया गया"
                    SosWorkflowState.Idle -> ""
                },
                style = MaterialTheme.typography.headlineMedium,
                color = SosCrimson
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (sosState) {
                    is SosWorkflowState.Countdown -> {
                        if (sosState.isTestMode) {
                            Text(
                                text = "टेस्ट मोड सक्रिय है: कोई वास्तविक कॉल या SMS नहीं जाएगा।",
                                color = CautionAmber,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "स्रोत: ${sosState.triggerSource}\nगलती से दबने पर नीचे दिए बड़े लाल/सफ़ेद बटन से अभी रद्द करें।",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    is SosWorkflowState.PreparingLocation -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = SosCrimson,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Google Maps लिंक और SMS संदेश तैयार किया जा रहा है…",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is SosWorkflowState.ReadyOrDispatched -> {
                        if (sosState.isTestMode) {
                            Surface(
                                color = CautionAmber.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, CautionAmber)
                            ) {
                                Text(
                                    text = "TEST MODE: वास्तविक कॉल या SMS नहीं भेजा गया।",
                                    color = CautionAmber,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Text(
                            text = sosState.statusSummaryHindi,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SonarCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sosState.locationStatusHindi,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "तैयार SMS:\n${sosState.preparedSmsText}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        val targetPhone = sosState.primaryContact.phone.ifBlank {
                            sosState.secondaryContact.phone
                        }
                        val targetName = sosState.primaryContact.name.ifBlank {
                            sosState.secondaryContact.name.ifBlank { "आपातकालीन संपर्क" }
                        }

                        if (targetPhone.isNotBlank()) {
                            Button(
                                onClick = { onOpenSms(targetPhone, sosState.preparedSmsText) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("sos_open_sms_button"),
                                enabled = !sosState.isTestMode
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SMS ऐप खोलें ($targetName)")
                            }

                            Button(
                                onClick = { onOpenDialer(targetPhone) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SosCrimson
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("sos_open_dialer_button")
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (sosState.isTestMode) "टेस्ट कॉल सिम्युलेट ($targetPhone)"
                                    else "कॉल डायलर खोलें ($targetName)"
                                )
                            }
                        }
                    }

                    is SosWorkflowState.Cancelled -> {
                        Text(
                            text = sosState.messageHindi,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    SosWorkflowState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            if (sosState is SosWorkflowState.Countdown) {
                Button(
                    onClick = onCancelSos,
                    colors = ButtonDefaults.buttonColors(containerColor = SosCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("cancel_sos_button")
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SOS रद्द करें (CANCEL)", style = MaterialTheme.typography.titleMedium)
                }
            } else if (sosState !is SosWorkflowState.PreparingLocation) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dismiss_sos_button")
                ) {
                    Text("बंद करें (Close)")
                }
            }
        }
    )
}

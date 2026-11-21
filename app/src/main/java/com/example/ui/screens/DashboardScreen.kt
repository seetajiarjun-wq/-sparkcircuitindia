package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppSettings
import com.example.sensors.EspButtonState
import com.example.sensors.EspConnectionStatus
import com.example.sensors.EspSensorData
import com.example.sensors.WifiStatusInfo
import com.example.ui.components.ConnectionStatusCard
import com.example.ui.components.HardwareSimulatorCard
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SonarCyan
import com.example.ui.theme.SosCrimson
import com.example.ui.theme.TelemetryMonoFontFamily

@Composable
fun DashboardScreen(
    sensorData: EspSensorData,
    connectionStatus: EspConnectionStatus,
    wifiStatus: WifiStatusInfo,
    errorMessageHindi: String?,
    settings: AppSettings,
    statusBanner: String?,
    onNavigateCamera: () -> Unit,
    onNavigateVoice: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateGuide: () -> Unit,
    onTriggerSos: () -> Unit,
    onSpeakAllStatus: () -> Unit,
    onSpeakConnectionStatus: () -> Unit,
    onSpeakObstacleStatus: () -> Unit,
    onSpeakWaterStatus: () -> Unit,
    onCallPrimaryContact: () -> Unit,
    onSimulateDistance: (Int) -> Unit,
    onSimulateWater: (Boolean) -> Unit,
    onSimulateButton: (EspButtonState) -> Unit,
    onToggleSimulator: (Boolean) -> Unit,
    onReconnectNow: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Branded Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_esp_hardware_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.28f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ANDHA HELPER",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "अंधा हेल्पर • दृष्टिबाधित सहायक प्रणाली",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onSpeakAllStatus,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("header_speak_status_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "पूरा स्टेटस हिंदी में सुनें",
                                tint = SonarCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!statusBanner.isNullOrBlank()) {
            item {
                Card(
                    onClick = onDismissBanner,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // ESP8266 & Wi-Fi Connection Status
        item {
            ConnectionStatusCard(
                connectionStatus = connectionStatus,
                wifiStatus = wifiStatus,
                endpointUrl = "${settings.espBaseUrl}sensors",
                deviceConnectedFlag = sensorData.deviceConnectedFlag,
                errorMessageHindi = errorMessageHindi,
                isTestMode = settings.testModeEnabled,
                onSpeakStatus = onSpeakConnectionStatus,
                onReconnectNow = onReconnectNow
            )
        }

        // Distance Sonar Hero Card
        item {
            DistanceSonarHeroCard(
                distanceCm = sensorData.distanceCm,
                obstacleAlert = sensorData.obstacleAlert,
                onClickSpeak = onSpeakObstacleStatus
            )
        }

        // Primary Action Buttons (Huge High-Contrast Touch Targets)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AccessibleActionTile(
                        title = "कैमरा AI",
                        subtitle = "सामने क्या है?",
                        icon = Icons.Default.CameraAlt,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        accentColor = SonarCyan,
                        onClick = onNavigateCamera,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_camera_button")
                    )
                    AccessibleActionTile(
                        title = "हिंदी आवाज़",
                        subtitle = "बोलकर पूछें",
                        icon = Icons.Default.Mic,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        accentColor = SafeEmerald,
                        onClick = onNavigateVoice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_voice_button")
                    )
                }

                // Prominent Red SOS Emergency Button
                Button(
                    onClick = onTriggerSos,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SosCrimson,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("dashboard_sos_button")
                        .semantics {
                            contentDescription = "आपातकालीन SOS बटन। दबाने पर 5 सेकंड की गिनती शुरू होगी।"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (settings.testModeEnabled) "आपातकालीन SOS (TEST MODE)" else "आपातकालीन SOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GPS लोकेशन + SMS + कॉल सहायता",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Sensor Telemetry Cards: Water + Obstacle/Vibration
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SensorMetricCard(
                    title = "पानी सेंसर (Water)",
                    primaryValue = if (sensorData.waterDetected) "पानी मिला!" else "सुरक्षित",
                    secondaryValue = "Raw ADC: ${sensorData.waterRaw}",
                    icon = Icons.Default.WaterDrop,
                    accentColor = if (sensorData.waterDetected) CautionAmber else SafeEmerald,
                    isAlert = sensorData.waterDetected,
                    onClick = onSpeakWaterStatus,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("water_sensor_card")
                )

                SensorMetricCard(
                    title = "वाइब्रेशन व बाधा",
                    primaryValue = if (sensorData.obstacleAlert) "बाधा अलर्ट!" else "सामान्य",
                    secondaryValue = if (sensorData.vibrationActive) "Vib: चालू (ON)" else "Vib: बंद (OFF)",
                    icon = Icons.Default.Vibration,
                    accentColor = if (sensorData.obstacleAlert) SosCrimson else SonarCyan,
                    isAlert = sensorData.obstacleAlert,
                    onClick = onSpeakObstacleStatus,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("obstacle_vibration_card")
                )
            }
        }

        // Last Button & Current Alert / LCD Message Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = SonarCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "अंतिम बटन: ${sensorData.lastButton.hindiLabel}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Surface(
                            color = if (sensorData.alertMessage != "NONE") SosCrimson.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (sensorData.alertMessage != "NONE") SosCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sensorData.alertMessage,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Text(
                        text = "ESP LCD संदेश: ${sensorData.lcdMessage}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Optional Hardware Simulator when enabled in Settings or Test Mode
        if (settings.hardwareSimulatorEnabled || settings.testModeEnabled) {
            item {
                HardwareSimulatorCard(
                    simulatorEnabled = settings.hardwareSimulatorEnabled,
                    onToggleSimulator = onToggleSimulator,
                    currentDistance = sensorData.distanceCm,
                    waterDetected = sensorData.waterDetected,
                    onSimulateDistance = onSimulateDistance,
                    onSimulateWater = onSimulateWater,
                    onSimulateButton = onSimulateButton
                )
            }
        }

        // Quick Secondary Actions: Call Contact, Settings, Setup Guide
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCallPrimaryContact,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("dashboard_call_button")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("कॉल")
                }

                OutlinedButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("dashboard_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("सेटिंग्स")
                }

                OutlinedButton(
                    onClick = onNavigateGuide,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("dashboard_guide_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("गाइड")
                }
            }
        }

        // Safety Disclaimer Footer
        item {
            Text(
                text = "सावधानी: AI और अल्ट्रासोनिक सेंसर सभी खतरों की पहचान की गारंटी नहीं देते। कृपया पारंपरिक सुरक्षा सावधानी रखें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DistanceSonarHeroCard(
    distanceCm: Int,
    obstacleAlert: Boolean,
    onClickSpeak: () -> Unit
) {
    val ringColor = when {
        obstacleAlert -> SosCrimson
        distanceCm in 1..100 -> CautionAmber
        else -> SonarCyan
    }

    Card(
        onClick = onClickSpeak,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("distance_hero_card")
            .semantics {
                contentDescription = if (distanceCm >= 0) {
                    "सामने की दूरी $distanceCm सेंटीमीटर। सुनने के लिए टैप करें।"
                } else {
                    "दूरी सेंसर डेटा उपलब्ध नहीं है।"
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(2.dp, ringColor.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = ringColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "अल्ट्रासोनिक दूरी (HC-SR04)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (distanceCm >= 0) "$distanceCm cm" else "-- cm",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ringColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        distanceCm < 0 -> "सेंसर प्रतीक्षा में (टैप करें सुनने के लिए)"
                        obstacleAlert -> "चेतावनी: बहुत नज़दीक बाधा!"
                        distanceCm <= 100 -> "सावधान: मध्यम दूरी पर वस्तु"
                        else -> "रास्ता अपेक्षाकृत खुला है"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Custom Canvas Sonar Arc Visualizer
            Canvas(modifier = Modifier.size(86.dp)) {
                val center = Offset(size.width / 2f, size.height * 0.78f)
                val maxR = size.width * 0.45f
                val intensity = if (distanceCm <= 0) 0.25f else (1f - (distanceCm.coerceIn(10, 250) / 250f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ringColor.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = maxR
                    ),
                    radius = maxR,
                    center = center
                )
                for (i in 1..3) {
                    val r = maxR * (i / 3f)
                    val alpha = if (intensity >= (i - 1) * 0.3f) 0.9f else 0.25f
                    drawArc(
                        color = ringColor.copy(alpha = alpha),
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }
                drawCircle(color = ringColor, radius = 8f, center = center)
            }
        }
    }
}

@Composable
private fun AccessibleActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SensorMetricCard(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    icon: ImageVector,
    accentColor: Color,
    isAlert: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) accentColor.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = if (isAlert) 0.9f else 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

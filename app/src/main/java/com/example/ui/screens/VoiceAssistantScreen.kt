package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SonarCyan
import com.example.ui.theme.SosCrimson
import com.example.voice.VoiceAssistantState

private val QuickHindiCommands = listOf(
    "मेरे सामने क्या है?",
    "सेंसर स्टेटस बताओ",
    "आपातकालीन कॉल करें",
    "मदद SOS"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantScreen(
    voiceState: VoiceAssistantState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSubmitCommand: (String) -> Unit,
    onSpeakConnectionStatus: () -> Unit,
    onSpeakSensorStatus: () -> Unit,
    onSpeakObstacleStatus: () -> Unit,
    onSpeakWaterStatus: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            onStartListening()
        }
    }

    var manualInput by remember { mutableStateOf("") }

    val pulseTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState.isListening) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "हिंदी आवाज़ सहायक",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SafeEmerald
                )
                Text(
                    text = if (voiceState.hindiVoiceAvailable) "Hindi TTS तैयार है"
                    else "डिफ़ॉल्ट TTS सक्रिय है",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onStopSpeaking,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("stop_tts_button")
            ) {
                Icon(Icons.Default.VolumeOff, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("चुप करें")
            }
        }

        // Giant Accessible Microphone Button
        Button(
            onClick = {
                if (voiceState.isListening) {
                    onStopListening()
                } else if (!hasMicPermission) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    onStartListening()
                }
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (voiceState.isListening) SosCrimson else SafeEmerald
            ),
            modifier = Modifier
                .size(144.dp)
                .scale(micScale)
                .testTag("voice_mic_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (voiceState.isListening) "सुनना बंद करें" else "हिंदी में बोलें",
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (voiceState.isListening) "सुन रहा है…" else "बोलें",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Spoken Transcript & Response Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.5.dp, SonarCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (voiceState.partialTranscript.isNotBlank() || voiceState.lastSpokenInput.isNotBlank()) {
                    Text(
                        text = "आपने कहा: ${voiceState.partialTranscript.ifBlank { voiceState.lastSpokenInput }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SonarCyan
                    )
                }
                Text(
                    text = voiceState.lastAssistantResponse.ifBlank {
                        "माइक बटन दबाएं और बोलें: 'मेरे सामने क्या है?' या 'सेंसर स्टेटस बताओ'।"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!voiceState.voiceError.isNullOrBlank()) {
                    Text(
                        text = voiceState.voiceError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SosCrimson
                    )
                }
            }
        }

        // Spoken Hardware Status Shortcuts
        Text(
            text = "त्वरित आवाज़ स्थिति (Spoken Status)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSpeakConnectionStatus,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("speak_connection_btn")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("कनेक्शन सुनें")
                }
                OutlinedButton(
                    onClick = onSpeakSensorStatus,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("speak_sensors_btn")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("पूरा स्टेटस सुनें")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSpeakObstacleStatus,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("speak_obstacle_btn")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("बाधा अलर्ट सुनें")
                }
                OutlinedButton(
                    onClick = onSpeakWaterStatus,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("speak_water_btn")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("पानी अलर्ट सुनें")
                }
            }
        }

        // Quick Hindi Voice Command Chips
        Text(
            text = "उदाहरण कमांड (टैप करके आज़माएं):",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickHindiCommands.forEach { cmd ->
                FilterChip(
                    selected = false,
                    onClick = { onSubmitCommand(cmd) },
                    label = { Text(cmd) }
                )
            }
        }

        // Manual text query fallback when microphone/Google speech service is offline
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                label = { Text("हिंदी कमांड टाइप करें") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_voice_command_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (manualInput.isNotBlank()) {
                        onSubmitCommand(manualInput.trim())
                        manualInput = ""
                    }
                },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("manual_voice_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "कमांड भेजें",
                    tint = SonarCyan
                )
            }
        }
    }
}

package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ai.VisionUiState
import com.example.data.AiProviderMode
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SonarCyan
import com.example.ui.theme.SosCrimson
import kotlinx.coroutines.delay

private const val TAG = "CameraVisionScreen"

private val HindiCameraPrompts = listOf(
    "मेरे सामने क्या है?",
    "इस तस्वीर में क्या दिखाई दे रहा है?",
    "क्या कोई वस्तु दिखाई दे रही है?",
    "इस दृश्य का संक्षिप्त वर्णन करो।"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraVisionScreen(
    visionState: VisionUiState,
    selectedPrompt: String,
    pendingAutoCapturePrompt: String?,
    providerMode: AiProviderMode,
    onSelectPrompt: (String) -> Unit,
    onConsumeAutoCapture: () -> String?,
    onSetCapturing: () -> Unit,
    onImageCaptured: (Bitmap, String?) -> Unit,
    onCameraError: (String) -> Unit,
    onRetry: () -> Unit,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onCameraError("कैमरा अनुमति नहीं मिली। कृपया सेटिंग्स या नीचे दिए बटन से अनुमति दें।")
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    fun triggerCapture(promptOverride: String? = null) {
        val captureUseCase = imageCapture
        if (captureUseCase == null) {
            onCameraError("कैमरा अभी तैयार नहीं है। कृपया एक क्षण प्रतीक्षा करें।")
            return
        }
        onSetCapturing()
        captureUseCase.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = imageProxyToRotatedBitmap(image)
                        image.close()
                        onImageCaptured(bitmap, promptOverride)
                    } catch (e: Exception) {
                        image.close()
                        onCameraError("फोटो प्रोसेस करने में त्रुटि हुई।")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                    onCameraError("तस्वीर खींचने में समस्या आई: ${exception.localizedMessage ?: "त्रुटि"}")
                }
            }
        )
    }

    // Handle auto-capture requested by ESP ASK button or Hindi voice command
    LaunchedEffect(pendingAutoCapturePrompt, hasCameraPermission, imageCapture) {
        if (pendingAutoCapturePrompt != null && hasCameraPermission && imageCapture != null) {
            delay(500L)
            val prompt = onConsumeAutoCapture()
            if (prompt != null) {
                triggerCapture(prompt)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "कैमरा AI दृश्य विवरण",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SonarCyan
                )
                Text(
                    text = "प्रदाता: ${providerMode.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!hasCameraPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, CautionAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "सामने का दृश्य पहचानने के लिए कैमरा अनुमति आवश्यक है।",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("grant_camera_permission_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("कैमरा अनुमति दें")
                    }
                }
            }
        } else {
            // CameraX Live Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                border = BorderStroke(2.dp, SonarCyan.copy(alpha = 0.7f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val previewView = remember { PreviewView(context) }

                    DisposableEffect(lifecycleOwner) {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        val listener = Runnable {
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "CameraX binding failed", e)
                                onCameraError("इस डिवाइस पर बैक कैमरा शुरू नहीं हो सका।")
                            }
                        }
                        cameraProviderFuture.addListener(
                            listener,
                            ContextCompat.getMainExecutor(context)
                        )
                        onDispose {
                            try {
                                cameraProviderFuture.get().unbindAll()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Hindi Prompt Preset Chips
        Text(
            text = "प्रश्न चुनें:",
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HindiCameraPrompts.forEach { prompt ->
                FilterChip(
                    selected = selectedPrompt == prompt,
                    onClick = { onSelectPrompt(prompt) },
                    label = { Text(prompt) }
                )
            }
        }

        // Huge Accessible Capture Button
        Button(
            onClick = {
                if (!hasCameraPermission) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    triggerCapture(selectedPrompt)
                }
            },
            enabled = visionState !is VisionUiState.Capturing && visionState !is VisionUiState.Analyzing,
            colors = ButtonDefaults.buttonColors(containerColor = SonarCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("capture_analyze_button")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "तस्वीर लें और हिंदी में बताएं",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Vision Result / Loading / Error Card
        when (visionState) {
            VisionUiState.Idle -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ऊपर दिए बटन को दबाएं या 'मेरे सामने क्या है?' बोलें। AI दृश्य का संक्षिप्त हिंदी विवरण पढ़कर सुनाएगा।",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            VisionUiState.Capturing, VisionUiState.Analyzing -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, SonarCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = SonarCyan, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (visionState is VisionUiState.Capturing) "तस्वीर ली जा रही है…"
                            else "AI द्वारा दृश्य का विश्लेषण किया जा रहा है…",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            is VisionUiState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.5.dp, SafeEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vision_result_card")
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
                            Text(
                                text = if (visionState.isDemoResponse) "AI उत्तर (डेमो मोड)" else "AI दृश्य विवरण",
                                style = MaterialTheme.typography.titleMedium,
                                color = SafeEmerald
                            )
                            Surface(
                                color = SafeEmerald.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = visionState.providerLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SafeEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = visionState.descriptionHindi,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedButton(
                            onClick = { onSpeakText(visionState.descriptionHindi) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("vision_respeak_button")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("दोबारा सुनें")
                        }
                    }
                }
            }

            is VisionUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.5.dp, SosCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vision_error_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = visionState.messageHindi,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("retry_vision_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("पुनः प्रयास करें (Retry)")
                        }
                    }
                }
            }
        }

        Text(
            text = "चेतावनी: AI दृश्य विवरण केवल सहायता के लिए है। यह सभी बाधाओं, गड्ढों या खतरों की पहचान करने और सुरक्षित नेविगेशन की गारंटी नहीं देता।",
            style = MaterialTheme.typography.bodyMedium,
            color = CautionAmber
        )
    }
}

private fun imageProxyToRotatedBitmap(image: ImageProxy): Bitmap {
    val rawBitmap = image.toBitmap()
    val rotationDegrees = image.imageInfo.rotationDegrees
    if (rotationDegrees == 0) return rawBitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
}

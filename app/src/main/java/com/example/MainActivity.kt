package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.ui.components.SosWorkflowDialog
import com.example.ui.navigation.CameraRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.GuideRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.VoiceRoute
import com.example.ui.screens.CameraVisionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupAndGuideScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.AndhaHelperTheme
import com.example.viewmodel.AndhaHelperViewModel
import com.example.viewmodel.AppNavigationEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring before Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AndhaHelperRootApp()
        }
    }
}

private data class NavTabItem(
    val label: String,
    val route: Any,
    val testTag: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AndhaHelperRootApp(
    viewModel: AndhaHelperViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val wifiStatus by viewModel.wifiStatus.collectAsStateWithLifecycle()
    val sensorErrorHindi by viewModel.sensorErrorHindi.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val visionState by viewModel.visionState.collectAsStateWithLifecycle()
    val selectedCameraPrompt by viewModel.selectedCameraPrompt.collectAsStateWithLifecycle()
    val pendingAutoCapturePrompt by viewModel.pendingAutoCapturePrompt.collectAsStateWithLifecycle()
    val sosState by viewModel.sosState.collectAsStateWithLifecycle()
    val locationTestResult by viewModel.locationTestResult.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppForegroundChanged(true)
                Lifecycle.Event.ON_STOP -> viewModel.onAppForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { navEvent ->
            when (navEvent) {
                is AppNavigationEvent.OpenCameraAndCapture -> {
                    navController.navigate(CameraRoute) {
                        launchSingleTop = true
                    }
                }
                AppNavigationEvent.OpenVoiceAssistant -> {
                    navController.navigate(VoiceRoute) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val navTabs = listOf(
        NavTabItem("डैशबोर्ड", DashboardRoute, "nav_tab_dashboard", Icons.Default.Dashboard),
        NavTabItem("कैमरा AI", CameraRoute, "nav_tab_camera", Icons.Default.CameraAlt),
        NavTabItem("आवाज़", VoiceRoute, "nav_tab_voice", Icons.Default.Mic),
        NavTabItem("सेटिंग्स", SettingsRoute, "nav_tab_settings", Icons.Default.Settings)
    )

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    AndhaHelperTheme(
        highContrast = settings.highContrastMode,
        largeTextMode = settings.largeTextMode
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar {
                            navTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    DashboardRoute -> currentDest?.hasRoute<DashboardRoute>() == true
                                    CameraRoute -> currentDest?.hasRoute<CameraRoute>() == true
                                    VoiceRoute -> currentDest?.hasRoute<VoiceRoute>() == true
                                    SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true ||
                                        currentDest?.hasRoute<GuideRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail {
                            navTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    DashboardRoute -> currentDest?.hasRoute<DashboardRoute>() == true
                                    CameraRoute -> currentDest?.hasRoute<CameraRoute>() == true
                                    VoiceRoute -> currentDest?.hasRoute<VoiceRoute>() == true
                                    SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true ||
                                        currentDest?.hasRoute<GuideRoute>() == true
                                    else -> false
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<DashboardRoute> {
                            DashboardScreen(
                                sensorData = sensorData,
                                connectionStatus = connectionStatus,
                                wifiStatus = wifiStatus,
                                errorMessageHindi = sensorErrorHindi,
                                settings = settings,
                                statusBanner = statusBanner,
                                onNavigateCamera = { navigateToTab(CameraRoute) },
                                onNavigateVoice = { navigateToTab(VoiceRoute) },
                                onNavigateSettings = { navigateToTab(SettingsRoute) },
                                onNavigateGuide = { navController.navigate(GuideRoute) },
                                onTriggerSos = { viewModel.triggerSosWorkflow() },
                                onSpeakAllStatus = { viewModel.speakFullSensorAndConnectionStatus() },
                                onSpeakConnectionStatus = { viewModel.speakConnectionStatus() },
                                onSpeakObstacleStatus = { viewModel.speakObstacleStatus() },
                                onSpeakWaterStatus = { viewModel.speakWaterStatus() },
                                onCallPrimaryContact = { viewModel.initiateCallToPrimaryContact() },
                                onSimulateDistance = { viewModel.simulateDistance(it) },
                                onSimulateWater = { viewModel.simulateWater(it) },
                                onSimulateButton = { viewModel.simulateButton(it) },
                                onToggleSimulator = { viewModel.setHardwareSimulator(it) },
                                onReconnectNow = { viewModel.reconnectEspNow() },
                                onDismissBanner = { viewModel.clearStatusBanner() }
                            )
                        }

                        composable<CameraRoute> {
                            CameraVisionScreen(
                                visionState = visionState,
                                selectedPrompt = selectedCameraPrompt,
                                pendingAutoCapturePrompt = pendingAutoCapturePrompt,
                                providerMode = settings.aiProviderMode,
                                onSelectPrompt = { viewModel.selectCameraPrompt(it) },
                                onConsumeAutoCapture = { viewModel.consumePendingAutoCapturePrompt() },
                                onSetCapturing = { viewModel.setCameraCapturingState() },
                                onImageCaptured = { bmp, overridePrompt ->
                                    viewModel.analyzeCapturedImage(bmp, overridePrompt)
                                },
                                onCameraError = { viewModel.onCameraError(it) },
                                onRetry = { viewModel.retryLastVisionAnalysis() },
                                onSpeakText = { viewModel.speakText(it) }
                            )
                        }

                        composable<VoiceRoute> {
                            VoiceAssistantScreen(
                                voiceState = voiceState,
                                onStartListening = { viewModel.startVoiceListening() },
                                onStopListening = { viewModel.stopVoiceListening() },
                                onSubmitCommand = { viewModel.submitTextVoiceCommand(it) },
                                onSpeakConnectionStatus = { viewModel.speakConnectionStatus() },
                                onSpeakSensorStatus = { viewModel.speakFullSensorAndConnectionStatus() },
                                onSpeakObstacleStatus = { viewModel.speakObstacleStatus() },
                                onSpeakWaterStatus = { viewModel.speakWaterStatus() },
                                onStopSpeaking = { viewModel.stopSpeaking() }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                settings = settings,
                                locationTestResult = locationTestResult,
                                onSaveContacts = { pName, pPhone, sName, sPhone, sms ->
                                    viewModel.saveEmergencyContacts(pName, pPhone, sName, sPhone, sms)
                                },
                                onSaveEspSettings = { url, interval ->
                                    viewModel.saveEspSettings(url, interval)
                                },
                                onSaveAiSettings = { mode, customUrl ->
                                    viewModel.saveAiSettings(mode, customUrl)
                                },
                                onSaveVoiceAndAccessibility = { lang, speed, autoSpeak, hc, lt ->
                                    viewModel.saveVoiceAndAccessibility(lang, speed, autoSpeak, hc, lt)
                                },
                                onToggleTestMode = { viewModel.setTestMode(it) },
                                onToggleHardwareSimulator = { viewModel.setHardwareSimulator(it) },
                                onTestLocation = { viewModel.testGpsLocation() },
                                onNavigateGuide = { navController.navigate(GuideRoute) }
                            )
                        }

                        composable<GuideRoute> {
                            SetupAndGuideScreen()
                        }
                    }
                }
            }

            SosWorkflowDialog(
                sosState = sosState,
                onCancelSos = { viewModel.cancelSosWorkflow() },
                onDismiss = { viewModel.dismissSosStatus() },
                onOpenSms = { phone, body -> viewModel.openSmsForSos(phone, body) },
                onOpenDialer = { phone -> viewModel.openDialerForSos(phone) }
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SonarCyan
import com.example.ui.theme.TelemetryMonoFontFamily

@Composable
fun SetupAndGuideScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "सेटअप, हार्डवेयर व सहायता गाइड",
            style = MaterialTheme.typography.headlineMedium,
            color = SonarCyan
        )

        GuideCard(
            title = "1. ESP8266 Wi-Fi कनेक्शन निर्देश",
            body = """
                1. अपने NodeMCU ESP8266 प्रोटोटाइप को पावर दें।
                2. फोन की Wi-Fi सेटिंग्स खोलें और नेटवर्क चुनें:
                   • SSID: ANDHA-HELPER
                   • पासवर्ड: 12345678
                3. यदि Android कहे कि "इस Wi-Fi पर इंटरनेट नहीं है", तो "Stay Connected / कनेक्ट रहें" चुनें।
                4. यह ऐप स्वचालित रूप से Wi-Fi सॉकेट को 192.168.4.1 पर बाइंड करता है।
            """.trimIndent()
        )

        GuideCard(
            title = "2. ESP8266 NodeMCU पिनआउट और API",
            body = """
                • HC-SR04 TRIG: D5 (GPIO14) | ECHO: D6 (GPIO12)
                • Buzzer: D7 (GPIO13) | Vibration Driver: D8 (GPIO15)
                • Water Sensor: A0
                • बटन: ASK = D1 (GPIO5), CALL = D2 (GPIO4), SOS = D3 (GPIO0), CHECK = D4 (GPIO2)

                API एंडपॉइंट्स (अपना फर्मवेयर अपरिवर्तित रखें):
                • GET http://192.168.4.1/sensors
                • GET http://192.168.4.1/answer?text=...
                • GET http://192.168.4.1/clearButton
            """.trimIndent(),
            useMono = true
        )

        GuideCard(
            title = "3. AI विज़न बैकएंड सुरक्षा (API Key Safety)",
            body = """
                • APK में कभी भी गुप्त AI API कुंजी न डालें।
                • डिफ़ॉल्ट रूप से ऐप Firebase AI Logic (App Check) का उपयोग करता है।
                • यदि आप अपना खुद का OpenAI/Gemini बैकएंड प्रॉक्सी उपयोग करना चाहते हैं, तो सेटिंग्स में "Custom Backend Proxy" चुनें और अपना HTTPS सर्वर URL दर्ज करें।
                • नोट: ANDHA-HELPER Wi-Fi पर इंटरनेट न होने पर Offline Demo Mode या मोबाइल डेटा आवश्यक हो सकता है।
            """.trimIndent()
        )

        GuideCard(
            title = "4. Android SOS कॉल और SMS नीति",
            body = """
                • Android 10+ और Google Play नीतियाँ बैकग्राउंड में चुपचाप कॉल या SMS भेजने को प्रतिबंधित करती हैं।
                • इसलिए सुरक्षित और अनुपालन-योग्य तरीके से यह ऐप 5-सेकंड के रद्द-करने-योग्य (Cancellable) काउंटडाउन के बाद Google Maps लोकेशन के साथ सिस्टम SMS ऐप (smsto:) और डायलर (tel:) खोलता है।
                • परीक्षण के दौरान हमेशा TEST MODE चालू रखें और कभी भी 112/100/911 पर स्वचालित कॉल न करें।
            """.trimIndent(),
            accentCaution = true
        )

        GuideCard(
            title = "5. परीक्षण चेकलिस्ट (Testing Checklist)",
            body = """
                ✓ TEST MODE व Hardware Simulator से सेंसर और SOS काउंटडाउन जांचें।
                ✓ ANDHA-HELPER Wi-Fi से जुड़कर डैशबोर्ड पर लाइव दूरी और वाटर वैल्यू देखें।
                ✓ ESP के CHECK, ASK, CALL और SOS बटन दबाकर ऐप की प्रतिक्रिया जांचें।
                ✓ कैमरा अनुमति देकर "मेरे सामने क्या है?" का हिंदी वॉइस विवरण सुनें।
                ✓ GPS लोकेशन और Google Maps लिंक की सटीकता जांचें।
            """.trimIndent(),
            accentSuccess = true
        )

        GuideCard(
            title = "6. समस्या निवारण (Troubleshooting)",
            body = """
                • ESP डिस्कनेक्ट दिख रहा है: जांचें कि फोन ANDHA-HELPER Wi-Fi से जुड़ा है और VPN बंद है।
                • AI विज़न त्रुटि: ANDHA-HELPER पर इंटरनेट न होने पर Settings में Demo Mode आज़माएं या Dual-Wi-Fi/Cellular पर AI जांचें।
                • हिंदी आवाज़ नहीं बोल रही: फोन की Settings > Accessibility > Text-to-Speech में Google Speech Services (Hindi India) डेटा डाउनलोड करें।
            """.trimIndent()
        )
    }
}

@Composable
private fun GuideCard(
    title: String,
    body: String,
    useMono: Boolean = false,
    accentCaution: Boolean = false,
    accentSuccess: Boolean = false
) {
    val borderColor = when {
        accentCaution -> CautionAmber
        accentSuccess -> SafeEmerald
        else -> MaterialTheme.colorScheme.outline
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    accentCaution -> CautionAmber
                    accentSuccess -> SafeEmerald
                    else -> SonarCyan
                }
            )
            Text(
                text = body,
                style = if (useMono) {
                    MaterialTheme.typography.bodyMedium.copy(fontFamily = TelemetryMonoFontFamily)
                } else {
                    MaterialTheme.typography.bodyMedium
                }
            )
        }
    }
}

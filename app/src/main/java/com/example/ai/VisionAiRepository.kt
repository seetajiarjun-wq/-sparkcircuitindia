package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.AiProviderMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "VisionAiRepository"

sealed interface VisionUiState {
    data object Idle : VisionUiState
    data object Capturing : VisionUiState
    data object Analyzing : VisionUiState
    data class Success(
        val descriptionHindi: String,
        val promptUsed: String,
        val providerLabel: String,
        val isDemoResponse: Boolean = false
    ) : VisionUiState

    data class Error(val messageHindi: String) : VisionUiState
}

class VisionAiRepository {

    // Lazy initialization per Firebase AI Logic contract
    private val geminiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val customBackendClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        userPrompt: String,
        providerMode: AiProviderMode,
        customBackendUrl: String
    ): Result<String> {
        val scaled = scaleBitmapForUpload(bitmap)
        return when (providerMode) {
            AiProviderMode.FIREBASE_AI -> analyzeWithFirebaseAi(scaled, userPrompt)
            AiProviderMode.CUSTOM_BACKEND -> analyzeWithCustomBackend(scaled, userPrompt, customBackendUrl)
            AiProviderMode.DEMO_MOCK -> Result.success(buildDemoHindiDescription(userPrompt))
        }
    }

    private suspend fun analyzeWithFirebaseAi(bitmap: Bitmap, userPrompt: String): Result<String> {
        val systemPrompt = buildHindiAccessibilityPrompt(userPrompt)
        return try {
            val response = withTimeout(60_000L) {
                geminiModel.generateContent(
                    content {
                        image(bitmap)
                        text(systemPrompt)
                    }
                )
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("AI मॉडल ने खाली उत्तर दिया। कृपया पुनः प्रयास करें।"))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI vision timed out", e)
            Result.failure(Exception("AI विश्लेषण 60 सेकंड में पूरा नहीं हो सका। इंटरनेट कनेक्शन जांचें।", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI vision error", e)
            val detail = e.localizedMessage ?: "अज्ञात त्रुटि"
            Result.failure(
                Exception("AI सेवा से जुड़ने में समस्या (यदि आप ANDHA-HELPER Wi-Fi पर हैं, तो इंटरनेट अनुपलब्ध हो सकता है): $detail", e)
            )
        }
    }

    private suspend fun analyzeWithCustomBackend(
        bitmap: Bitmap,
        userPrompt: String,
        backendUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedUrl = backendUrl.trim()
        if (trimmedUrl.isEmpty() || (!trimmedUrl.startsWith("https://") && !trimmedUrl.startsWith("http://"))) {
            return@withContext Result.failure(
                IllegalArgumentException("कृपया सेटिंग्स में एक मान्य कस्टम बैकएंड URL (https://...) दर्ज करें।")
            )
        }

        try {
            val jpegBytes = compressToJpegBytes(bitmap)
            val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("imageBase64", base64Image)
                put("mimeType", "image/jpeg")
                put("prompt", buildHindiAccessibilityPrompt(userPrompt))
                put("language", "hi")
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(trimmedUrl)
                .post(requestBody)
                .build()

            withTimeout(60_000L) {
                customBackendClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withTimeout Result.failure(
                            Exception("बैकएंड सर्वर त्रुटि (${response.code}): ${bodyStr.take(160)}")
                        )
                    }
                    val parsedText = extractDescriptionFromBackendJson(bodyStr)
                    if (parsedText.isNotBlank()) {
                        Result.success(parsedText)
                    } else {
                        Result.failure(Exception("बैकएंड से खाली विवरण प्राप्त हुआ।"))
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Custom backend timed out", e)
            Result.failure(Exception("कस्टम AI बैकएंड से जवाब आने में समय सीमा समाप्त हुई।", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Custom backend error", e)
            Result.failure(Exception("कस्टम बैकएंड त्रुटि: ${e.localizedMessage ?: "नेटवर्क विफल"}", e))
        }
    }

    private fun extractDescriptionFromBackendJson(raw: String): String {
        return try {
            val json = JSONObject(raw)
            when {
                json.has("description") -> json.optString("description")
                json.has("answer") -> json.optString("answer")
                json.has("text") -> json.optString("text")
                else -> raw.trim()
            }
        } catch (_: Exception) {
            raw.trim()
        }
    }

    private fun buildHindiAccessibilityPrompt(userQuery: String): String {
        return """
            आप एक दृष्टिबाधित सहायता प्रोटोटाइप (ANDHA HELPER) के लिए दृश्य विवरण सहायक हैं।
            उपयोगकर्ता का प्रश्न: "$userQuery"
            नियम:
            1. उत्तर केवल सरल, स्पष्ट और संक्षिप्त हिंदी में दें (अधिकतम 2-3 छोटे वाक्य)।
            2. जो कुछ तस्वीर में स्पष्ट दिख रहा है (जैसे मेज़, कुर्सी, दरवाज़ा, सीढ़ियाँ, व्यक्ति, दीवार या प्रकाश), केवल उसी का वर्णन करें।
            3. यदि कोई हिस्सा धुंधला या अंधेरा है तो विनम्रता से बताएं कि स्पष्ट दिखाई नहीं दे रहा।
            4. कभी यह दावा न करें कि रास्ता पूरी तरह सुरक्षित है या सभी खतरे पहचान लिए गए हैं।
        """.trimIndent()
    }

    private fun buildDemoHindiDescription(userPrompt: String): String {
        return "[डेमो / टेस्ट मोड] प्रश्न \"$userPrompt\" के लिए नमूना उत्तर: आपके सामने लगभग डेढ़ मीटर की दूरी पर एक मेज़ और दाईं ओर खुलने वाला रास्ता प्रतीत होता है। कृपया सावधानी से आगे बढ़ें।"
    }

    fun scaleBitmapForUpload(source: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source

        val scale = maxDimension.toFloat() / maxOf(width, height).toFloat()
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun compressToJpegBytes(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}

package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.example.sensors.EspSensorResponse
import com.example.sensors.WifiStatusInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

interface Esp8266Api {
    @GET("sensors")
    suspend fun getSensors(): EspSensorResponse

    @GET("answer")
    suspend fun sendAnswer(@Query("text") text: String): Response<ResponseBody>

    @GET("clearButton")
    suspend fun clearButton(): Response<ResponseBody>
}

class EspNetworkClient(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    private var cachedBaseUrl: String = ""

    @Volatile
    private var cachedWifiNetworkId: Long = -1L

    @Volatile
    private var cachedApi: Esp8266Api? = null

    fun getApi(baseUrl: String): Esp8266Api {
        val wifiNetwork = findActiveWifiNetwork()
        val networkHandle = wifiNetwork?.networkHandle ?: -1L
        val existing = cachedApi
        if (existing != null && cachedBaseUrl == baseUrl && cachedWifiNetworkId == networkHandle) {
            return existing
        }

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        // Bind local ESP8266 HTTP traffic directly to the active Wi-Fi network so Android
        // routes 192.168.4.1 to the ESP8266 SoftAP even when that AP has no internet access.
        val isLoopback = baseUrl.contains("127.0.0.1") || baseUrl.contains("localhost")
        if (wifiNetwork != null && !isLoopback) {
            val socketFactory: SocketFactory = wifiNetwork.socketFactory
            clientBuilder.socketFactory(socketFactory)
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(Esp8266Api::class.java)
        cachedBaseUrl = baseUrl
        cachedWifiNetworkId = networkHandle
        cachedApi = api
        return api
    }

    fun getWifiStatus(): WifiStatusInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiNetwork = findActiveWifiNetwork(cm)
        val isWifiConnected = wifiNetwork != null

        val ssid = readCurrentSsid(cm, wifiNetwork)
        val isAndhaAp = ssid.contains("ANDHA-HELPER", ignoreCase = true)

        return WifiStatusInfo(
            isWifiConnected = isWifiConnected,
            ssid = ssid,
            isBoundToWifi = wifiNetwork != null,
            isAndhaHelperAp = isAndhaAp
        )
    }

    @Suppress("DEPRECATION")
    private fun findActiveWifiNetwork(
        cm: ConnectivityManager? = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    ): Network? {
        cm ?: return null
        return cm.allNetworks.firstOrNull { network ->
            val caps = cm.getNetworkCapabilities(network)
            caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }

    @Suppress("DEPRECATION")
    private fun readCurrentSsid(cm: ConnectivityManager?, wifiNetwork: Network?): String {
        if (wifiNetwork == null) return "Wi-Fi जुड़ा नहीं है"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val caps = cm?.getNetworkCapabilities(wifiNetwork)
            val transportInfo = caps?.transportInfo as? android.net.wifi.WifiInfo
            val rawSsid = transportInfo?.ssid?.replace("\"", "")
            if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                return rawSsid
            }
        }

        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo
            val raw = info?.ssid?.replace("\"", "")
            if (!raw.isNullOrBlank() && raw != "<unknown ssid>") raw else "Wi-Fi जुड़ा है"
        } catch (_: SecurityException) {
            "Wi-Fi जुड़ा है"
        }
    }
}

package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "LocationRepository"

sealed interface LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val mapsUrl: String,
        val timestampMillis: Long
    ) : LocationResult

    data object PermissionDenied : LocationResult
    data object GpsDisabled : LocationResult
    data class Error(val messageHindi: String) : LocationResult
}

class LocationRepository(private val context: Context) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(timeoutMs: Long = 10_000L): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.PermissionDenied
        }
        if (!isLocationEnabled()) {
            return LocationResult.GpsDisabled
        }

        return try {
            val freshLocation = withTimeoutOrNull(timeoutMs) {
                fetchFreshFusedLocation()
            }
            val location = freshLocation ?: fetchLastKnownLocation()
            if (location != null) {
                val mapsUrl = buildGoogleMapsUrl(location.latitude, location.longitude)
                LocationResult.Success(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    mapsUrl = mapsUrl,
                    timestampMillis = System.currentTimeMillis()
                )
            } else {
                LocationResult.Error("वर्तमान GPS लोकेशन समय पर नहीं मिल सकी।")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Location retrieval failed", e)
            LocationResult.Error("लोकेशन त्रुटि: ${e.localizedMessage ?: "अज्ञात समस्या"}")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFreshFusedLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (cont.isActive) cont.resume(location)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (cont.isActive) cont.resume(location)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    companion object {
        fun buildGoogleMapsUrl(lat: Double, lng: Double): String {
            return "https://maps.google.com/?q=$lat,$lng"
        }
    }
}

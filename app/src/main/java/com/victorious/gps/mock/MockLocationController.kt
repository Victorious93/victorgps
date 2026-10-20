package com.victorious.gps.mock

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.victorious.gps.domain.Coordinates
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes mock fixes through LocationManager test providers (GPS + network) and, when Google Play
 * services is present, FusedLocationProviderClient mock mode. All of this only works once the user
 * has selected VictorGPS under Developer options > Select mock location app; otherwise Android
 * throws SecurityException, which we surface as [MockStatus.NotSelected].
 */
@Singleton
class MockLocationController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lm: LocationManager,
) {
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private val added = mutableSetOf<String>()
    private var fused: FusedLocationProviderClient? = null

    /** True if the system mock-location app op is granted to us. */
    fun isSelectedMockApp(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isPlayServicesAvailable(): Boolean = try {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    } catch (t: Throwable) {
        false
    }

    @SuppressLint("MissingPermission")
    fun start(): MockStatus {
        if (!isSelectedMockApp()) return MockStatus.NotSelected
        try {
            for (p in providers) {
                runCatching { lm.removeTestProvider(p) }
                addProvider(p)
                lm.setTestProviderEnabled(p, true)
                added += p
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "test provider rejected", e)
            stop()
            return MockStatus.NotSelected
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "test provider failed", e)
            stop()
            return MockStatus.Error(e.message ?: "Could not add test provider")
        }
        if (isPlayServicesAvailable()) {
            fused = LocationServices.getFusedLocationProviderClient(context).also { client ->
                client.setMockMode(true).addOnFailureListener { Log.w(TAG, "fused setMockMode failed", it) }
            }
        }
        return MockStatus.Running(fusedActive = fused != null)
    }

    @SuppressLint("MissingPermission")
    fun push(c: Coordinates, accuracy: Float, altitude: Double) {
        for (p in added) {
            try {
                lm.setTestProviderLocation(p, build(p, c, accuracy, altitude))
            } catch (e: Exception) {
                Log.w(TAG, "push to $p failed", e)
            }
        }
        fused?.setMockLocation(build("fused", c, accuracy, altitude))
            ?.addOnFailureListener { Log.w(TAG, "fused setMockLocation failed", it) }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        for (p in added.toList()) {
            runCatching { lm.setTestProviderEnabled(p, false) }
            runCatching { lm.removeTestProvider(p) }
        }
        added.clear()
        fused?.let { runCatching { it.setMockMode(false) } }
        fused = null
    }

    private fun addProvider(name: String) {
        val gps = name == LocationManager.GPS_PROVIDER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lm.addTestProvider(
                name, !gps, gps, false, false, true, true, true,
                ProviderProperties.POWER_USAGE_LOW,
                if (gps) ProviderProperties.ACCURACY_FINE else ProviderProperties.ACCURACY_COARSE,
            )
        } else {
            addTestProviderLegacy(name, gps)
        }
    }

    // Pre-S overload: Criteria constants share the same int values as ProviderProperties.
    @Suppress("DEPRECATION")
    @SuppressLint("WrongConstant")
    private fun addTestProviderLegacy(name: String, gps: Boolean) {
        lm.addTestProvider(
            name, !gps, gps, false, false, true, true, true,
            android.location.Criteria.POWER_LOW,
            if (gps) android.location.Criteria.ACCURACY_FINE else android.location.Criteria.ACCURACY_COARSE,
        )
    }

    private fun build(provider: String, c: Coordinates, accuracy: Float, altitude: Double) =
        Location(provider).apply {
            latitude = c.latitude
            longitude = c.longitude
            this.accuracy = accuracy
            this.altitude = altitude
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            speed = 0f
            bearing = 0f
            verticalAccuracyMeters = accuracy
            speedAccuracyMetersPerSecond = 0.1f
            bearingAccuracyDegrees = 0.1f
        }

    private companion object { const val TAG = "VictorGPS" }
}

sealed interface MockStatus {
    data object Idle : MockStatus
    data object NotSelected : MockStatus
    data class Running(val fusedActive: Boolean) : MockStatus
    data class Error(val message: String) : MockStatus
}

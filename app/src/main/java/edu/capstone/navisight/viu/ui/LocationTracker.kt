package edu.capstone.navisight.viu.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

sealed interface LocationEvent {
    data class Success(val lat: Double, val lon: Double) : LocationEvent
    object GpsDisabled : LocationEvent
}

class LocationTracker(
    private val context: Context,
    private val onEvent: (LocationEvent) -> Unit
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    private lateinit var hostActivity: Activity
    private var requestCode: Int = 0

    // Throttle to prevent dialog spam but still enforce GPS
    private val throttleWindowMs = 6_000L
    private var lastDialogTime = 0L

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000
    ).setMinUpdateIntervalMillis(1000)
        .setWaitForAccurateLocation(true)
        .build()

    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {
            for (location: Location in result.locations) {
                onEvent(LocationEvent.Success(location.latitude, location.longitude))
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) {
                onEvent(LocationEvent.GpsDisabled)

                val now = System.currentTimeMillis()
                if (now - lastDialogTime > throttleWindowMs) {
                    lastDialogTime = now
                    checkSettingsAndStart(hostActivity, requestCode)
                }
            }
        }
    }

    fun checkSettingsAndStart(activity: Activity, resolveRequestCode: Int) {
        hostActivity = activity
        requestCode = resolveRequestCode

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> =
            client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startTracking()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(activity, resolveRequestCode)
                } catch (_: IntentSender.SendIntentException) {}
            }
        }
    }

    // Explicit recheck when app regains focus
    fun forceGpsRecheck(activity: Activity, resolveRequestCode: Int) {
        checkSettingsAndStart(activity, resolveRequestCode)
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d("LocationTracker", "Tracker Started")
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("LocationTracker", "Tracker Stopped")
    }
}

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
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Setup high accuracy request
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000
    ).apply {
        setMinUpdateIntervalMillis(1000)
        setWaitForAccurateLocation(false) //nagbabasa kaya kayo ng comment '_' wally kalbsss
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location: Location in result.locations) {
                // Send coordinates to UI
                onEvent(LocationEvent.Success(location.latitude, location.longitude))
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            super.onLocationAvailability(availability)
            // Notify if GPS is disabled
            if (!availability.isLocationAvailable) {
                Log.d("LocationTracker", "GPS unavailable")
                onEvent(LocationEvent.GpsDisabled)
            }
        }
    }

    // Check if GPS is on, if not request to turn it on
    fun checkSettingsAndStart(activity: Activity, resolveRequestCode: Int) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startTracking()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(activity, resolveRequestCode)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationTracker", "Tracker Started")
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error starting updates: ${e.message}")
        }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("LocationTracker", "Tracker Stopped")
    }
}
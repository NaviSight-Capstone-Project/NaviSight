package edu.capstone.navisight.viu.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

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

    private val throttleWindowMs = 3_000L
    private var lastDialogTimestamp = 0L

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000
    ).setMinUpdateIntervalMillis(1000)
        .setWaitForAccurateLocation(false)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                onEvent(LocationEvent.Success(location.latitude, location.longitude))
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) {
                onEvent(LocationEvent.GpsDisabled)
                if (::hostActivity.isInitialized) {
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

        val settingsClient = LocationServices.getSettingsClient(context)

        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                startTracking()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    val now = System.currentTimeMillis()
                    if (now - lastDialogTimestamp > throttleWindowMs) {
                        lastDialogTimestamp = now
                        try {
                            exception.startResolutionForResult(activity, resolveRequestCode)
                        } catch (e: IntentSender.SendIntentException) {

                        }
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
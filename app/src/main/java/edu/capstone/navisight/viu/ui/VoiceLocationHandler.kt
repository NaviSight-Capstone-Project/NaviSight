package edu.capstone.navisight.viu.ui

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import edu.capstone.navisight.viu.utils.TextToSpeechHelper
import edu.capstone.navisight.viu.utils.VoiceRecognitionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class VoiceLocationHandler(
    private val context: Context,
    private val ttsHelper: TextToSpeechHelper,
    private val scope: CoroutineScope
) {

    private var voiceHelper: VoiceRecognitionHelper? = null

    @Volatile var currentLat: Double? = null
    @Volatile var currentLon: Double? = null

    fun initialize() {
        voiceHelper = VoiceRecognitionHelper(
            context = context,
            onResult = { spokenText -> processCommand(spokenText) },
            onError = { errorMsg ->
                Log.e("VoiceHandler", "Error: $errorMsg")
            }
        )
    }

    fun startListeningForCommand() {
        ttsHelper.stop()
        voiceHelper?.startListening()
    }

    private fun processCommand(text: String) {
        val cleanText = text.lowercase().trim()
        Log.d("VoiceHandler", "Command received: $cleanText")

        if (cleanText.contains("where") && (cleanText.contains("am i") || cleanText.contains("location"))) {
            handleWhereAmI()
        } else {
        }
    }

    private fun handleWhereAmI() {
        val lat = currentLat
        val lon = currentLon

        if (lat != null && lon != null) {
            ttsHelper.speak("Getting your location...")

            scope.launch(Dispatchers.IO) {
                val addressText = getAddressFromLocation(lat, lon)

                withContext(Dispatchers.Main) {
                    ttsHelper.speak("You are currently at $addressText")
                }
            }
        } else {
            ttsHelper.speak("I am waiting for a GPS signal. Please try again in a moment.")
        }
    }

    @Suppress("DEPRECATION")
    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                val parts = listOfNotNull(
                    address.thoroughfare, // Street name
                    address.featureName,  // Landmark/Building name
                    address.locality      // City
                ).distinct()

                if (parts.isNotEmpty()) {
                    parts.joinToString(", ")
                } else {
                    address.getAddressLine(0) ?: "an unknown street"
                }
            } else {
                "an unknown location"
            }
        } catch (e: Exception) {
            Log.e("VoiceHandler", "Geocoding failed", e)
            "GPS coordinates only."
        }
    }

    fun cleanup() {
        voiceHelper?.cleanup()
        ttsHelper.stop()
    }
}
package edu.capstone.navisight.viu.ui

import android.content.Context
import android.location.Geocoder
import android.util.Log
import edu.capstone.navisight.viu.utils.TextToSpeechHelper
import edu.capstone.navisight.viu.utils.VoiceRecognitionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class VoiceLocationHandler(
    private val context: Context,
    private val ttsHelper: TextToSpeechHelper,
    private val scope: CoroutineScope // Need scope for background work
) {

    private var voiceHelper: VoiceRecognitionHelper? = null
    var currentLat: Double? = null
    var currentLon: Double? = null

    fun initialize() {
        voiceHelper = VoiceRecognitionHelper(
            context = context,
            onResult = { spokenText -> processCommand(spokenText) },
            onError = { errorMsg -> Log.e("VoiceHandler", errorMsg) }
        )
    }

    fun startListeningForCommand() {
        // You might want to vibrate here to signal listening started
        voiceHelper?.startListening()
    }

    private fun processCommand(text: String) {
        val cleanText = text.lowercase()
        if (cleanText.contains("where") && (cleanText.contains("am i") || cleanText.contains("are we"))) {
            handleWhereAmI()
        } else {
            // Optional: Provide feedback if command not recognized
            // ttsHelper.speak("I didn't understand.")
        }
    }

    private fun handleWhereAmI() {
        if (currentLat != null && currentLon != null) {
            // Move Geocoding to IO thread to prevent freezing
            scope.launch(Dispatchers.IO) {
                val address = getAddressFromLocation(currentLat!!, currentLon!!)
                // Switch back to Main thread to speak (though TTS is usually async, it's safer)
                withContext(Dispatchers.Main) {
                    ttsHelper.speak("You are currently at $address")
                }
            }
        } else {
            ttsHelper.speak("I am still finding your location.")
        }
    }

    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                "${address.thoroughfare ?: address.featureName ?: ""}, ${address.locality ?: ""}"
            } else {
                "an unknown location"
            }
        } catch (e: Exception) {
            "coordinate $lat, $lon"
        }
    }

    fun cleanup() {
        voiceHelper?.cleanup()
    }
}
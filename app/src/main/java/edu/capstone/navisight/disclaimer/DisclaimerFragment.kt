package edu.capstone.navisight.disclaimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.R
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.disclaimer.audioVisualizer.AudioVisualizerViewModel
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.viu.ViuHomeFragment
import java.util.*

class DisclaimerFragment : Fragment() {
    private val defaultSharePreferencesName = "NaviData" // Set default preferences name
    private val audioViewModel = AudioVisualizerViewModel()
    private var speechRecognizer: SpeechRecognizer? = null
    private var isConfirming = false
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val disclaimerText = """
        I'll read disclaimer text shown on the screen.
        NaviSight isn't perfect and is not a substitute for a complete visual aid.
        It acts as a supporter. By using this app, you agree not to use it on its own.
        The NaviSight team is not liable for any damages or accidents.
        You also agree to the Terms and Conditions and Privacy Policy.
    """.trimIndent()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val volumeLevel by audioViewModel.volumeLevel.collectAsState()
                DisclaimerScreen(
                    volumeLevel = volumeLevel,
                    onAgree = { handleAgreement() } // checkbox or manual click
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupSpeechRecognizer()
        startDisclaimer()
        startSimulatedMic()
    }

    override fun onPause() {
        super.onPause()
        stopAllSpeech()
    }


    private fun setupSpeechRecognizer() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(singleRecognizerListener)
        }
    }

    private fun startDisclaimer() {
        isConfirming = false
        TextToSpeechHelper.queueSpeakLatest(requireContext(), disclaimerText)
        startListening()
    }

    private fun stopAllSpeech() {
        try {
            TextToSpeechHelper.shutdown()
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("Disclaimer", "Error stopping speech: ${e.message}")
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    private val singleRecognizerListener = object : RecognitionListener {
        override fun onRmsChanged(rmsdB: Float) {
            audioViewModel.updateFromMic(rmsdB)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.joinToString(" ")?.lowercase(Locale.getDefault()) ?: return
            handleSpeechInput(text)
        }

        override fun onError(error: Int) {
            Log.e("STT", "Error: $error")
            restartListening()
        }

        // unused
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleSpeechInput(text: String) {
        if (!isConfirming && text.contains("agree")) {
            confirmAgree()
        } else if (!isConfirming && text.contains("repeat")) {
            TextToSpeechHelper.speak(requireContext(), "Repeating the disclaimer")
            startDisclaimer()
        } else if (isConfirming) {
            handleConfirmation(text)
        } else {
            restartListening()
        }
    }

    private fun confirmAgree() {
        isConfirming = true
        TextToSpeechHelper.speak(requireContext(), "Did you say agree? Please say yes or no.")
        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 2000)
    }

    private fun handleConfirmation(text: String) {
        when {
            text.contains("yes") -> handleAgreement()
            text.contains("no") -> {
                TextToSpeechHelper.speak(requireContext(), "Continuing the disclaimer.")
                isConfirming = false
                startDisclaimer()
            }
            else -> {
                TextToSpeechHelper.speak(requireContext(), "Please say yes or no.")
                startListening()
            }
        }
    }

    private fun restartListening() {
        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 1500)
    }

    private fun handleAgreement() {
        saveDisclaimerAgreement()
        TextToSpeechHelper.speak(requireContext(), "Thank you. Proceeding now.")
        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 2000)
    }

    private fun saveDisclaimerAgreement() {
        val prefs = requireContext().getSharedPreferences(defaultSharePreferencesName, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("IsDisclaimerAgreed", true).apply()
    }

    private fun navigateNext() {
        val currentUser = auth.currentUser
        val nextFragment = if (currentUser != null) {
            ViuHomeFragment()
        } else {
            GuestFragment()
        }
        parentFragmentManager.commit {
            replace(R.id.fragment_container, nextFragment)
        }
    }

    private fun startSimulatedMic() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val rms = (0..10).random().toFloat()
                audioViewModel.updateFromMic(rms)
                handler.postDelayed(this, 100)
            }
        }
        handler.post(runnable)
    }
}

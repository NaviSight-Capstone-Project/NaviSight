package edu.capstone.navisight.viu.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    @Volatile
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("NaviSightTTS", "Language not supported or missing data")
            } else {
                isInitialized = true
                tts?.setSpeechRate(1.0f)
            }
        } else {
            Log.e("NaviSightTTS", "Initialization failed")
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (isInitialized) {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, null)
        } else {
            Log.e("NaviSightTTS", "TTS not ready yet. Attempted to speak: $text")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun setRate(rate: Float) {
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
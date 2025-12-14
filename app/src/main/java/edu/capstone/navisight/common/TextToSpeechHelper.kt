package edu.capstone.navisight.common

/*

TTSHelper.kt

Formerly named as TTSHelper.kt
This is in commons as both VIU and Guest mode utilizes the feature.
Please try to use this instead of making a new one unless the said helper is a dedicated one.

 */

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.UUID

object TextToSpeechHelper : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private val speechQueue: Queue<String> = LinkedList()
    private var isSpeaking = false
    private val handler = Handler(Looper.getMainLooper())
    var isSilenced: Boolean = false
        private set

    fun initialize(context: Context) {
        if (tts == null) {
            // Use applicationContext to prevent memory leaks.
            tts = TextToSpeech(context.applicationContext, this)
        }
        // If tts is already being initialized (not null), do nothing.
    }

    fun toggleSilence(): Boolean {
        isSilenced = !isSilenced

        // If silencing, immediately stop any current speech
        if (isSilenced) {
            stop()
        }
        return isSilenced
    }

    fun setRate(rate: Float) {
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun stop() {
        tts?.stop()
        speechQueue.clear()
        isSpeaking = false
    }

    fun speak(context: Context, text: String) {
        if (isSilenced) return
        if (isInitialized) {
            // Speak immediately if initialized
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        } else {
            // If not initialized, store as pending text and ensure initialization is running (fallback)
            pendingText = text
            if (tts == null) {
                initialize(context)
            }
        }
    }

    fun queueSpeakLatest(context: Context?, text: String, customDelayMs: Long = 0L) {
        if (tts == null) {
            tts = TextToSpeech(context?.applicationContext, this)
        }
        speechQueue.clear()
        speechQueue.add(text)

        if (isInitialized && !isSpeaking) {
            // Schedule the next speech with custom delay
            handler.postDelayed({
                speakNextInQueue()
            }, customDelayMs)
        }
    }

    fun getSpeechQueue(): List<String> {
        return speechQueue.toList()  // Returning a copy of the queue to avoid external modification
    }

    fun clearQueue() {
        speechQueue.clear()  // Clear all items in the queue
    }

    // TODO: Not used. May incorporate in future distance detection algorithms.
    fun queueSpeakWithDelay(context: Context, text: String, customDelayMs: Long) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
            speechQueue.add(text)
        } else {
            speechQueue.add(text)
            if (isInitialized && !isSpeaking) {
                // Schedule the next speech with custom delay
                handler.postDelayed({
                    speakNextInQueue()
                }, customDelayMs)
            }
        }
    }

    // TODO: Not used. May incorporate in future distance detection algorithms.
    private fun removeConsecutiveDuplicates() {
        if (speechQueue.isEmpty()) return

        val iterator = speechQueue.iterator()
        var last: String? = null
        val tempQueue: Queue<String> = LinkedList()

        while (iterator.hasNext()) {
            val current = iterator.next()
            if (last == null || current != last) {
                tempQueue.add(current)
                last = current
            }
        }

        // Clear and refill the original queue with the filtered items
        speechQueue.clear()
        speechQueue.addAll(tempQueue)
    }

    fun queueSpeak(context: Context?, text: String) {
        if (isSilenced) return
        if (tts == null) {
            tts = TextToSpeech(context?.applicationContext, this)
            speechQueue.add(text)
        } else {
            speechQueue.add(text)
            if (isInitialized && !isSpeaking) {
                speakNextInQueue()
            }
        }
    }

    private fun removeAllDuplicates() {
        val seenTexts = mutableSetOf<String>()
        val tempQueue: Queue<String> = LinkedList()

        for (text in speechQueue) {
            if (!seenTexts.contains(text)) {
                seenTexts.add(text)
                tempQueue.add(text)
            }
        }

        // Clear and refill the original queue with unique texts
        speechQueue.clear()
        speechQueue.addAll(tempQueue)
    }

    private fun speakNextInQueue() {
        removeAllDuplicates()
        if (speechQueue.isNotEmpty()) {
            val nextText = speechQueue.poll()
            isSpeaking = true
            tts?.speak(nextText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSHelper", "This language is not supported.")
            } else {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        speakNextInQueue()
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        Log.e("TTSHelper", "Error during TTS.")
                        speakNextInQueue()
                    }
                })

                // Speak pending text (from speak() if it was called before init)
                pendingText?.let {
                    tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
                    pendingText = null
                }

                // Engine primer
                tts?.speak("", TextToSpeech.QUEUE_FLUSH, null, "")


                // Start queue if anything waiting
                if (speechQueue.isNotEmpty() && !isSpeaking) {
                    speakNextInQueue()
                }
            }
        } else {
            Log.e("TTSHelper", "Initialization failed.")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        pendingText = null
        speechQueue.clear()
        isSpeaking = false
    }
}
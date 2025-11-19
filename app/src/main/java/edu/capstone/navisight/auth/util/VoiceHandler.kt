package edu.capstone.navisight.auth.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class VoiceHandler(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    // Observable state to let UI know when to animate the mic
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NaviSpeak")
        }
    }

    fun startListening(
        inputType: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        _isListening.value = true
        tts?.stop()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }

            override fun onError(error: Int) {
                _isListening.value = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error."
                    else -> "Try again."
                }
                onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val rawText = matches[0]
                    val processedText = parseSmartInput(rawText, isPassword = (inputType == "password"))
                    onResult(processedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        _isListening.value = false
        speechRecognizer?.stopListening()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    private fun parseSmartInput(input: String, isPassword: Boolean): String {
        // Basic cleanup before processing
        val rawWords = input.trim().split("\\s+".toRegex())
        val sb = StringBuilder()

        var nextCapital = false
        var nextAllCaps = false
        var nextLiteral = false // Ignores number/symbol mapping for the next word

        for (word in rawWords) {
            val lower = word.lowercase()

            // Check for "Literal/Text" Command
            // If user says "Text" or "Word", treat the NEXT word as plain text
            if (lower == "text" || lower == "word" || lower == "literal") {
                nextLiteral = true
                continue
            }

            // Check Capitalization Commands
            if (lower == "capital" || lower == "uppercase") {
                nextCapital = true
                continue
            }
            if ((lower == "all" && rawWords.contains("caps")) || lower == "allcaps") {
                nextAllCaps = true
                continue
            }
            if (lower == "caps") continue

            // Handle Space Command
            if (lower == "space") {
                // If "Literal Space" is said, print the word "space"
                if (nextLiteral) {
                    var segment = "space"
                    if (nextCapital) segment = "Space"
                    if (nextAllCaps) segment = "SPACE"
                    sb.append(segment)
                    nextLiteral = false
                    nextCapital = false
                    nextAllCaps = false
                } else if (isPassword) {
                    sb.append(" ") // Actual whitespace
                }
                continue
            }

            // Map Symbols & Numbers (ONLY if nextLiteral is FALSE)
            var finalSegment = word // Default to the original word

            if (!nextLiteral) {
                // Try to map "six" -> "6", "at" -> "@"
                finalSegment = mapWordToSymbol(lower)
            } else {
                // "Text six" -> keeps "six"
                // We consume the flag now
                nextLiteral = false
            }

            // Apply Capitalization
            if (nextAllCaps) {
                finalSegment = finalSegment.uppercase()
                nextAllCaps = false
            } else if (nextCapital) {
                finalSegment = finalSegment.replaceFirstChar { it.uppercase() }
                nextCapital = false
            } else {
                // Standardize to lowercase unless commanded otherwise
                finalSegment = finalSegment.lowercase()
            }

            sb.append(finalSegment)
        }

        var result = sb.toString()

        // Final Email Cleanup
        if (!isPassword) {
            result = result.replace(" ", "").lowercase()
        }

        return result
    }

    private fun mapWordToSymbol(word: String): String {
        return when (word) {
            "at", "atsign" -> "@"
            "dot", "period" -> "."
            "underscore" -> "_"
            "dash", "hyphen", "minus" -> "-"
            "hashtag", "pound" -> "#"
            "dollar", "dollarsign" -> "$"
            "percent" -> "%"
            "star", "asterisk" -> "*"
            "exclamation", "exclamationmark" -> "!"
            "question", "questionmark" -> "?"
            "slash", "forwardslash" -> "/"
            "backslash" -> "\\"
            "zero" -> "0"
            "one" -> "1"
            "two" -> "2"
            "three" -> "3"
            "four" -> "4"
            "five" -> "5"
            "six" -> "6"
            "seven" -> "7"
            "eight" -> "8"
            "nine" -> "9"
            else -> word
        }
    }
}
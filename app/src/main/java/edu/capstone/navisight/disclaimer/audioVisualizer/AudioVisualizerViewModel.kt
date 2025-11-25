package edu.capstone.navisight.disclaimer.audioVisualizer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioVisualizerViewModel : ViewModel() {
    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel = _volumeLevel.asStateFlow()

    fun updateFromMic(rmsDb: Float) {
        _volumeLevel.value = rmsDb.coerceIn(0f, 10f) / 10f
    }

    fun simulateTtsStart() {
        _volumeLevel.value = 0.8f
    }

    fun simulateTtsStop() {
        _volumeLevel.value = 0f
    }
}

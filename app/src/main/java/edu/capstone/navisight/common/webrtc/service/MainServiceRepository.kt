package edu.capstone.navisight.common.webrtc.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import edu.capstone.navisight.common.TextToSpeechHelper

class MainServiceRepository(private val context: Context) {
    private fun startServiceIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
            Log.d("ServiceCheck", "Service ran on VERSION CODE >= O")
        } else {
            context.startService(intent)
            Log.d("ServiceCheck", "Service ran on plain startService")
        }
    }

    fun showToastOnServiceRepoThreadAndTTS(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        TextToSpeechHelper.queueSpeak(context, message)
    }

    fun setupViews(videoCall: Boolean, caller: Boolean, target: String) {
        val intent = Intent(context, MainService::class.java).apply {
            action = MainServiceActions.SETUP_VIEWS.name
            putExtra("isVideoCall", videoCall)
            putExtra("target", target)
            putExtra("isCaller", caller)
        }
        startServiceIntent(intent)
    }

    fun sendEndOrAbortCall() {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.IDENTIFY_END_OR_ABORT_CALL.name
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO.name
        intent.putExtra("shouldBeMuted", shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_VIDEO.name
        intent.putExtra("shouldBeMuted", shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO_DEVICE.name
        intent.putExtra("type", type)
        startServiceIntent(intent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_SCREEN_SHARE.name
        intent.putExtra("isStarting", isStarting)
        startServiceIntent(intent)
    }

    fun stopService() {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.STOP_SERVICE.name
        startServiceIntent(intent)
    }

    companion object {
        @Volatile
        private var INSTANCE: MainServiceRepository? = null

        /**
         * Returns the single instance of MainServiceRepository, creating it if necessary.
         * It uses the application context to safely hold a reference.
         */
        fun getInstance(context: Context): MainServiceRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MainServiceRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
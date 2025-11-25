package edu.capstone.navisight.common

/*

VibrationHelper.kt

Set all vibration properties here.

 */


import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class VibrationHelper (private var context: Context){
    private val tag = "VibrationHelper"
    private var vibrationValue = 500L // Set to milliseconds, long

    fun setVibration(milliseconds: Long) {
        vibrationValue = milliseconds
    }

    fun vibrate(){
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(vibrationValue,
                VibrationEffect.DEFAULT_AMPLITUDE))
            Log.i(tag, "I just vibrated - New API (>=26) used.")

        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationValue)
            Log.i(tag, "I just vibrated - Old API (<26) used.")
        }
    }
}
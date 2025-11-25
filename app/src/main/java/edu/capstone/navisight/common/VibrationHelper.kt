package edu.capstone.navisight.common

/*

VibrationHelper.kt

Set all vibration properties here.
Defaults to 500 milliseconds (0.5)

 */

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class VibrationHelper (private var context: Context?){
    private val tag = "VibrationHelper"
    private val defaultVibrationMilliseconds = 500L // Modify default here

    fun vibrate(milliseconds:Long=defaultVibrationMilliseconds){
        val vibrator = context?.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(milliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE))
            Log.i(tag, "I just vibrated - New API (>=26) used.")

        } else {
            @Suppress("DEPRECATION")
            (vibrator?.vibrate(milliseconds))
            Log.i(tag, "I just vibrated - Old API (<26) used.")
        }
    }
}
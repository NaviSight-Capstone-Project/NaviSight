package edu.capstone.navisight.viu.ui.profile

import android.content.Context
import androidx.core.content.edit

object ViuSettingsManager {
    private const val PREFS_NAME = "ViuLocalSettings"
    const val KEY_APP_NOTIFICATION = "viuAppNotificationEnabled"
    const val KEY_SOUND_ALERT = "viuSoundAlertEnabled"
    const val KEY_VIBRATION = "viuVibrationEnabled"

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, defaultValue)
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(key, value) }
    }

    fun resetSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }
}
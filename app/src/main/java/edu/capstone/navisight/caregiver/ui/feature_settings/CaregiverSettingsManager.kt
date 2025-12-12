package edu.capstone.navisight.caregiver.ui.feature_settings

import android.content.Context
import androidx.core.content.edit

object CaregiverSettingsManager {
    private const val PREFS_NAME = "CaregiverLocalSettings"
    const val KEY_APP_NOTIFICATION = "caregiverAppNotificationEnabled"
    const val KEY_SOUND_ALERT = "caregiverSoundAlertEnabled"
    const val KEY_VIBRATION = "caregiverVibrationEnabled"

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
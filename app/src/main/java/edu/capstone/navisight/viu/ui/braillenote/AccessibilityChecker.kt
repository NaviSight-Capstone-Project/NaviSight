package edu.capstone.navisight.viu.ui.braillenote

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager


fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as?
            AccessibilityManager
    return accessibilityManager?.let {
        it.isEnabled && !it.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).isNullOrEmpty()
    } ?: false
}

/**
 * Creates an Intent to open the device's Accessibility Settings.
 */
fun createAccessibilitySettingsIntent(): Intent {
    return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}


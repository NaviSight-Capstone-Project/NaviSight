package edu.capstone.navisight.common

import kotlin.time.Duration.Companion.milliseconds


data class HapticEvent(
    val durationMs: Long,
    val isVibration: Boolean // true for vibration, false for pause
)

object Constants {

    // Shared Preferences
    const val GLOBAL_LOCAL_SETTINGS = "NaviData"
    const val VIU_LOCAL_SETTINGS = "ViuLocalSettings"
    const val CAREGIVER_LOCAL_SETTINGS = "CaregiverLocalSettings"
    const val GUEST_LOCAL_SETTINGS = "GuestLocalSettings"
    const val USER_TYPE_KEY = "CURRENT_USER_TYPE"
    const val SP_IS_USER_WARNED_OF_LOWBAT = "IsUserWarnedOnLowBatteryLevel"
    const val SP_IS_EMERGENCY_MODE_ACTIVE = "IsEmergencyModeActive"

    // Broadcast Receiver
    const val BR_ACTION_MISSED_CALL = "TARGET_MISSED_YOUR_CALL"
    const val BR_ACTION_DENIED_CALL = "TARGET_DECLINED_YOUR_CALL"
    const val BR_CONNECTION_ESTABLISHED = "CONNECTION_ESTABLISHED"
    const val BR_CONNECTION_FAILURE = "CONNECTION_FAILURE"

    // WebRTC Firebase Client
    const val USER_TYPE_CAREGIVER = "caregivers"
    const val USER_TYPE_VIU = "vius"

    // Object Detection Settings (ViuLocalSettings)
    const val PREF_THRESHOLD = "pref_threshold"
    const val PREF_MAX_RESULTS = "pref_max_results"
    const val PREF_THREADS = "pref_threads"
    const val PREF_DELEGATE = "pref_delegate"

    // Vibration Haptic Patterns (most follows the pattern: PULSE 1, PAUSE, PULSE 2)
    val VIBRATE_DEFAULT_DURATION = 20.milliseconds
    val VIBRATE_KEY_PRESS = 10.milliseconds
    val VIBRATE_BUTTON_TAP = 30.milliseconds
    val VIBRATE_SUCCESS = listOf(
        HapticEvent(80L, isVibration = true), // Pulse 1: 80ms Vibrate
        HapticEvent(durationMs = 50L, isVibration = false), // Pause: 50ms Silence
        HapticEvent(durationMs = 80L, isVibration = true) // Pulse 2: 80ms Vibrate
    )
    val VIBRATE_ERROR = listOf(
        HapticEvent(50L, isVibration = true),
        HapticEvent(durationMs = 50L, isVibration = false),
        HapticEvent(durationMs = 50L, isVibration = true)
    )
    val VIBRATE_OBJECT_DETECTED = listOf(
        HapticEvent(60L, isVibration = true),
        HapticEvent(durationMs = 40L, isVibration = false),
        HapticEvent(durationMs = 60L, isVibration = true)
    )

    // Object Detection
    val OUTDOOR_ITEMS = listOf(
        "bicycle",
        "car",
        "motorcycle",
        "airplane",
        "bus",
        "train",
        "truck",
        "boat",
        "traffic light",
        "fire hydrant",
        "stop sign",
        "parking meter",
        "bench",
        "bird",
        "horse",
        "sheep",
        "cow",
        "elephant",
        "bear",
        "zebra",
        "giraffe",
        "frisbee",
        "skis",
        "snowboard",
        "sports ball",
        "kite",
        "baseball bat",
        "baseball glove",
        "skateboard",
        "surfboard",
        "tennis racket"
    )

    val COLLISION_ITEMS = listOf(
        "person",
        "bicycle",
        "car",
        "motorcycle",
        "airplane",
        "bus",
        "train",
        "truck",
        "boat",
        "traffic light",
        "fire hydrant",
        "stop sign",
        "parking meter",
        "bench",
        "horse",
        "cow",
        "elephant",
        "bear",
        "zebra",
        "giraffe",
        "couch",
        "bed",
        "dining table",
        "tv",
        "microwave",
        "oven",
        "refrigerator",
        "toilet"
    )
}


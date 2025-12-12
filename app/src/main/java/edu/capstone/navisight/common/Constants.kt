package edu.capstone.navisight.common

object Constants {

    // Shared Preferences
    const val SHARED_PREFERENCES_NAME = "NaviData"
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

}
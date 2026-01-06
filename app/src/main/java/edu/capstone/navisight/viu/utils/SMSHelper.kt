package edu.capstone.navisight.viu.utils

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import edu.capstone.navisight.common.ConverterHelpers.convertMillisToDetailedDateTime
import edu.capstone.navisight.common.ConverterHelpers.convertMillisToTime
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.viu.model.Viu

private const val TAG = "SMSHelper"

object SMSHelper {
    fun sendSMSDirectly(context: Context, phoneNumber: String, message: String) {
        val formattedNumber = if (phoneNumber.startsWith("+63")) {
            phoneNumber.replaceFirst("+63", "09")
        } else {
            phoneNumber
        }

        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Log.e(TAG, "Phone number or message cannot be empty.")
            return
        }

        try {
            val defaultSubId = SubscriptionManager.getDefaultSmsSubscriptionId()
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(defaultSubId)
            } else {
                // Fallback if older version
                SmsManager.getSmsManagerForSubscriptionId(defaultSubId)
            }

            // Send
            smsManager?.sendTextMessage(
                formattedNumber,
                null,
                message,
                null,
                null)
            Log.i(TAG, "SMS sent successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "SMS failed to send: ${e.message}")
            e.printStackTrace()
        }
    }

    fun sendEmergencySMS(context: Context,
                         viu : Viu,
                         caregiver : Caregiver,
                         lastLocationLongitude : String,
                         lastLocationLatitude : String,
                         lastLocationTimestamp : Long,
                         batteryLevel : Int) {

        val latDouble = lastLocationLatitude.toDoubleOrNull() ?: 0.0
        val latDirection = if (latDouble >= 0) "N" else "S"
        val formattedLatitude = "${String.format("%.6f", Math.abs(latDouble))} $latDirection"
        val lonDouble = lastLocationLongitude.toDoubleOrNull() ?: 0.0
        val lonDirection = if (lonDouble >= 0) "E" else "W"
        val formattedLongitude = "${String.format("%.6f", Math.abs(lonDouble))} $lonDirection"
        val timeString = convertMillisToTime(lastLocationTimestamp)
        var caregiverPhoneNumber = caregiver.phoneNumber

        val message = """
           VIU EMERGENCY - NAVISIGHT
           ${viu.firstName} ${viu.lastName} needs help as of $timeString
           Last Location: $formattedLatitude $formattedLongitude
           Battery level: $batteryLevel%
       """.trimIndent()

        // SEND TO DIFFERENT SMS
        Log.e(TAG, message)
        sendSMSDirectly(context, caregiverPhoneNumber, message)
//        sendSMSDirectly(context, "Police", message)
//        sendSMSDirectly(context, "Emergency hotline", message)
    }
}
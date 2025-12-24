package edu.capstone.navisight.viu.utils

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import edu.capstone.navisight.common.ConverterHelpers.convertMillisToDetailedDateTime
import edu.capstone.navisight.viu.model.Caregiver
import edu.capstone.navisight.viu.model.Viu

private const val TAG = "SMSHelper"

object SMSHelper {
    fun sendSMSDirectly(context: Context, phoneNumber: String, message: String) {
        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Log.e(TAG, "Phone number cannot be empty.")
            throw Exception("Phone number cannot be empty.")
        }

        try {
            // FORMAT CORRECTLY THE PHONE NUMBER TO PH STANDARDS

            val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(
                phoneNumber,
                null, // SC (Service Center) address, null uses the default
                message,
                null, // PendingIntent for delivery status (optional)
                null  // PendingIntent for send status (optional)
            )
            Log.e(TAG, "SMS sent successfully!")
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
                         lastLocationTimestamp : Long) {

        val latDouble = lastLocationLatitude.toDoubleOrNull() ?: 0.0
        val latDirection = if (latDouble >= 0) "N" else "S"
        val formattedLatitude = "${String.format("%.6f", Math.abs(latDouble))} $latDirection"
        val lonDouble = lastLocationLongitude.toDoubleOrNull() ?: 0.0
        val lonDirection = if (lonDouble >= 0) "E" else "W"
        val formattedLongitude = "${String.format("%.6f", Math.abs(lonDouble))} $lonDirection"
        val dateTimeString = convertMillisToDetailedDateTime(lastLocationTimestamp)

       val message = """
           EMERGENCY FROM VIU - NaviSight
           VIU ${viu.firstName} ${viu.lastName} needs help.
           Emergency was activated on $dateTimeString
           Last Location: $formattedLatitude $formattedLongitude
       """.trimIndent()

        var caregiverPhoneNumber = caregiver.phoneNumber
        caregiverPhoneNumber = ""

        // SEND TO DIFFERENT SMS
        sendSMSDirectly(context, caregiverPhoneNumber, message)
//        sendSMSDirectly(context, "Police", message)
//        sendSMSDirectly(context, "Emergency hotline", message)
    }
}
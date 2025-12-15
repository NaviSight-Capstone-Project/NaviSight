package edu.capstone.navisight.viu.utils

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast

private const val TAG = "SMSHelper"

object SMSHelper {
    fun sendSMSDirectly(context: Context, phoneNumber: String, message: String) {
        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Log.e(TAG, "Phone number cannot be empty.")
            throw Exception("Phone number cannot be empty.")
        }

        try {
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
}
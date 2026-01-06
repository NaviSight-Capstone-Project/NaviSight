package edu.capstone.navisight.common

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ConverterHelpers {
    fun convertMillisToDate(millis: Long): String {
        val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    fun convertMillisToTime(millis: Long): String {
        val pattern ="hh:mm a"
        val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
        return formatter.format(Date(millis))
    }

    fun convertMillisToDetailedDateTime(millis: Long): String {
        val pattern = "MMM. dd, yyyy 'at' hh:mm a zzz"
        val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }

        return formatter.format(Date(millis))
    }

    // This method assumes the Uri points to a local file stored by the app
    fun uriToFile(context: Context, uri: Uri): File? {
        try {
            if (uri.path != null) {
                return File(uri.path!!)
            }
        } catch (e: Exception) {
            Log.e("EmergencyActivity", "Error converting Uri to File: ${e.message}")
        }
        return null
    }
}
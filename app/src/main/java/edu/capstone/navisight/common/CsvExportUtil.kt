package edu.capstone.navisight.common

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

object CsvExportUtil {
    fun exportAndShare(context: Context, logs: List<GeofenceActivity>, viuName: String) {
        try {
            val fileName = "TravelLog_${viuName.replace(" ", "_")}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            // Header
            writer.append("Date,Time,Event,Location\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            // Rows
            logs.forEach { log ->
                val date = log.timestamp?.toDate()
                val dStr = if (date != null) dateFormat.format(date) else "-"
                val tStr = if (date != null) timeFormat.format(date) else "-"
                val action = if (log.eventType == "ENTER") "Arrived" else "Left"

                writer.append("$dStr,$tStr,$action,${log.geofenceName}\n")
            }
            writer.flush()
            writer.close()

            // Share Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Travel Log"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
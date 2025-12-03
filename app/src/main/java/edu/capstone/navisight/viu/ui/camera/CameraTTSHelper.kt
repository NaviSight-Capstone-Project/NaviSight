package edu.capstone.navisight.viu.ui.camera

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CameraTTSHelper {
    fun getCurrentDateTime(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return now.format(formatter)
    }
}
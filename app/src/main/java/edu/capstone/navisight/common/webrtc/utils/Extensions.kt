package edu.capstone.navisight.common.webrtc.utils

import android.Manifest
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX

fun AppCompatActivity.getCameraAndMicPermission(success:()->Unit){
    PermissionX.init(this)
        .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        .request{allGranted,_,_ ->
            if (allGranted){
                success()
            } else{
                Toast.makeText(this, "Camera and mic permission is required", Toast.LENGTH_SHORT)
                    .show()
            }
        }
}

fun Int.convertToHumanTime() : String {
    val totalSeconds = this
    val hours = totalSeconds / 3600 // 3600 seconds in an hour
    val remainingSecondsAfterHours = totalSeconds % 3600
    val minutes = remainingSecondsAfterHours / 60
    val seconds = remainingSecondsAfterHours % 60

    val secondsString = if (seconds < 10) "0$seconds" else "$seconds"
    val minutesString = if (minutes < 10) "0$minutes" else "$minutes"
    val hoursString = if (hours < 10) "0$hours" else "$hours"

    return "$hoursString:$minutesString:$secondsString"
}
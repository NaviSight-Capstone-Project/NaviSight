package edu.capstone.navisight.common.webrtc.adapter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.R


// Compose, compose, compose.

@Composable
fun UserListView (
    users: SnapshotStateList<Triple<String, String, String>>,
    onVideoCallClicked: (String) -> Unit,
    onAudioCallClicked: (String) -> Unit
) {
    LazyColumn {
        items(users) { user ->
            UserListItem(
                username = user.first,
                uid = user.second,
                status = user.third, // Update if necessary.
                onVideoCallClicked = onVideoCallClicked,
                onAudioCallClicked = onAudioCallClicked
            )
        }
    }
}

@Composable
fun UserListItem(
    uid: String,
    status: String,
    onVideoCallClicked: (String) -> Unit,
    onAudioCallClicked: (String) -> Unit,
    username: String
) {
    val statusColor = when (status) {
        "ONLINE" -> Color(0xFF4CAF50)  // light green
        "OFFLINE" -> Color(0xFFF44336) // red
        "IN_CALL" -> Color(0xFFFFEB3B) // yellow
        else -> Color.Gray
    }

    val isOnline = status == "ONLINE"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {

        Text(
            text = username,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colorResource(id=R.color.dark_violet)
        )
        Text(text=uid, fontSize=14.sp)

        Text(
            text = when (status) {
                "ONLINE" -> "Online"
                "OFFLINE" -> "Offline"
                "IN_CALL" -> "In Call"
                else -> "Unknown"
            },
            color = statusColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        if (isOnline) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onVideoCallClicked(uid) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera_on),
                            contentDescription = "Video Call Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Video Call")
                    }
                }

                // --- Audio Call Button ---
                Button(onClick = { onAudioCallClicked(uid) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_call),
                            contentDescription = "Audio Call Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Audio Call")
                    }
                }
            }
        }
    }
}

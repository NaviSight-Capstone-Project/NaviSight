package edu.capstone.navisight.common.webrtc.adapter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Viu

@Composable
fun UserListView (
    users: List<Triple<Viu, String, String>>,
    listState: LazyListState,
    onVideoCallClicked: (String) -> Unit,
    onAudioCallClicked: (String) -> Unit
) {
    LazyColumn (state=listState){
        items(users) { user ->
            UserListItem(
                viu = user.first,
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
    viu: Viu,
    uid: String,
    status: String,
    onVideoCallClicked: (String) -> Unit,
    onAudioCallClicked: (String) -> Unit,
) {
    val statusColor = when (status) {
        "ONLINE" -> Color(0xFF4CAF50)  // light green
        "OFFLINE" -> Color(0xFFF44336) // red
        "IN_CALL" -> Color(0xFFFFEB3B) // yellow
        else -> Color.Gray
    }

    val isOnline = status == "ONLINE"
    val buttonContainerSize = 45.dp
    val buttonContentSize = 25.dp

    // PFP
    val profileBorderColor = colorResource(R.color.royal_purple)
    val profileBorderWidth = 10.dp
    val profileImageSize = 60.dp
    val statusBadgeSize = 14.dp

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical=8.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                Row {
                    // PROFILE PICTURE
                    Box(modifier = Modifier.size(profileImageSize)) {
                        AsyncImage(
                            model = viu.profileImageUrl,
                            contentDescription = "Profile Image",
                            fallback = painterResource(R.drawable.default_profile),
                            error = painterResource(R.drawable.default_profile),
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .border(profileBorderWidth, profileBorderColor, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(
                            modifier = Modifier
                                .size(statusBadgeSize)
                                .align(Alignment.TopEnd)
                                .background(
                                    color = statusColor,
                                    shape = CircleShape
                                )
                                .padding(end = 0.dp, top = 0.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "${viu.firstName} ${viu.lastName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colorResource(id = R.color.dark_violet),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "PRIMARY",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = when (status) {
                                "ONLINE" -> "Online"
                                "OFFLINE" -> "Offline"
                                "IN_CALL" -> "In Call"
                                else -> "Unknown"
                            },
                            color = statusColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onVideoCallClicked(uid) },
                    enabled = isOnline,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(buttonContainerSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.light_blue),
                        contentColor = colorResource(R.color.blue)
                    )

                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video_call_outline),
                            contentDescription = "Video Call Icon",
                            modifier = Modifier.size(buttonContentSize)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Button(
                    onClick = { onAudioCallClicked(uid) },
                    enabled = isOnline,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(buttonContainerSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.light_purple),
                        contentColor = colorResource(R.color.royal_purple)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_call_outline),
                            contentDescription = "Audio Call Icon",
                            modifier = Modifier.size(buttonContentSize)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

        }
    }

}

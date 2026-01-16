package edu.capstone.navisight.caregiver.ui.feature_notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest
import edu.capstone.navisight.caregiver.model.TransferPrimaryRequest
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import edu.capstone.navisight.caregiver.model.AlertNotification
import edu.capstone.navisight.caregiver.model.AlertType

@Composable
fun NotificationScreen(viewModel: NotificationViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf("Activity") }

    // Observers
    val combinedFeed by viewModel.combinedFeed.collectAsState()
    val unreadAlerts by viewModel.unreadAlerts.collectAsState()

    val secondaryRequests by viewModel.pendingRequests.collectAsState()
    val transferRequests by viewModel.transferRequests.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPendingRequests()
    }

    val onAlertTapped: (AlertNotification) -> Unit = { alert ->
        viewModel.dismissAlert(alert.id)

        println("Alert tapped! ID: ${alert.id}, VIU: ${alert.viu.firstName}")

        val details = alert.extraDetails
        val viuName = alert.viu.firstName

        println("Details for ${alert.title} from $viuName coming soon. " +
                "Last Location: ${details["lastKnownLocation"]}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notification_outline),
                contentDescription = null,
                tint = Color(0xFF6041EC),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF202833),
                style = TextStyle(brush = Brush.verticalGradient(colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton("Activity", selectedTab == "Activity", { selectedTab = "Activity" }, Modifier.weight(1f))
            TabButton("Request", selectedTab == "Request", { selectedTab = "Request" }, Modifier.weight(1f))
//            TabButton("Read", selectedTab == "Read",
//                { selectedTab = "Read" }, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            "Activity" -> {
                if (combinedFeed.isEmpty()) { // Use combinedFeed here
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recent activity or alerts.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(combinedFeed) { item ->
                            when (item) {
                                is GeofenceActivity -> ActivityRow(
                                    activity = item,
                                    onDelete = { viewModel.deleteFeedItem(item.id, false) }
                                )
                                is AlertNotification -> AlertRow(
                                    alert = item,
                                    onDelete = { viewModel.deleteFeedItem(item.id, isAlert = true, isRead = false) },
                                    onClick = { onAlertTapped(item) }
                                )
                                // TODO: Add more types dito
                            }
                        }
                    }
                }

            }
            "Request" -> {
                when {
                    loading && secondaryRequests.isEmpty() && transferRequests.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = error ?: "", color = Color.Red) }
                    secondaryRequests.isEmpty() && transferRequests.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pending requests.", color = Color.Gray) }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(transferRequests) { request ->
                                TransferRequestCard(
                                    request = request,
                                    onApprove = { viewModel.approveTransfer(request) },
                                    onDeny = { viewModel.denyTransfer(request.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(secondaryRequests) { request ->
                                RequestRow(
                                    request = request,
                                    onApprove = { viewModel.approveRequest(request) },
                                    onDeny = { viewModel.denyRequest(request.id) }
                                )
                                Divider(color = Color(0x11000000), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
//            "Read" -> {
//                if (readAlerts.isEmpty()) {
//                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        Text("No read alerts", color = Color.Gray)
//                    }
//                } else {
//                    LazyColumn(
//                        verticalArrangement = Arrangement.spacedBy(8.dp),
//                        contentPadding = PaddingValues(bottom = 16.dp)
//                    ) {
//                        items(readAlerts) { alert ->
//                            AlertRow(
//                                alert = alert,
//                                onDelete = { viewModel.deleteFeedItem(
//                                    alert.id,
//                                    isAlert = true,
//                                    isRead = true) }, // Pass isRead = true for deletion
//                                onClick = {
//                                    // Optionally show details again, but don't mark as read
//                                    println("Read Alert :D! ID: ${alert.id}")
//                                }
//                            )
//                        }
//                    }
//                }
//            }
        }
    }
}


@Composable
fun AlertRow(
    alert: AlertNotification,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateString = alert.timestamp?.let {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Just now"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            // Apply clickable modifier to the card, but exclude the delete button area
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (alert.type == AlertType.LOW_BATTERY)
                        {Color(0xFFF4B536).copy(alpha = 0.1f)} // Yellow
                        else Color(0xFFF44336).copy(alpha = 0.1f) // Red icon background
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("!", fontSize = 20.sp, fontWeight = FontWeight.Black,
                    color = if (alert.type == AlertType.LOW_BATTERY)
                    Color(0xFFF4B536) // Yellow
                    else Color(0xFFF44336) // Red icon background
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (alert.type == AlertType.LOW_BATTERY)
                    Color(0xFF9D8C1C) // Yellow
                    else Color(0xFF9D281C) // Red
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = alert.message,
                    fontSize = 13.sp,
                    color = Color(0xFF414040)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Alert",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ActivityRow(
    activity: GeofenceActivity,
    onDelete: () -> Unit
) {
    val isEnter = activity.eventType == "ENTER"
    val iconColor = if (isEnter) Color(0xFF4CAF50) else Color(0xFFF44336)

    // Format Timestamp
    val dateString = activity.timestamp?.toDate()?.let {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Just now"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${activity.viuName} ${if(isEnter) "arrived at" else "left"} ${activity.geofenceName}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF202833)
                )
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
    )
    val inactiveColor = Color(0xFFECECFF)

    Box(
        modifier = modifier
            .height(38.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(50.dp)
            )
            .background(
                if (isSelected) activeGradient
                else Brush.linearGradient(listOf(inactiveColor, inactiveColor)),
                shape = RoundedCornerShape(50.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFF6B6B6B),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TransferRequestCard(
    request: TransferPrimaryRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)) // Orange tint for urgency
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Transferring Primary Access To You",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFE65100)
            )
            Text(
                text = "From: ${request.currentPrimaryCaregiverName}",
                fontSize = 13.sp,
                color = Color(0xFF414040)
            )
            Text(
                text = "For VIU: ${request.viuName}",
                fontSize = 13.sp,
                color = Color(0xFF414040)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.height(32.dp).weight(1f)
                ) {
                    Text("ACCEPT", color = Color.White, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDeny,
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.height(32.dp).weight(1f)
                ) {
                    Text("DENY", color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RequestRow(
    request: SecondaryPairingRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                painter = painterResource(id = R.drawable.ic_req),
                contentDescription = null,
                tint = Color(0xFFB544F1),
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Requesting To Be Secondary Companion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFB544F1)
                )
                Text(
                    text = "VIU: ${request.viuName ?: "Unknown"}",
                    fontSize = 13.sp,
                    color = Color(0xFF414040)
                )
                Text(
                    text = "Requested by: ${request.requesterName}",
                    fontSize = 13.sp,
                    color = Color(0xFF414040)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(50.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f)
                    ) {
                        Text(
                            "APPROVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDeny,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(50.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f)
                    ) {
                        Text(
                            "DENY",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
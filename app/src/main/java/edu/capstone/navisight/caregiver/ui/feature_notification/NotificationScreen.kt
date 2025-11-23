package edu.capstone.navisight.caregiver.ui.feature_notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import edu.capstone.navisight.caregiver.model.SecondaryPairingRequest

@Composable
fun NotificationScreen(viewModel: NotificationViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf("Activity") }

    val requests by viewModel.pendingRequests.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPendingRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notification),
                contentDescription = "Notification Icon",
                tint = Color(0xFF6041EC),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF202833),
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                text = "Activity",
                isSelected = selectedTab == "Activity",
                onClick = { selectedTab = "Activity" },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Request",
                isSelected = selectedTab == "Request",
                onClick = { selectedTab = "Request" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Content
        when (selectedTab) {
            "Activity" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent activity", color = Color.Gray)
                }
            }
            "Request" -> {
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    error != null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text(text = error ?: "", color = Color.Red) }

                    requests.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text("No pending requests", color = Color.Gray) }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            requests.forEachIndexed { idx, request ->
                                RequestRow(
                                    request = request,
                                    onApprove = { viewModel.approveRequest(request) },
                                    onDeny = { viewModel.denyRequest(request.id) }
                                )
                                if (idx < requests.lastIndex) {
                                    Divider(color = Color(0x11000000), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
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
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "VIU: ${request.viuName ?: "Unknown"}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFB544F1)
            )
            Text(
                text = "Requested by: ${request.requesterName}",
                fontSize = 13.sp,
                color = Color(0xFF414040)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.height(32.dp).weight(1f)
                ) {
                    Text("APPROVE", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = onDeny,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.height(32.dp).weight(1f)
                ) {
                    Text("DENY", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

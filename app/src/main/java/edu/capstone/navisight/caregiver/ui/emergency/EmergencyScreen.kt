package edu.capstone.navisight.caregiver.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Colors
val RedAlert = Color(0xFFE53935)
val GreenResponse = Color(0xFF4CAF50)
val OrangeResponse = Color(0xFFFF9800)
val DefaultText = Color(0xFF333333) // Dark text for readability

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
fun EmergencyScreen(
    title: String = "Emergency Alert",
    headerIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alert Icon",
            tint = RedAlert,
            modifier = Modifier.size(20.dp)
        )
    },
    message: String,
    timestamp: Long,
    responses: List<Pair<String, Color>>,
    onResponseSelected: (String) -> Unit = {}
) {
    var responseToConfirm by remember { mutableStateOf<String?>(null) }

    Scaffold(
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big ahh triangle
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "EMERGENCY ALERT",
                tint = RedAlert,
                modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
            )

            // Title
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = RedAlert,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date and time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 15.dp)
            ) {
                Text(
                    text = "Date & Time Received: ${formatTimestamp(timestamp)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Message
            Text(
                text = message,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = DefaultText
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Responses
            Column(modifier = Modifier.fillMaxWidth()) {
                responses.forEach { (text, color) ->
                    ResponseOption(
                        text = text,
                        color = color,
                        onClick = { responseToConfirm = text }
                    )
                }
            }
        }
    }



    responseToConfirm?.let { response ->
        ConfirmationDialog (
            title = "Confirm Action",
            message = "Are you sure you want to \n'${response}'?\nThis will close the emergency alert.",
            onConfirm = {
                onResponseSelected(response)
                responseToConfirm = null
            },
            onCancel = {
                responseToConfirm = null
            }
        )
    }
}

@Composable
fun ResponseOption(text: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        // Divider line matching the image style
        Divider(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            color = Color(0xFFEEEEEE), // Light gray line
            thickness = 1.dp
        )
    }
}
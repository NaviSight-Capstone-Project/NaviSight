package edu.capstone.navisight.caregiver.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors
val RedAlert = Color(0xFFE53935)
val GreenResponse = Color(0xFF4CAF50)
val YellowResponse = Color(0xFFFFC107)
val DefaultText = Color(0xFF333333) // Dark text for readability

@Composable
fun EmergencyScreen(
    title: String = "Alert Message",
    headerIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alert Icon",
            tint = RedAlert,
            modifier = Modifier.size(20.dp)
        )
    },
    message: String,
    responses: List<Pair<String, Color>>,
    onResponseSelected: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            // Simple Red Header matching the image style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = RedAlert,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Date and time
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        headerIcon()
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Today",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = DefaultText
                        )
                    }
                    Text(
                        text = "12:15",
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

                Spacer(modifier = Modifier.height(30.dp)) // Spacer before responses

                // Responses
                Column(modifier = Modifier.fillMaxWidth()) {
                    responses.forEach { (text, color) ->
                        ResponseOption(
                            text = text,
                            color = color,
                            onClick = { onResponseSelected(text) }
                        )
                    }
                }
            }
        }
    )
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
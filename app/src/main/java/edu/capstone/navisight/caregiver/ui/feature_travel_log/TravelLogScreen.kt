package edu.capstone.navisight.caregiver.ui.feature_travel_log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TravelLogScreen(
    viuUid: String,
    viuName: String,
    viewModel: TravelLogViewModel
) {
    val logs by viewModel.logs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viuUid) {
        viewModel.loadLogs(viuUid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Travel Logs",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF8E41E8),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { viewModel.exportToCsv(context, viuName) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export CSV",
                    tint = Color(0xFF6041EC)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            color = Color.White
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFBAC8FF))
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date/Time",
                        modifier = Modifier.weight(1.2f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Action",
                        modifier = Modifier.weight(0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Location",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8E41E8))
                    }
                } else if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No logs found.", color = Color.Gray)
                    }
                } else {
                    Column {
                        logs.take(5).forEachIndexed { index, log ->
                            LogRows(log)
                            if (index < logs.take(5).size - 1) {
                                Divider(color = Color(0xFFF5F5F5))
                            }
                        }
                    }
                }
            }
        }

        if (logs.size > 5) {
            Text(
                text = "Showing recent 5 logs. Export to view all.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun LogRows(log: GeofenceActivity) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val dateStr = log.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "-"
    val isEnter = log.eventType == "ENTER"

    val color = if (isEnter) Color(0xFF2E7D32) else Color(0xFFC62828)
    val actionText = if (isEnter) "Arrived" else "Left"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateStr,
            modifier = Modifier.weight(1.2f),
            fontSize = 13.sp,
            color = Color.DarkGray
        )
        Text(
            text = actionText,
            modifier = Modifier.weight(0.8f),
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = log.geofenceName,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = Color.Black
        )
    }
}
package edu.capstone.navisight.caregiver.ui.feature_travel_log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.exportToCsv(context, viuName) }) {
                Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Color(0xFF6041EC))
            }
        }

        // Table Headers
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(12.dp)
        ) {
            Text("Date/Time", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Action", modifier = Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Location", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Divider()

        // Table Body
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No logs found.", color = Color.Gray) }
        } else {
            LazyColumn {
                items(logs) { log ->
                    LogRows(log)
                    Divider(color = Color(0xFFEEEEEE))
                }
            }
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
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateStr, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
        Text(actionText, modifier = Modifier.weight(0.8f), fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
        Text(log.geofenceName, modifier = Modifier.weight(1f), fontSize = 12.sp)
    }
}
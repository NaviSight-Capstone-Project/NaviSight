package edu.capstone.navisight.caregiver.ui.feature_map.mapScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.capstone.navisight.caregiver.model.Geofence
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt

@Composable
fun AddGeofenceDialog(
    latLng: LatLng,
    onDismiss: () -> Unit,
    onAdd: (name: String, location: LatLng, radius: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sliderValue by remember { mutableStateOf(50f) }
    val radiusRange = 30f..100f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Geofence") },
        text = {
            Column {
                Text("Location: ${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Geofence Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Radius: ${sliderValue.roundToInt()} meters")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = (it.roundToInt() / 10 * 10).toFloat() },
                    valueRange = radiusRange,
                    steps = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, latLng, sliderValue.toDouble()) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GeofenceDetailsDialog(
    geofence: Geofence,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(geofence.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Radius: ${geofence.radius.format(0)} meters")
                geofence.location?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Location: ${it.latitude.format(4)}, ${it.longitude.format(4)}")
                }
            }
        },
        confirmButton = {
            if (canEdit) {
                Button(
                    onClick = { onDelete(geofence.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (canEdit) "Cancel" else "Close")
            }
        }
    )
}
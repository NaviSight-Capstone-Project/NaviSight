package edu.capstone.navisight.caregiver.ui.emergency

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EmergencyAlertDialog(
    onDismissRequest: () -> Unit,
    message: String,
    responses: List<Pair<String, androidx.compose.ui.graphics.Color>>,
    onResponseSelected: (String) -> Unit,
    timestamp: Long,
    lastLocation: String
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false, // Prevents closing via back button (like the Activity lock)
            dismissOnClickOutside = false, // Prevents closing by tapping outside
            usePlatformDefaultWidth = false // Allows the dialog to take more width
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            EmergencyDialogContent(
                title = "Emergency Alert",
                message = message,
                responses = responses,
                onResponseSelected = { response ->
                    onResponseSelected(response)
                    onDismissRequest() // Close dialog after selecting a response
                },
                timestamp = timestamp
            )
        }
    }
}

@Composable
private fun EmergencyDialogContent(
    title: String,
    message: String,
    responses: List<Pair<String, androidx.compose.ui.graphics.Color>>,
    onResponseSelected: (String) -> Unit,
    timestamp : Long
) {
    EmergencyScreen(
        title = title,
        message = message,
        responses = responses,
        onResponseSelected = onResponseSelected,
        timestamp = timestamp
    )
}
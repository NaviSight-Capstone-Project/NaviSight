package edu.capstone.navisight.viu.ui.braillenote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrailleNoteAppScreen() {
    val context = LocalContext.current

    // State for the note content
    var noteContent by remember { mutableStateOf("") }
    // State to control the visibility of the accessibility dialog
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    // Check accessibility status when the screen is first composed
    LaunchedEffect(Unit) {
        if (!isAccessibilityServiceEnabled(context)) {
            showAccessibilityDialog = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Compose Note Taker") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Note Input Field
            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                label = { Text("Type your note here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Takes up available space
                singleLine = false,
                maxLines = 10,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    if (noteContent.isNotBlank()) {
                        android.widget.Toast.makeText(context, "Note saved!", android.widget.Toast.LENGTH_SHORT).show()
                        noteContent = "" // Clear the field
                    } else {
                        android.widget.Toast.makeText(context, "Note is empty!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = noteContent.isNotBlank()
            ) {
                Text("Save Note")
            }
        }
    }

    // Accessibility Assistance Dialog
    if (showAccessibilityDialog) {
        AccessibilityAssistDialog(
            onDismiss = { showAccessibilityDialog = false },
            onGoToSettings = {
                context.startActivity(createAccessibilitySettingsIntent())
                showAccessibilityDialog = false // Dismiss after starting intent
            }
        )
    }
}

/**
 * The Composable function for the AlertDialog.
 */
@Composable
fun AccessibilityAssistDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibility Required") },
        text = {
            Text("For Braille input, please ensure an accessibility service (like TalkBack) is enabled and the Braille keyboard is active. Would you like to go to settings now?")
        },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
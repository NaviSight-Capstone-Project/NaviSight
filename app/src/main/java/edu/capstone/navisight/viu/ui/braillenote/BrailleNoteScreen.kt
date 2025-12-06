package edu.capstone.navisight.viu.ui.braillenote

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService

fun showKeyboardPicker(context: Context) {
    val imeManager: InputMethodManager? = context.getSystemService()
    if (imeManager != null) {
        imeManager.showInputMethodPicker()
    } else {
        android.widget.Toast.makeText(
            context,
            "Could not access Input Method Manager.",
            android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(
        Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    return accessibilityManager?.let {
        it.isEnabled && !it.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).isNullOrEmpty()
    } ?: false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrailleNoteScreen() {

    val context = LocalContext.current

    var noteContent by remember { mutableStateOf("") }
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

    if (showAccessibilityDialog) {
        AccessibilityAssistDialog(
            onDismiss = { showAccessibilityDialog = false },
            onGoToKeyboardPicker = {
                showKeyboardPicker(context)
                showAccessibilityDialog = false // Dismiss after triggering the picker
            }
        )
    }
}

@Composable
fun AccessibilityAssistDialog(
    onDismiss: () -> Unit,
    onGoToKeyboardPicker: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Braille Keyboard Setup") },
        text = {
            Text("To use Braille input, you need to switch your current keyboard. Please select the Braille keyboard option from the menu.")
        },
        confirmButton = {
            Button(onClick = onGoToKeyboardPicker) {
                Text("Select Keyboard")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
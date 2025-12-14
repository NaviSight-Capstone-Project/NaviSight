package edu.capstone.navisight.viu.ui.braillenote

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// --- MAIN SCREEN (TEXT EDITOR) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrailleNoteScreen(
    viewModel: BrailleViewModel,
    onNavigateToSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.speak("Compose Mode. Use your Braille Keyboard.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSaved) {
                        Icon(Icons.Default.Description, contentDescription = "View Saved")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentText.isNotBlank()) {
                        viewModel.saveNote(currentText)
                        tts?.speak("Note Saved", TextToSpeech.QUEUE_FLUSH, null, null)
                        currentText = ""
                    } else {
                        tts?.speak("Note is empty", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = currentText,
                onValueChange = { currentText = it },
                label = { Text("Type here...") },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 24.sp),
                maxLines = 20
            )
        }
    }
}

// --- SAVED NOTES SCREEN (READING) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedNotesScreen(
    viewModel: BrailleViewModel,
    onBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.speak("Saved Notes", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No saved notes")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                tts?.speak(note.content, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        // ROW: Content on the left, Delete button on the right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = note.getFormattedDate(), style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = note.content, style = MaterialTheme.typography.bodyLarge)
                            }

                            // DELETE BUTTON
                            IconButton(
                                onClick = {
                                    viewModel.deleteNote(note)
                                    tts?.speak("Note Deleted", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
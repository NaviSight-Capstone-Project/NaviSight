package edu.capstone.navisight.caregiver.ui.feature_stream

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.R
import edu.capstone.navisight.common.webrtc.adapter.UserListView
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.common.webrtc.FirebaseClient
import kotlinx.coroutines.launch

@Composable
fun StreamScreen(
    viewModel: StreamViewModel,
     onVideoCall: (username: String) -> Unit,
     onAudioCall: (username: String) -> Unit){
    val firebaseClient = FirebaseClient.getInstance()
    val viuList by viewModel.viuList.collectAsState()
    val filteredUsers by viewModel.filteredViuTriples.collectAsState()
    val listState = rememberLazyListState()

    // Init. search query stuff
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Hold the list of VIU UIDs associated with the caregiver
    var associatedViuUids by remember { mutableStateOf<List<String>?>(null) }

    // Get the current caregiver UID (MUST be non-null if they're signed in)
    val caregiverUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
        // Handle unauthenticated state (e.g., navigate to login, show error)
        Log.e("StreamScreen", "Caregiver UID is null. Cannot fetch relationships.")
        return
    }

    // Fetch Associated VIU UIDs from Firestore
    LaunchedEffect(key1 = caregiverUid) {
        // This runs ONCE when the screen loads
        val uids = firebaseClient.getAssociatedViuUids(caregiverUid)
        associatedViuUids = uids // Update state, triggers the next LaunchedEffect
    }

    //  Observe RTDB only when UIDs are available
    LaunchedEffect(key1 = associatedViuUids) {
        val viusToObserve = associatedViuUids

        // Only run if the UIDs have been fetched (i.e., not null)
        if (viusToObserve != null) {
            if (viusToObserve.isNotEmpty()) {
                viewModel.observeViuStatuses(viusToObserve, firebaseClient)
            } else {
                viewModel.observeViuStatuses(emptyList(), firebaseClient)
            }
        }
    }

    val scope = rememberCoroutineScope()
    val alphabet = ('A'..'Z').toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF0F1))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize(), // Occupy the whole screen area

            horizontalArrangement = Arrangement.SpaceBetween // Pushes the two main children apart
        ) {
            // Set header with main content (this is compartmentalized for alphabet scrollbar)
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Set header
                Row(
                    modifier = Modifier
                        .fillMaxSize(),

                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Set header with main content (list, search, header)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 12.dp, end = 4.dp)
                    ) {
                        // Set header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stream),
                                contentDescription = "Stream Icon",
                                tint = Color(0xFF6041EC),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Live Video and Audio Call",
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

                        // Set search bar.
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) }
                        )


                        Spacer(modifier = Modifier.height(12.dp))

                        // Set main content with controls
                        if (associatedViuUids == null) {
                            // loooooaaaaddddinnngggg
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(25.dp))
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = "Fetching the status of your VIUs.\nPlease wait!",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    )
                                }
                            }

                        } else if (filteredUsers.isEmpty() && associatedViuUids?.isNotEmpty() == true) {
                            // viu state no changes
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (searchQuery.isNotEmpty()) {
                                        Text(
                                            textAlign = TextAlign.Center,
                                            text = "No matching VIUs found for \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Gray
                                            )
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_no_internet),
                                            contentDescription = "Icon",
                                            modifier = Modifier.size(120.dp),
                                            colorFilter = ColorFilter.tint(Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.height(25.dp))
                                        Text(
                                            textAlign = TextAlign.Center,
                                            text = "No VIUs are currently online or matching your search.\nTry again later?",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Gray
                                            )
                                        )
                                    }
                                }

                            }

                        } else if (associatedViuUids?.isEmpty() == true) {
                            //  empty viu
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_no_viu_stream),
                                        contentDescription = "Icon",
                                        modifier = Modifier.size(120.dp),
                                        colorFilter = ColorFilter.tint(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(25.dp))
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = "No VIU currently registered.\nTry registering?",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                UserListView(
                                    users = filteredUsers,
                                    listState = listState,
                                    onVideoCallClicked = { uid ->
                                        Log.d("StreamCheck", "Video call clicked for $uid")
                                        onVideoCall(uid)
                                    },
                                    onAudioCallClicked = { uid ->
                                        Log.d("StreamCheck", "Audio call clicked for $uid")
                                        onAudioCall(uid)
                                    }
                                )
                            }
                        }
                    }

                    // Set alphabet scrollbar
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start=6.dp, end = 0.dp)
                    ) {
                        alphabet.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        val targetIndex = viuList.indexOfFirst {
                                            it.firstName.startsWith(
                                                letter.toString(),
                                                ignoreCase = true
                                            )
                                        }
                                        if (targetIndex != -1) {
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(4.dp, RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        placeholder = {
            Text(
                text = "Search a VIU to video or audio call",
                style = TextStyle(color = Color.Gray, fontSize = 14.sp)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF6041EC)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF6041EC),
            unfocusedBorderColor = Color.Transparent,
            cursorColor = Color(0xFF6041EC)
        )
    )
}

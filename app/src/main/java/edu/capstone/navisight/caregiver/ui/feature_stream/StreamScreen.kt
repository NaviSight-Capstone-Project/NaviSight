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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.common.webrtc.adapter.UserListView
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.common.webrtc.FirebaseClient
import kotlinx.coroutines.launch

@Composable
fun StreamScreen(
    viewModel: StreamViewModel,
    // Init. callbacks for MainActivity
     onVideoCall: (username: String) -> Unit,
     onAudioCall: (username: String) -> Unit){
    val usersList = remember { mutableStateListOf<Triple<Viu, String, String>>() }
    val firebaseClient = FirebaseClient.getInstance()

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
            firebaseClient.observeAssociatedUsersStatus(viusToObserve) { result: List<Pair<Viu?, String>> ->

                val processedUsers = result
                    // The filter for 'viu != null' is still important here
                    // in case credentials fetching failed.
                    .filter { (viu, _) -> viu != null }
                    .mapNotNull { (viu, status) ->
                        val uid = viu?.uid ?: return@mapNotNull null
                        Triple(viu, uid, status)
                    }

                usersList.clear()
                usersList.addAll(processedUsers)
            }
        }
    }

    data class Item(val id: Int, val name: String)

    val rawDataFromSource = remember {
        listOf(
            Item(1, "Dog"),
            Item(2, "Apple"),
            Item(3, "Frog"),
            Item(4, "Banana")
        )
    }

    val yourSortedItemList = remember {
        rawDataFromSource.sortedBy { it.name }
    }


    val listState = rememberLazyListState()


    LazyColumn(state = listState) {
        items(yourSortedItemList) { item ->
            Text(item.name) // Or your custom item composable
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
            Column (
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Set header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Set main content with controls
                if (associatedViuUids == null) {
                    // Show when waiting
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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

                } else if (usersList.isEmpty() && associatedViuUids?.isNotEmpty() == true) {
                    // Show a message if relationships exist, but no one is online
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally

                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_no_internet),
                                contentDescription = "Icon",
                                modifier = Modifier.size(120.dp),
                                colorFilter = ColorFilter.tint(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(25.dp))
                            Text(
                                textAlign = TextAlign.Center,
                                text = "Can't fetch your VIUs at the moment.\nTry again later?",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            )
                        }
                    }

                } else if (usersList.isEmpty() && associatedViuUids?.isEmpty() == true) {
                    // Show on empty
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                    Box (modifier=Modifier
                        .padding(horizontal=12.dp)) {
                        // Show if all is good
                        UserListView(
                            users = usersList,
                            onVideoCallClicked = { username ->
                                Log.d("StreamCheck", "Video call clicked for $username")
                                onVideoCall(username)
                            },
                            onAudioCallClicked = { username ->
                                Log.d("StreamCheck", "Audio call clicked for $username")
                                onAudioCall(username)
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
            ) {
                alphabet.forEach { letter ->
                    Text(
                        text = letter.toString(),
                        modifier = Modifier.clickable {
                            scope.launch {
                                // TODO: Fix connection
                                val targetIndex = yourSortedItemList.indexOfFirst {
                                    it.name.startsWith(letter.toString(), ignoreCase = true)
                                }
                                if (targetIndex != -1) {
                                    // Scroll to the item
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

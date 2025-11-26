package edu.capstone.navisight.viu.ui.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import edu.capstone.navisight.viu.model.Viu
import edu.capstone.navisight.viu.model.QR
import edu.capstone.navisight.R

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onVideoCall: () -> Unit,
    onAudioCall: () -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6041EC))
            }
        }

        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        }

        uiState.user != null -> {
            // MODIFICATION 1: Pass the uiState.qr object
            ProfileContent(
                user = uiState.user,
                qr = uiState.qr, // <-- ADDED THIS
                qrBitmap = uiState.qrBitmap,
                onLogout = onLogout,
                onVideoCall = onVideoCall,
                onAudioCall = onAudioCall
            )
        }
    }
}

@Composable
// MODIFICATION 2: Add the 'qr: QR?' parameter
fun ProfileContent(user: Viu, qr: QR?, qrBitmap: Bitmap?,
                   onLogout: () -> Unit, onVideoCall: () -> Unit,
                   onAudioCall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Card
        ProfileCard(user = user)

        Spacer(modifier = Modifier.height(24.dp))

        // QR Section (Updated Layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "SCAN QR CODE",
                    color = Color(0xFF6041EC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "To Pair",
                    color = Color.Black,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The pairing is one caregiver to one VIU. If the current caregiver approves, you will be in charge of this VIU and Navisight will not be liable for anything that may happen.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "QR: ${qr?.QrUid ?: "N/A"}",
                    color = Color(0xFF6041EC),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .shadow(4.dp, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "User QR Code",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Text(
                    "QR Code not available",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notification Section
        NotificationCard(title = "Notification") {
            NotificationToggleItem("App Notification")
            NotificationToggleItem("Sound Alert")
            NotificationToggleItem("Vibration")
            NotificationToggleItem("Email Alerts")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video call button
        Button(
            onClick = onVideoCall,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = "Video call your caregiver",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Audio call button
        Button(
            onClick = onAudioCall,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A93C3)),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = "Audio call your caregiver",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C5C)),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = "Logout",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileCard(user: Viu) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6041EC), Color(0xFFB644F1))
                    )
                )
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = user.profileImageUrl ?: R.drawable.default_profile,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (!user.status.isNullOrEmpty()) {
                        Text("Status: ${user.status}", fontSize = 13.sp, color = Color.White)
                    }
                    Text("Email: ${user.email}", fontSize = 13.sp, color = Color.White)
                    Text("Phone: ${user.phone}", fontSize = 13.sp, color = Color.White)

                    if (!user.address.isNullOrEmpty()) {
                        Text("Address: ${user.address}", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    title: String,
    titleColor: Color = Color(0xFF4E34C5),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
fun NotificationToggleItem(title: String) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = { isChecked = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4E34C5))
        )
    }
}
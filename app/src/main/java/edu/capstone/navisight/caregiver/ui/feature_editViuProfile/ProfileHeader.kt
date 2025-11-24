package edu.capstone.navisight.caregiver.ui.feature_editViuProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Viu
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun ProfileHeader(
    viuData: Viu,
    isUploading: Boolean,
    onImageClick: () -> Unit
) {

    val inputFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val outputFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val formattedBirthday = remember(viuData.birthday) {
        if (viuData.birthday.isNullOrEmpty()) {
            "N/A"
        } else {
            try {
                // Parse the long date string
                val date = inputFormat.parse(viuData.birthday)
                // Format it to the short string
                date?.let { outputFormat.format(it) } ?: "N/A"
            } catch (e: Exception) {
                // Fallback if parsing fails (e.g., format is already different)
                viuData.birthday
            }
        }
    }
    // Root Row layout
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // IMAGE BOX (LEFT)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !isUploading) { onImageClick() }
        ) {
            AsyncImage(
                model = viuData.profileImageUrl,
                contentDescription = "Profile Image",
                fallback = painterResource(R.drawable.default_profile),
                error = painterResource(R.drawable.default_profile),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isUploading) Color.Black.copy(alpha = 0.5f)
                        else Color.Transparent
                    )
            )

            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Image",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp)
                        .fillMaxWidth()
                )
            }
        }

        // SPACER
        Spacer(modifier = Modifier.width(16.dp))

        // INFO COLUMN (RIGHT)
        Column(
            modifier = Modifier.weight(1f), // Takes up remaining space
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${viuData.firstName} ${viuData.middleName.ifEmpty { "" }} ${viuData.lastName}".replace("  ", " "),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (!viuData.status.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8E4FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = viuData.status,
                        color = Color(0xFF6041EC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            InfoRow(icon = Icons.Filled.Phone, text = viuData.phone)
            InfoRow(icon = Icons.Filled.LocationOn, text = viuData.address ?: "No address set")

            // Sex and Birthday Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth() // Added to help with truncation
            ) {
                Text("Sex: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(
                    text = viuData.sex ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD81B60), // Pinkish color from reference
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Birthday: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1 // Added
                )
                Text(
                    // Use formatted date
                    text = formattedBirthday,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f) // Allow truncation
                )
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6041EC),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}
package edu.capstone.navisight.caregiver.ui.feature_editProfile

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Caregiver

@Composable
internal fun CaregiverProfileHeader(
    caregiverData: Caregiver?,
    selectedImageUri: Uri?,
    isUploading: Boolean,
    onImageClick: () -> Unit
) {

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFFAA41E5), Color(0xFF6342ED))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(enabled = !isUploading) { onImageClick() }
        ) {
            val imageModifier = Modifier.fillMaxSize().clip(CircleShape)
            // Logic to display selected URI, profile URL, or placeholder
            when {
                selectedImageUri != null -> AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )
                !caregiverData?.profileImageUrl.isNullOrEmpty() -> AsyncImage(
                    model = caregiverData?.profileImageUrl,
                    contentDescription = "Profile Image",
                    fallback = painterResource(R.drawable.profile_placeholder), // Use placeholder
                    error = painterResource(R.drawable.profile_placeholder),
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
                else -> Image(
                    painter = painterResource(R.drawable.profile_placeholder),
                    contentDescription = "Default Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )
            }

            // Edit Icon Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isUploading) Color.Black.copy(alpha = 0.5f) // Show overlay when uploading
                        else Color.Transparent
                    )
            )

            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else {
                // Edit icon at the bottom
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

        Text(
            // Handle null profile case for name display
            text = caregiverData?.let { "${it.firstName} ${it.middleName?.takeIf { m -> m.isNotEmpty() }?.let { m -> "$m " } ?: ""}${it.lastName}" } ?: "Companion",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
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
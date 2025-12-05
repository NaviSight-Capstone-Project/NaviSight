package edu.capstone.navisight.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.R

// Converted using AI. TBA when to implement this

val RoyalPurple = Color(0xFF6A0DAD) // Approximate color for @color/royal_purple
val AcceptTint = Color(0xFF003807) // Tint for accept icon
val DeclineTint = Color(0xFF470711) // Tint for decline icon
val BackgroundPopup = Color(0xFFFFFFFF) // Assuming the background drawable is white or similar

/**
 * Composable function equivalent to call_request_notification.xml.
 *
 * @param onAcceptClick Lambda to execute when the Accept button is clicked.
 * @param onDeclineClick Lambda to execute when the Decline button is clicked.
 */
@Composable
fun IncomingCallNotification(
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Root Layout: LinearLayout (Vertical)
    // The fitsSystemWindows="true" equivalent is handled by using WindowInsets
    Column(
        modifier = modifier
            // This is the Compose equivalent of .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.statusBars)
            .fillMaxWidth()
            .wrapContentHeight()
            // Mimic popup_request_background (assuming a simple background color and shape)
            .background(BackgroundPopup) // Placeholder for the drawable background
            .padding(bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TextView: Incoming call (id: title)
            Text(
                text = "Incoming call",
                fontSize = 25.sp,
                color = RoyalPurple,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                // Note: Compose uses Font Families, not literal font names like "verdana"
                fontFamily = FontFamily.SansSerif, // Closest common default for example
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(5.dp)) // Small spacing between title/subtitle

            // TextView: One of your VIU is calling you! (id: incomingCallTitleTv)
            Text(
                text = "One of your VIU is calling you!",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.fillMaxWidth()
            )
        }


        Row(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            // XML gravity="center" on the LinearLayout is achieved via Row alignment
            horizontalArrangement = Arrangement.Center
        ) {
            // AppCompatButton: Accept (id: acceptButton)
            Button(
                onClick = onAcceptClick,
                modifier = Modifier.size(width = 150.dp, height = 40.dp),
//                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green), // Placeholder for rounded_corners_accept_button
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                // DrawableLeft equivalent
                Icon(
                    painter = painterResource(id = R.drawable.ic_call), // Replace with your actual resource ID
                    contentDescription = "Accept Call",
                    tint = AcceptTint,
                    modifier = Modifier.size(18.dp) // Adjust icon size as needed
                )
                Spacer(modifier = Modifier.width(8.dp)) // Spacing between icon and text
                Text(text = "Accept", color = Color.White)
            }

            // Space: 50sp width
            Spacer(modifier = Modifier.width(50.dp))

            // AppCompatButton: Decline (id: declineButton)
            Button(
                onClick = onDeclineClick,
                modifier = Modifier.size(width = 150.dp, height = 40.dp),
//                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red), // Placeholder for rounded_corners_decline_button
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                // DrawableLeft equivalent
                Icon(
                    painter = painterResource(id = R.drawable.ic_end_call), // Replace with your actual resource ID
                    contentDescription = "Decline Call",
                    tint = DeclineTint,
                    modifier = Modifier.size(18.dp) // Adjust icon size as needed
                )
                Spacer(modifier = Modifier.width(8.dp)) // Spacing between icon and text
                Text(text = "Decline", color = Color.White)
            }
        }
    }
}

// Visual.
@Preview(showBackground = true)
@Composable
fun IncomingCallNotificationPreview() {
    IncomingCallNotification(
        onAcceptClick = { /* Do nothing */ },
        onDeclineClick = { /* Do nothing */ }
    )
}
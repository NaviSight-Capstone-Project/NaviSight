package edu.capstone.navisight.viu.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog


@Composable
fun LogoutDialogScreen(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .width(300.dp) // Standard fixed width for alerts
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Are you sure you want to logout?",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "You will need to enter your credentials next time.",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = Color.DarkGray // Muted color for secondary text
                )
            }

            // Set that clean-looking ahh divider
            Divider(color = Color(0xFFC0C0C0), thickness = 0.5.dp)

            // Setup buttons go here
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                ) {
                    Text(
                        text = "No",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Vertical Divider between buttons
                Divider(
                    color = Color(0xFFC0C0C0),
                    modifier = Modifier
                        .height(44.dp) // Match the button height
                        .width(0.5.dp)
                )

                // "Yes" Button (Confirm/Destructive Action)
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = "Yes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
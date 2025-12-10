package edu.capstone.navisight.caregiver.ui.emergency

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp), // Sleek rounded corners
            modifier = Modifier.padding(horizontal = 40.dp), // Ensures padding on sides
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title and Message
                Column(modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 15.dp, end = 15.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Button Separator (Horizontal Divider)
                Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)

                // Buttons (Horizontal Layout with Vertical Separator)
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Cancel Button (Standard text and color)
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(45.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp,
                            color = Color.Blue // Standard iOS blue
                        )
                    }

                    // Vertical Separator
                    Divider(
                        color = Color(0xFFE0E0E0),
                        modifier = Modifier.width(0.5.dp).height(45.dp)
                    )

                    // Confirm Button (Bold and Alert Color for emphasis)
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(45.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = RedAlert // Use the RedAlert color for the primary action
                        )
                    }
                }
            }
        }
    }
}
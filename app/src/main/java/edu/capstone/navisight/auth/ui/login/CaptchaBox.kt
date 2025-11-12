package edu.capstone.navisight.auth.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.capstone.navisight.auth.util.CaptchaState

@Composable
fun CaptchaBox(
    state: CaptchaState,
    onRefresh: () -> Unit,
    onSubmit: (String) -> Unit,
    onPlayAudio: () -> Unit
) {
    var captchaInput by remember { mutableStateOf("") }
    val isLockedOut = System.currentTimeMillis() < state.lockoutEndTime
    val canRefresh = state.refreshesUsed < 5 && !isLockedOut

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (state.solved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Verified",
                    color = Color(0xFF4CAF50),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textDecoration = TextDecoration.LineThrough,
                    color = Color.DarkGray,
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row {
                    IconButton(onClick = onPlayAudio, enabled = !isLockedOut) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Play audio CAPTCHA"
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = canRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh CAPTCHA",
                            tint = if (canRefresh) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }

            Text(
                text = "Refreshes left: ${5 - state.refreshesUsed}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = captchaInput,
                    onValueChange = { captchaInput = it },
                    placeholder = { Text("Enter code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isLockedOut,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit(captchaInput) })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSubmit(captchaInput) },
                    enabled = !isLockedOut,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Verify")
                }
            }
        }

        state.error?.let {
            Text(
                text = it,
                color = if (it.contains("Locked out")) Color.Red else Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
package edu.capstone.navisight.auth.ui.signup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import edu.capstone.navisight.auth.ui.signup.caregiver.CaregiverSignupActivity
import edu.capstone.navisight.auth.ui.signup.viu.ViuSignupActivity

class RoleSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Your Theme
            // NaviSightTheme {
            RoleSelectionScreen(
                onCaregiverClicked = {
                    startActivity(Intent(this, CaregiverSignupActivity::class.java))
                },
                onViuClicked = {
                    startActivity(Intent(this, ViuSignupActivity::class.java))
                },
                onBackToLogin = {
                    finish() // Simply closes this activity and returns to Login
                }
            )
            // }
        }
    }
}
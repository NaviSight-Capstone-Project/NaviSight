package edu.capstone.navisight.auth.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import edu.capstone.navisight.auth.ui.signup.RoleSelectionActivity
import edu.capstone.navisight.viu.ui.ViuHomeActivity
import edu.capstone.navisight.caregiver.ui.CaregiverHomeActivity

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userCollection by viewModel.userCollection.collectAsState()

            LaunchedEffect(userCollection) {
                when (userCollection) {
                    "vius" -> {
                        startActivity(Intent(this@LoginActivity, ViuHomeActivity::class.java))
                        finish()
                    }
                    "caregivers" -> {
                        startActivity(Intent(this@LoginActivity, CaregiverHomeActivity::class.java))
                        finish()
                    }
                }
            }

            // Call the new LoginScreen
            LoginScreen(
                viewModel = viewModel,
                onSignUp = {
                    startActivity(Intent(this@LoginActivity, RoleSelectionActivity::class.java))
                }
            )
        }
    }
}
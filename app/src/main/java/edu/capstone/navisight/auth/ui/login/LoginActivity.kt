package edu.capstone.navisight.auth.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import edu.capstone.navisight.viu.ui.ViuHomeActivity
import edu.capstone.navisight.caregiver.ui.CaregiverHomeActivity

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToViuHome = {
                    startActivity(Intent(this, ViuHomeActivity::class.java))
                    finish()
                },
                onNavigateToCaregiverHome = {
                    startActivity(Intent(this, CaregiverHomeActivity::class.java))
                    finish()
                }
            )
        }
    }
}

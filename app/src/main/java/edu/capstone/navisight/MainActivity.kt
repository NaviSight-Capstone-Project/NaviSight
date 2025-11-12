package edu.capstone.navisight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        startActivity(Intent(this, edu.capstone.navisight.auth.ui.login.LoginActivity::class.java))
        finish()
    }
}

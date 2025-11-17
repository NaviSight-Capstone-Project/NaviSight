package edu.capstone.navisight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.domain.GetUserCollectionUseCase
import edu.capstone.navisight.auth.ui.login.LoginActivity
import edu.capstone.navisight.caregiver.CaregiverHomeActivity
import edu.capstone.navisight.viu.ui.ViuHomeActivity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var getUserCollectionUseCase: GetUserCollectionUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CloudinaryDataSource.init(this)
        auth = FirebaseAuth.getInstance()
        getUserCollectionUseCase = GetUserCollectionUseCase()

        lifecycleScope.launch {
            val currentUser = auth.currentUser

            if (currentUser == null) {
                navigateTo(LoginActivity::class.java)
            } else {
                try {
                    val collection = getUserCollectionUseCase(currentUser.uid)
                    when (collection) {
                        "vius" -> navigateTo(ViuHomeActivity::class.java)
                        "caregivers" -> navigateTo(CaregiverHomeActivity::class.java)
                        else -> {
                            auth.signOut()
                            navigateTo(LoginActivity::class.java)
                        }
                    }
                } catch (e: Exception) {
                    navigateTo(LoginActivity::class.java)
                }
            }
        }
    }


    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}
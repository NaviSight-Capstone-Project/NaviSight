package edu.capstone.navisight

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import edu.capstone.navisight.auth.data.remote.CloudinaryDataSource
import edu.capstone.navisight.auth.domain.GetUserCollectionUseCase
import edu.capstone.navisight.auth.ui.login.LoginActivity
import edu.capstone.navisight.caregiver.CaregiverHomeActivity
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import edu.capstone.navisight.guest.GuestFragment
import edu.capstone.navisight.viu.ViuHomeActivity
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var getUserCollectionUseCase: GetUserCollectionUseCase
    private val getCurrentUserUidUseCase = GetCurrentUserUidUseCase()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        MapLibre.getInstance(this)
        CloudinaryDataSource.init(this)
        auth = FirebaseAuth.getInstance()
        getUserCollectionUseCase = GetUserCollectionUseCase()

        lifecycleScope.launch {
            val currentUser = getCurrentUserUidUseCase()

            if (currentUser == null) {
                navigateToGuestMode()
            } else {
                try {
                    val collection = getUserCollectionUseCase(currentUser)
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

    fun navigateToGuestMode() {
        supportFragmentManager.commit {
            add(R.id.fragment_container, GuestFragment())
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}
package edu.capstone.navisight.caregiver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.ui.login.LoginActivity
import edu.capstone.navisight.caregiver.ui.navigation.BottomNavigationBar
import edu.capstone.navisight.caregiver.ui.feature_map.MapFragment // We will create this
import edu.capstone.navisight.caregiver.ui.feature_notification.NotificationFragment // We will create this
import edu.capstone.navisight.caregiver.ui.feature_records.RecordsFragment // We will create this
import edu.capstone.navisight.caregiver.ui.feature_settings.SettingsFragment // We will create this
import edu.capstone.navisight.caregiver.ui.feature_stream.StreamFragment // We will create this
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity

class CaregiverHomeActivity : AppCompatActivity() {

    private val viewModel: CaregiverHomeViewModel by viewModels()

    // Keep track of fragments
    private val mapFragment = MapFragment()
    private val recordsFragment = RecordsFragment()
    private val streamFragment = StreamFragment()
    private val notificationsFragment = NotificationFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Set the XML layout
        setContentView(R.layout.activity_caregiver_home)

        // 2. Setup Bottom Navigation Bar in its ComposeView
        val bottomNavView = findViewById<ComposeView>(R.id.bottom_nav_compose_view)
        bottomNavView.setContent {
            // Get the current index from the ViewModel
            val currentIndex by viewModel.currentScreenIndex.collectAsState()

            // Your BottomNavigationBar composable!
            BottomNavigationBar(
                currentIndex = currentIndex,
                onItemSelected = { index ->
                    viewModel.onScreenSelected(index)
                }
            )
        }

        // 3. Observe session and navigation state
        lifecycleScope.launch {
            // Session Check
            viewModel.isSessionValid.collect { isValid ->
                if (!isValid) {
                    startActivity(Intent(this@CaregiverHomeActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            // Navigation Controller
            viewModel.currentScreenIndex.collect { index ->
                when (index) {
                    0 -> showFragment(mapFragment)
                    1 -> showFragment(recordsFragment)
                    2 -> showFragment(streamFragment)
                    3 -> showFragment(notificationsFragment)
                    4 -> showFragment(settingsFragment)
                }
            }
        }
    }

    /**
     * Swaps the currently visible fragment in the container.
     * This hides all other fragments to preserve their state.
     */
    private fun showFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        // If the fragment hasn't been added yet, add it
        if (!fragment.isAdded) {
            transaction.add(R.id.fragment_container, fragment)
        }

        // Hide all fragments
        supportFragmentManager.fragments.forEach {
            if (it.id == R.id.fragment_container) {
                transaction.hide(it)
            }
        }

        // Show the one we want
        transaction.show(fragment)
        transaction.commit()
    }
}
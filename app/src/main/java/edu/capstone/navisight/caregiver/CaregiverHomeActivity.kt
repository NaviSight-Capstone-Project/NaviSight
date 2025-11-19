package edu.capstone.navisight.caregiver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.ui.login.LoginActivity
import edu.capstone.navisight.caregiver.ui.feature_map.MapFragment
import edu.capstone.navisight.caregiver.ui.feature_notification.NotificationFragment
import edu.capstone.navisight.caregiver.ui.feature_records.RecordsFragment
import edu.capstone.navisight.caregiver.ui.feature_records.RecordsFragment.OnViuClickedListener
import edu.capstone.navisight.caregiver.ui.feature_settings.SettingsFragment
import edu.capstone.navisight.caregiver.ui.feature_stream.StreamFragment
import edu.capstone.navisight.caregiver.ui.navigation.BottomNavigationBar
import kotlinx.coroutines.launch

class CaregiverHomeActivity : AppCompatActivity(), OnViuClickedListener {

    private val viewModel: CaregiverHomeViewModel by viewModels()

    private val mapFragment = MapFragment()
    private val recordsFragment = RecordsFragment()
    private val streamFragment = StreamFragment()
    private val notificationsFragment = NotificationFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_home)

        setupBottomNavigation()
        observeSession()
        observeNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavView = findViewById<ComposeView>(R.id.bottom_nav_compose_view)
        bottomNavView.setContent {
            val currentIndex by viewModel.currentScreenIndex.collectAsState()

            BottomNavigationBar(
                currentIndex = currentIndex,
                onItemSelected = { index ->
                    viewModel.onScreenSelected(index)
                }
            )
        }
    }

    private fun observeSession() {
        lifecycleScope.launch {
            viewModel.isSessionValid.collect { isValid ->
                if (!isValid) {
                    startActivity(Intent(this@CaregiverHomeActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            viewModel.currentScreenIndex.collect { index ->

                when (index) {
                    0 -> showFragment(mapFragment, "TAG_MAP")
                    1 -> showFragment(recordsFragment, "TAG_RECORDS")
                    2 -> showFragment(streamFragment, "TAG_STREAM")
                    3 -> showFragment(notificationsFragment, "TAG_NOTIFICATIONS")
                    4 -> showFragment(settingsFragment, "TAG_SETTINGS")
                }
            }
        }
    }


    private fun showFragment(fragmentInstance: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            transaction.add(R.id.fragment_container, fragmentInstance, tag)
        }

        transaction.commit()
    }


    override fun onViuClicked(viuUid: String) {
        // Navigation to edit ui profile
        Toast.makeText(this, "Clicked VIU: $viuUid", Toast.LENGTH_SHORT).show()


    }
}
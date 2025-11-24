package edu.capstone.navisight.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import edu.capstone.navisight.MainActivity
import edu.capstone.navisight.R
import edu.capstone.navisight.auth.ui.login.LoginFragment
import edu.capstone.navisight.auth.ui.signup.RoleSelectionFragment
import edu.capstone.navisight.auth.ui.signup.caregiver.CaregiverSignupFragment
import edu.capstone.navisight.auth.ui.signup.viu.ViuSignupFragment

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.auth_fragment_container, LoginFragment())
            }
        }
    }

    fun onLoginSuccess() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun navigateToRoleSelection() {
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                android.R.anim.slide_in_left, android.R.anim.slide_out_right
            )
            replace(R.id.auth_fragment_container, RoleSelectionFragment())
            addToBackStack(null)
        }
    }

    fun navigateToSignUp() = navigateToRoleSelection()

    fun navigateToCaregiverSignup() {
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                android.R.anim.slide_in_left, android.R.anim.slide_out_right
            )
            replace(R.id.auth_fragment_container, CaregiverSignupFragment())
            addToBackStack(null)
        }
    }

    fun navigateToViuSignup() {
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                android.R.anim.slide_in_left, android.R.anim.slide_out_right
            )
            replace(R.id.auth_fragment_container, ViuSignupFragment())
            addToBackStack(null)
        }
    }
}
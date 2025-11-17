package edu.capstone.navisight.auth.ui.signup.viu

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.capstone.navisight.auth.ui.login.LoginActivity
import com.yalantis.ucrop.UCrop
import java.io.File

class ViuSignupActivity : ComponentActivity() {

    private val viewModel: ViuSignupViewModel by viewModels()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { startCrop(it) }
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val croppedUri = UCrop.getOutput(result.data!!)
                croppedUri?.let { viewModel.onProfileImageCropped(it) }
            }
        }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

        val options = UCrop.Options().apply {
            setToolbarColor(android.graphics.Color.parseColor("#6342ED")) // Gradient End
            setStatusBarColor(android.graphics.Color.parseColor("#6342ED"))
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#78E4EF")) // Gradient Start
            setToolbarWidgetColor(android.graphics.Color.WHITE)
            setCircleDimmedLayer(true)
        }

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(this)

        cropLauncher.launch(cropIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Your Theme
            // NaviSightTheme {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "signup") {

                composable("signup") {
                    ViuSignupScreen(
                        viewModel = viewModel,
                        onSelectImageClick = { imagePickerLauncher.launch("image/*") },
                        onSignupSuccess = { uid ->
                            navController.navigate("otp/$uid") {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        onBackClick = { finish() }
                    )
                }

                composable(
                    route = "otp/{uid}",
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })
                ) { backStackEntry ->
                    val uid = backStackEntry.arguments?.getString("uid") ?: ""

                    ViuOtpScreen(
                        viewModel = viewModel,
                        uid = uid,
                        onVerificationSuccess = {
                            val intent = Intent(this@ViuSignupActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        },
                        onCancelSignup = {
                            viewModel.cancelSignup(uid)
                            finish()
                        }
                    )
                }
            }
            // }
        }
    }
}
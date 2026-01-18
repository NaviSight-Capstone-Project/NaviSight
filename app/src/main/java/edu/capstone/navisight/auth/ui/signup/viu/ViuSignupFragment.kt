package edu.capstone.navisight.auth.ui.signup.viu

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yalantis.ucrop.UCrop
import edu.capstone.navisight.auth.AuthActivity
import java.io.File

class ViuSignupFragment : Fragment() {

    private val viewModel: ViuSignupViewModel by viewModels()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { startCrop(it) }
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val croppedUri = UCrop.getOutput(result.data!!)
                croppedUri?.let { viewModel.onProfileImageCropped(it) }
            }
        }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

        val options = UCrop.Options().apply {

            setToolbarColor(android.graphics.Color.parseColor("#6342ED"))
            setStatusBarColor(android.graphics.Color.parseColor("#6342ED"))
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#78E4EF"))
            setToolbarWidgetColor(android.graphics.Color.WHITE)
            setCircleDimmedLayer(true)
            setShowCropFrame(true)
            setShowCropGrid(false)
        }

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(requireContext())

        cropLauncher.launch(cropIntent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadLocationData(requireContext()) // Init. location data for dropdown.
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
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
                            onBackClick = {
                                parentFragmentManager.popBackStack()
                            }
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
                                (requireActivity() as? AuthActivity)?.onLoginSuccess()
                            },
                            onCancelSignup = {
                                viewModel.cancelSignup(uid)
                                parentFragmentManager.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
package edu.capstone.navisight.caregiver.ui.feature_editProfile

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.capstone.navisight.R
import edu.capstone.navisight.common.domain.usecase.GetCurrentUserUidUseCase
import com.yalantis.ucrop.UCrop
import java.io.File

class AccountInfoFragment : Fragment() {

    private val viewModel: AccountInfoViewModel by viewModels()
    private val getCurrentUserUidUseCase = GetCurrentUserUidUseCase()

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var ucropLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ucropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                val uid = getCurrentUserUidUseCase.invoke()

                if (resultUri != null && uid != null) {
                    viewModel.uploadProfileImageNow(uid, resultUri)
                } else if (uid == null) {
                    Toast.makeText(context, "User not found, could not upload.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to retrieve cropped image", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(context, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { sourceUri: Uri? ->
            sourceUri?.let {
                launchUCrop(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadLocationData(requireContext()) // Init. location data for dropdown.
    }


    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottom_nav_compose_view)?.visibility = View.GONE // Hide bottom nav
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.bottom_nav_compose_view)?.visibility = View.VISIBLE // Show bottom nav
    }

    private fun launchUCrop(sourceUri: Uri) {
        val context = requireContext()
        val destinationFileName = "cropped_image_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(context.cacheDir, destinationFileName))

        val options = UCrop.Options()
        options.setCircleDimmedLayer(true)
        options.setShowCropGrid(false)
        options.withAspectRatio(1f, 1f)
        options.withMaxResultSize(400, 400)

        val ucropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .getIntent(context)

        ucropLauncher.launch(ucropIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_account_info, container, false)
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val formState by viewModel.formState.collectAsStateWithLifecycle()
                val uid = getCurrentUserUidUseCase.invoke()
                uid?.let {
                    val profile by viewModel.profile.collectAsState()
                    val isSaving by viewModel.isSaving.collectAsState()
                    var uiMessage by remember { mutableStateOf<String?>(null) }
                    val reauthError by viewModel.reauthError.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.uiEvent.collect { message ->
                            uiMessage = message
                        }
                    }

                    LaunchedEffect(it) {
                        viewModel.loadProfile(it)
                    }

                    AccountInfoScreen(
                        profile = profile,
                        selectedImageUri = null,
                        onPickImage = {
                            imagePickerLauncher.launch("image/*")
                        },
                        onSave = { first, middle, last, phone, birthday, address, password ->
                            viewModel.saveProfileChanges(it, first, middle, last, phone, birthday, address, password)
                        },
                        onResendPasswordOtp = {
                            viewModel.resendPasswordChangeOtp(requireContext())
                        },
                        onRequestPasswordChange = { current, new ->
                            viewModel.requestPasswordChange(requireContext(), it, current, new)
                        },
                        onChangeEmail = { newEmail, password ->
                            viewModel.updateEmail(requireContext(), it, newEmail, password)
                        },
                        onVerifyEmailOtp = { otp ->
                            viewModel.verifyEmailOtp(it, otp)
                        },
                        onVerifyPasswordOtp = { otp ->
                            viewModel.verifyPasswordChangeOtp(otp)
                        },
                        onCancelEmailChange = {
                            viewModel.cancelEmailChange(it)
                        },
                        onCancelPasswordChange = {
                            viewModel.cancelPasswordChange(it)
                        },
                        onCheckLockout = {
                            viewModel.checkLockoutAndPerform(it) { }
                        },
                        onDeleteAccount = { password ->
                            viewModel.deleteAccount(it, password) {
                                // On Success: Navigate to Login/Welcome
                                Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_LONG).show()

                                // Restart activity to trigger AuthStateListener or reset app state
                                requireActivity().finish()
                                startActivity(requireActivity().intent)
                            }
                        },
                        isSaving = isSaving,
                        uiMessage = uiMessage,
                        onMessageShown = { uiMessage = null },
                        reauthError = reauthError,
                        onBackClick = {
                            parentFragmentManager.popBackStack()
                        },
                        state=formState,
                        onProvinceSelected = { province ->
                                    viewModel.onEvent(SignupEvent.ProvinceChanged(province))
                                },
                        onCitySelected = { city ->
                            viewModel.onEvent(SignupEvent.CityChanged(city))
                        }
                    )
                }
            }
        }
        (rootView.findViewById<ViewGroup>(R.id.accountInfoContainer)).addView(composeView)
        return rootView
    }
}
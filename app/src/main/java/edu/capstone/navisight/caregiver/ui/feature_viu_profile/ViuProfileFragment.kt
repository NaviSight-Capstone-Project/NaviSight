package edu.capstone.navisight.caregiver.ui.feature_viu_profile

import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.yalantis.ucrop.UCrop
import edu.capstone.navisight.caregiver.ui.feature_edit_viu_profile.EditViuProfileViewModel
import edu.capstone.navisight.caregiver.ui.feature_travel_log.TravelLogViewModel
import java.io.File

class ViuProfileFragment : Fragment() {

    // Inject BOTH ViewModels here so they are scoped to this fragment
    private val editViewModel: EditViuProfileViewModel by viewModels()
    private val travelLogViewModel: TravelLogViewModel by viewModels()

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var ucropLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize uCrop logic (used by Edit VM)
        ucropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    editViewModel.uploadProfileImage(resultUri)
                } else {
                    Toast.makeText(context, "Failed to retrieve cropped image", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(context, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { sourceUri: Uri? ->
            sourceUri?.let { launchUCrop(it) }
        }
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
        return ComposeView(requireContext()).apply {
            setContent {
                // We pass both ViewModels to the container
                ViuProfileContainer(
                    editViewModel = editViewModel,
                    travelLogViewModel = travelLogViewModel,
                    onNavigateBack = { parentFragmentManager.popBackStack() },
                    onLaunchImagePicker = { imagePickerLauncher.launch("image/*") }
                )
            }
        }
    }
}
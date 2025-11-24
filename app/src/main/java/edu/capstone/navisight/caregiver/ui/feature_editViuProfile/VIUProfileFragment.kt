package edu.capstone.navisight.caregiver.ui.feature_editViuProfile

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
import edu.capstone.navisight.R
import com.yalantis.ucrop.UCrop
import java.io.File

class ViuProfileFragment : Fragment() {

    private val viewModel: ViuProfileViewModel by viewModels()

    // ActivityResultLaunchers
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var ucropLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This launcher handles the result *from* uCrop
        ucropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    // Success! Call the ViewModel to upload the cropped image.
                    viewModel.uploadProfileImage(resultUri)
                } else {
                    Toast.makeText(context, "Failed to retrieve cropped image", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(context, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // This launcher handles the result from the gallery
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { sourceUri: Uri? ->
            sourceUri?.let {
                // An image was picked, now launch uCrop for cropping
                launchUCrop(it)
            }
        }
    }

    /**
     * Creates a destination Uri and launches the uCrop activity.
     */
    private fun launchUCrop(sourceUri: Uri) {
        val context = requireContext()
        // Create a unique file name for the cropped image in the cache
        val destinationFileName = "cropped_image_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(context.cacheDir, destinationFileName))

        // Configure uCrop options for a circular profile picture
        val options = UCrop.Options()
        options.setCircleDimmedLayer(true)
        options.setShowCropGrid(false)
        options.withAspectRatio(1f, 1f)
        options.withMaxResultSize(400, 400) // Resize for profile pics

        // Get the intent from uCrop library
        val ucropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .getIntent(context)

        // Launch the uCrop activity
        ucropLauncher.launch(ucropIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ViuProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        parentFragmentManager.popBackStack()
                    },
                    onLaunchImagePicker = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }
        }
    }
}
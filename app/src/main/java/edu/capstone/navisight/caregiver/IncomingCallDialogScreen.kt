// IncomingCallDialogScreen.kt (FIXED)

package edu.capstone.navisight.caregiver

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import edu.capstone.navisight.R
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType

class IncomingCallDialog(
    context: Context,
    private val callModel: DataModel,
    private val onAccept: (DataModel) -> Unit,
    private val onDecline: (DataModel) -> Unit,
    private val viuName: String? = null // This is the initial name (null from fragment)
) : Dialog(context, R.style.DialogAnimation_SlideFromTop) {

    private lateinit var incomingCallTitleTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init content
        setContentView(R.layout.dialog_viu_call_request)
        incomingCallTitleTv = findViewById(R.id.incomingCallTitleTv)

        // Init name (placeholder)
        updateViuName(viuName)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false) // Prevent dismissal by tapping outside

        window?.attributes?.gravity = android.view.Gravity.TOP
        val layoutParams = window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        window?.attributes = layoutParams

        val acceptButton: AppCompatButton = findViewById(R.id.acceptButton)
        val declineButton: AppCompatButton = findViewById(R.id.declineButton)

        acceptButton.setOnClickListener {
            onAccept(callModel)
            dismiss()
        }

        declineButton.setOnClickListener {
            onDecline(callModel)
            dismiss()
        }
    }

    // This function is called on creation (with null) and when the name is fetched (with the name).
    fun updateViuName(name: String?) {
        val displayName = name ?: callModel.sender ?: "Unknown"
        val isVideoCall = callModel.type == DataModelType.StartVideoCall
        val isVideoCallText = if (isVideoCall) "video" else "audio"

        // Ensure the TextView is initialized before accessing it
        if (::incomingCallTitleTv.isInitialized) {
            incomingCallTitleTv.text = "$displayName wants to $isVideoCallText call you!"
        }
    }
}
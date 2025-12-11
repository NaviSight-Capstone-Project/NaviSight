package edu.capstone.navisight.common.webrtc.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import edu.capstone.navisight.common.webrtc.FirebaseClient
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType.*
import edu.capstone.navisight.common.webrtc.utils.UserStatus
import org.webrtc.IceCandidate
import edu.capstone.navisight.common.webrtc.vendor.MyPeerObserver
import edu.capstone.navisight.common.webrtc.vendor.WebRTCClient
import com.google.gson.Gson
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.common.webrtc.GsonSingleton
import edu.capstone.navisight.common.webrtc.service.MainService
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import kotlin.jvm.java

/**
 * Non-DI Singleton MainRepository.
 * Works without Hilt, ready for use with or without WebRTCClient.
 */
class MainRepository private constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
) : WebRTCClient.Listener {
    var listener: Listener? = null
    private var target: String? = null
    // Use target as the primary tracker for call state, and these flags only for pre-connection signaling logic
    private var isCaller = false // Tracks if this instance initiated the current potential call
    private var isCallInProgress = false // Tracks if WebRTC connection is actually established (CONNECTED state)
    private var abortSignalTargetUid: String? = null // The peer to signal for abort/miss/end

    private var remoteView: SurfaceViewRenderer? = null
    private lateinit var currentLatestEvent: DataModel
    private var caregiverStatusListener: (() -> Unit)? = null

    fun login(uid: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.checkRTDB(uid, isDone)
    }

    fun getUserType(): String{
        return firebaseClient.getUserType()
    }

    fun initFirebase() {
        firebaseClient.clearLatestEvent()
        firebaseClient.observeLatestEvents(object : FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                Log.d("MainRepository", "Detected a change on latestevent triggered. Type: ${event.type}")
                listener?.onLatestEventReceived(event)
                currentLatestEvent = event
                abortSignalTargetUid = event.sender // Set the peer for signaling responses

                when (event.type) {
                    StartVideoCall,
                    StartAudioCall -> {
                        // Receiving a call
                        isCaller = false
                        isCallInProgress = false
                        Log.d("MainRepository", "Detected incoming call from ${event.sender}.")
                        // Logic delegates to MainService via listener for UI/timer handling
                    }
                    Offer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(target!!)
                        isCaller = false
                    }
                    Answer -> {
                        Log.d("MainRepository", "Detected an answer.")
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                    }
                    IceCandidates -> {
                        val candidate: IceCandidate? = try {
                            gson.fromJson(event.data.toString(), IceCandidate::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        candidate?.let {
                            webRTCClient.addIceCandidateToPeer(it)
                        }
                    }
                    EndCall -> {
                        listener?.endCall()
                    }
                    DenyCall -> {
                        listener?.deniedCall()
                    }
                    AbortCall -> {
                        listener?.abortCall()
                    }
                    MissCall -> {
                        listener?.missCall()
                    }
                    else -> Unit
                }
            }
        })
    }
    // Set the user offline and make sure any calls are automatically aborted.
    fun setOffline() {
        firebaseClient.changeMyStatus(UserStatus.OFFLINE)
        Log.d("abortsignal", "set offline triggered")
        // If we are the caller and haven't established connection, send an abort signal
        if (isCaller && !isCallInProgress && target != null) {
            sendAbortCall(target!!)
        }
        webRTCClient.closeConnection()
    }

    fun sendConnectionRequest(target: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = if (isVideoCall) StartVideoCall else StartAudioCall,
                target = target
            ), success
        )
        this.target = target
        this.abortSignalTargetUid = target
        isCaller = true
        isCallInProgress = false // Reset state
        Log.d("MainRepository", "Triggered send connection request to: $target")
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
        fun deniedCall()
        fun onAbortCallConnectionBased(targetId: String)
        fun onMissCall(targetId: String)
        fun missCall()
        fun onConnectionEstablished()
        fun abortCall()
    }

    fun setTarget(target: String) {
        this.target = target
    }

    fun setEmail(email: String) {
        firebaseClient.setEmail(email) // Set the email for making a RTDB entry if none detected.
    }

    fun initWebrtcClient(email: String) {
        webRTCClient.listener = this
        webRTCClient.initializeWebrtcClient(email, object : MyPeerObserver() {

            override fun onAddStream(p0: MediaStream?) {
                Log.d("MainRepository", "onAddStream is activated!")
                super.onAddStream(p0)
                try {
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    Log.e("MainRepository", "onAddStream failed ${e.stackTraceToString()}.")
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webRTCClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d("MainRepository", "onConnectionChange is activated! New State: $newState")
                super.onConnectionChange(newState)

                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        firebaseClient.clearLatestEvent()
                        changeMyStatus(UserStatus.IN_CALL)
                        isCallInProgress = true // Connection established!
                        MainService.stopCallTimer() // Stop missed call timer
                        listener?.onConnectionEstablished()
                    }
                    // Check for failure/disconnection states
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    PeerConnection.PeerConnectionState.FAILED -> {
                        // Only auto-abort if a call was actually in progress or if we were waiting for an answer
                        // The MainService listener handles the cleanup after this signal.
                        listener?.onAbortCallConnectionBased(target ?: "Unknown")
                    }
                    else -> Unit
                }
            }
        })
        Log.d("MainRepository", "initWebRTC client should have been finished")
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall: Boolean) {
        webRTCClient.initLocalSurfaceView(view, isVideoCall)
    }

    fun initLocalAudioOnly() {
        webRTCClient.initLocalAudioOnly()
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
        this.remoteView = view
    }

    fun startCall() {
        webRTCClient.call(target!!)
    }

    fun endCall() {
        webRTCClient.closeConnection()
        changeMyStatus(UserStatus.ONLINE)
        isCaller = false
        isCallInProgress = false
        target = null
        abortSignalTargetUid = null
        caregiverStatusListener?.invoke()
        caregiverStatusListener = null
    }

    fun sendEndCall(endCallTarget: String? = target) {
        // Use target or the last known abortSignalTargetUid if target is null (safe fallback)
        val signalTarget = endCallTarget ?: abortSignalTargetUid ?: return
        onTransferEventToSocket(
            DataModel(
                type = EndCall,
                target = signalTarget
            )
        )
    }

    fun sendDeniedCall(deniedCallTarget: String? = abortSignalTargetUid) {
        val signalTarget = deniedCallTarget ?: return
        onTransferEventToSocket(
            DataModel(
                type = DenyCall,
                target = signalTarget
            )
        )
    }

    fun sendMissCall(missCallTarget: String? = abortSignalTargetUid) {
        val signalTarget = missCallTarget ?: return
        onTransferEventToSocket(
            DataModel(
                type = MissCall,
                target = signalTarget
            )
        )
    }

    fun sendEndOrAbortCall() {
        val signalTarget = target ?: abortSignalTargetUid ?: return
        val verdict = when {
            isCallInProgress -> EndCall // Established connection, must be an end call
            isCaller && !isCallInProgress -> AbortCall // Caller, pre-connection, must be an abort
            else -> EndCall // Default to EndCall if unsure (e.g., callee hangs up before answering)
        }

        onTransferEventToSocket(
            DataModel(
                type = verdict,
                target = signalTarget
            )
        )
    }

    fun sendAbortCall(abortTarget: String? = abortSignalTargetUid) {
        val signalTarget = abortTarget ?: return
        onTransferEventToSocket(
            DataModel(
                type = AbortCall,
                target = signalTarget
            )
        )
    }

    private fun changeMyStatus(status: UserStatus) {
        firebaseClient.changeMyStatus(status)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data) {}
    }

    fun setScreenCaptureIntent(screenPermissionIntent: Intent) {
        webRTCClient.setPermissionIntent(screenPermissionIntent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        if (isStarting) {
            webRTCClient.startScreenCapturing()
        } else {
            webRTCClient.stopScreenCapturing()
        }
    }

    fun logOff(function: () -> Unit) = firebaseClient.logOff(function)

    companion object {
        @Volatile
        private var instance: MainRepository? = null

        fun getInstance(context: Context): MainRepository {
            return instance ?: synchronized(this) {
                instance ?: MainRepository(
                    FirebaseClient.getInstance(),
                    WebRTCClient.getInstance(context, GsonSingleton.instance),
                    GsonSingleton.instance
                ).also { instance = it }
            }
        }
    }
}
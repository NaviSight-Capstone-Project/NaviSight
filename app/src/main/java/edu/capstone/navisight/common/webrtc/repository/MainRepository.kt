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
import edu.capstone.navisight.MainActivity
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
    private var abortSignalSenderUidToCheck: String? = null
    private var missSignalTargetUidToCheck: String? = null
    private var justSentACallRequest = false
    private var receivedAVideoOrAudioCall = false
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
        firebaseClient.observeLatestEvents(object : FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                Log.d("MainRepository", "Detected a change on latestevent triggered.")
                listener?.onLatestEventReceived(event)
                currentLatestEvent = event
                when (event.type) {
                    StartVideoCall -> {
                        Log.d("MainRepository", "Detected start video call.")
                        abortSignalSenderUidToCheck = event.sender // Trigger on receiving
                        missSignalTargetUidToCheck = event.target // Trigger on receiving // TODO
                        receivedAVideoOrAudioCall = true
                    }
                    StartAudioCall -> {
                        Log.d("MainRepository", "Detected start video call.")
                        abortSignalSenderUidToCheck = event.sender // Trigger on receiving
                        missSignalTargetUidToCheck = event.target // Trigger on receiving

                        receivedAVideoOrAudioCall = true
                    }
                    Offer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(target!!)
                        abortSignalSenderUidToCheck = event.sender // Trigger on receiving
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
                    else -> {
                        Log.e("MainRepositoryIceCandidate", "I am hitting the else statement? $Unit")
                        Unit
                    }
                }
            }
        })
    }
    // Set the user offline and make sure any calls are automatically aborted.
    fun setOffline() {
        firebaseClient.changeMyStatus(UserStatus.OFFLINE)
        Log.d("abortsignal", "set offline triggered")
        if (abortSignalSenderUidToCheck != null && justSentACallRequest) {
            Log.d("abort", "send abort call should work now")
            sendAbortCall()
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
        abortSignalSenderUidToCheck = target // Trigger on receiving
        justSentACallRequest = true
        Log.d("MainRepository", "Triggered send connection request")
        Log.d("EndCallProblem", "Current abortSignalSenderUidToCheck is: $abortSignalSenderUidToCheck")
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
        fun deniedCall()
        fun onAbortCallConnectionBased(targetId: String)
        fun onMissCall(targetId: String)
        fun missCall()
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
                    Log.d("MainRepository", "onAddStream is finished!")
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    Log.d("MainRepository", "onAddStream failed ${e.stackTraceToString()}.")
                    e.printStackTrace()
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webRTCClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d("MainRepository", "onConnectionChange is activated! New State: $newState") // Log the new state
                super.onConnectionChange(newState)

                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        firebaseClient.clearLatestEvent()
                        changeMyStatus(UserStatus.IN_CALL)
                        MainService.stopCallTimer() // Stop missed call timer
                    }
                    // Check for failure/disconnection states
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    PeerConnection.PeerConnectionState.FAILED -> {
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
        caregiverStatusListener?.invoke()
        caregiverStatusListener = null
    }

    fun sendEndCall(endCallTarget: String? = target!!) {
        onTransferEventToSocket(
            DataModel(
                type = EndCall,
                target = endCallTarget!!
            )
        )
        justSentACallRequest = false
    }

    fun sendDeniedCall(deniedCallTarget: String? = target!!) {
        onTransferEventToSocket(
            DataModel(
                type = DenyCall,
                target = deniedCallTarget!!
            )
        )
        justSentACallRequest = false
    }

    fun sendMissCall() {
        Log.d("misscall", "triggered send misscall, current uid is: $abortSignalSenderUidToCheck")
        onTransferEventToSocket(
            DataModel(
                type = MissCall,
                target = abortSignalSenderUidToCheck.toString()
            )
        )
        justSentACallRequest = false
    }

    fun sendEndOrAbortCall() {
        var verdict = EndCall
        if (justSentACallRequest && !receivedAVideoOrAudioCall) {
            verdict = AbortCall
        }
        onTransferEventToSocket(
            DataModel(
                type = verdict,
                target = abortSignalSenderUidToCheck.toString()
            )
        )
        justSentACallRequest = false
    }

    fun sendAbortCall() {
        onTransferEventToSocket(
            DataModel(
                type = AbortCall,
                target = abortSignalSenderUidToCheck.toString()
            )
        )
        justSentACallRequest = false
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

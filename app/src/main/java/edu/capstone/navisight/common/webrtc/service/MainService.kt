package edu.capstone.navisight.common.webrtc.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import edu.capstone.navisight.common.Constants.BR_ACTION_DENIED_CALL
import edu.capstone.navisight.common.Constants.BR_CONNECTION_ESTABLISHED
import edu.capstone.navisight.common.Constants.BR_CONNECTION_FAILURE
import edu.capstone.navisight.common.Constants.USER_TYPE_VIU
import edu.capstone.navisight.common.NaviSightNotificationManager
import edu.capstone.navisight.common.TextToSpeechHelper
import edu.capstone.navisight.common.webrtc.vendor.RTCAudioManager
import edu.capstone.navisight.common.webrtc.service.MainServiceActions.*
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import org.webrtc.SurfaceViewRenderer


class MainService : Service(), MainRepository.Listener {
    private val TAG = "MainService" // Consistent TAG usage
    private var isServiceRunning = false
    private var email: String? = null

    // Init. vars for call timeout
    private val CALL_TIMEOUT_MS = 30000L // 30 seconds
    private val callTimeoutHandler = Handler(Looper.getMainLooper())
    private var currentIncomingCallerId: String? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager
    private var isPreviousCallStateVideo = true

    private lateinit var mainRepository : MainRepository

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var naviSightNotificationManager: NaviSightNotificationManager

    private val connectionFailureHandler = Handler(Looper.getMainLooper())
    private val connectionFailureRunnable = Runnable {
        Log.d(TAG, "5-second connection failure timeout reached. Auto-ending call.")
        endCallCleanUp()
    }

    private fun showToastOnMainThreadAndTTS(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
        TextToSpeechHelper.queueSpeak(applicationContext, message)
    }

    // This Runnable defines the action to take when the 30 seconds expire
    private val callTimeoutRunnable = Runnable {
        Log.w(TAG, "Call timeout triggered.")
        currentIncomingCallerId?.let { targetId ->
            Log.w(TAG, "Call from $targetId timed out after 30 seconds. Aborting.")
            // Call onMissCall for full cleanup and remote signaling
            onMissCall(targetId)
        }
        currentIncomingCallerId = null
        // The broadcast for missed call will be sent by MainActivity upon receiving onCallMissed from listener
        // The repository logic also handles signaling to the remote peer
    }

    companion object {
        @Volatile // Ensures INSTANCE is up-to-date across all threads
        private var INSTANCE: MainService? = null

        // This is the public method to get the single instance.
        // It uses 'synchronized' to be thread-safe.
        fun getInstance(): MainService {
            return INSTANCE ?: synchronized(this) {
                // If INSTANCE is still null (first time), throw an error.
                // The service must be started via the Android Intent system first.
                return INSTANCE ?: throw IllegalStateException("MainService must be started via Intent before calling getInstance()")
            }
        }

        // Helper function to safely get MainRepository without exposing the Service instance
        fun getMainRepository(): MainRepository? = INSTANCE?.mainRepository

        // Original companion properties remain for static access
        var listener: Listener? = null
        var endAndDeniedCallListener:EndAndDeniedCallListener?=null
        var localSurfaceView: SurfaceViewRenderer?=null
        var remoteSurfaceView: SurfaceViewRenderer?=null
        var screenPermissionIntent : Intent?=null

        // Public static method to safely start the service via the OS
        fun start(context: Context, intent: Intent) {
            intent.setClass(context, MainService::class.java)
            context.startService(intent)
        }

        fun stopCallTimer() {
            // Safely call the instance's method if the service is running
            INSTANCE?.stopCallTimeoutTimer()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MainService created.")
        INSTANCE = this
        mainRepository = MainRepository.getInstance(applicationContext)
        initializeRTCAudioManager()
        naviSightNotificationManager = NaviSightNotificationManager(applicationContext)
    }

    fun initializeRTCAudioManager(){
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand triggered")
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                START_SERVICE.name -> handleStartService(incomingIntent)
                SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                END_CALL.name -> handleEndCall()
                DENY_CALL.name -> handleDeniedCall()
                IDENTIFY_END_OR_ABORT_CALL.name -> handleEndOrAbortCall()
                ABORT_CALL.name -> handleAbortCall()
                SWITCH_CAMERA.name -> handleSwitchCamera()
                TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                TOGGLE_VIDEO.name -> handleToggleVideo(incomingIntent)
                TOGGLE_AUDIO_DEVICE.name -> handleToggleAudioDevice(incomingIntent)
                TOGGLE_SCREEN_SHARE.name -> handleToggleScreenShare(incomingIntent)
                STOP_SERVICE.name -> handleStopService()
                else -> Unit
            }
        }
        return START_STICKY
    }

    // Consolidated call cleanup function
    private fun endCallCleanUp() {
        mainHandler.post{
            Log.d(TAG, "COMMENCING END CALL CLEANUP")
            stopCallTimeoutTimer()
            connectionFailureHandler.removeCallbacks(connectionFailureRunnable)

            // Stop the RTCAudioManager to release audio focus and microphone
            if (::rtcAudioManager.isInitialized) { // Check if initialized before stopping
                rtcAudioManager.stop()
                Log.d(TAG, "RTCAudioManager stopped, releasing microphone.")
            }

            mainRepository.endCall() // Cleans up WebRTC and sets status to ONLINE
            endAndDeniedCallListener?.onCallEnded()
            // Re-initialize the client, but ONLY if the service is meant to stay alive (e.g., for subsequent incoming calls).
            // If the service is about to stop, this is redundant.
            if (isServiceRunning && email != null) {
                // Re-initialize for future calls only if service is running
                mainRepository.initWebrtcClient(email!!)
            }
            // Re-initialize the audio manager so it's ready for the next call
            initializeRTCAudioManager()

            Log.d(TAG, "FINISHED END CALL CLEANUP")
        }
    }

    // Handle denied calls (local action)
    private fun handleDeniedCall() {
        Log.d(TAG, "HANDLING DENIED CALL (Local Action)")
        mainRepository.sendDeniedCall() // Signal remote peer
        endCallCleanUp()
    }

    private fun handleEndOrAbortCall() {
        Log.d(TAG, "HANDLING END OR ABORT CALL")
        mainRepository.sendEndOrAbortCall() // Logic to determine EndCall or AbortCall is in repo
        endCallCleanUp()
    }

    // Handle abort calls (local action)
    private fun handleAbortCall() {
        Log.d(TAG, "HANDLING ABORT CALL (Local Action)")
        listener?.onCallAborted()
        // No need to call endCallCleanUp here as this command is usually for UI cleanup after an immediate failure/abort
        // The clean-up is handled by the peer connection state change in MainRepository.
    }

    private fun handleEndCall() {
        Log.d(TAG, "HANDLING END CALL (Local Action)")
        mainRepository.sendEndCall()
        endCallCleanUp()
    }

    private fun handleStartService(incomingIntent: Intent) {
        // Start our foreground service
        if (!isServiceRunning) {
            isServiceRunning = true
            email = incomingIntent.getStringExtra("email")
            startServiceWithNotification()

            // Setup my clients
            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(email!!)

        }
    }

    private fun handleStopService() {
        Log.d(TAG, "Activating handleStopService()")
        mainRepository.endCall()
        mainRepository.logOff {
            isServiceRunning = false
            stopSelf()
        }
    }

    private fun handleToggleScreenShare(incomingIntent: Intent) {
        val isStarting = incomingIntent.getBooleanExtra("isStarting", true)

        if (isStarting) {
            // Be a rebel and use a safe call (let) instead of the non-null assertion (!!)!!!!
            val intent = screenPermissionIntent

            if (intent == null) {
                Log.e(TAG, "ERROR: Cannot start screen sharing. screenPermissionIntent is null.")
                return
            }

            // Update foreground service to include MEDIA_PROJECTION type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                updateForegroundServiceType(
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            }

            // Start screen share
            // Remove the camera streaming first
            if (isPreviousCallStateVideo) {
                mainRepository.toggleVideo(true) // Stops camera capture
            }

            // Use the safe, non-null 'intent' variable
            mainRepository.setScreenCaptureIntent(intent)
            mainRepository.toggleScreenShare(true)

        } else {
            // Stop screen share and check if camera streaming was on so we should make it on back again
            mainRepository.toggleScreenShare(false)

            // Revert the foreground service type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                updateForegroundServiceType(
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            }

            if (isPreviousCallStateVideo) {
                mainRepository.toggleVideo(false) // Restarts camera capture
            }
        }
    }

    // Update the foreground service type
    private fun updateForegroundServiceType(newType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = notificationManager.activeNotifications
                .firstOrNull { it.id == 1 }?.notification
                ?: naviSightNotificationManager.buildWebrtcServiceStartNotification()
                    .setContentTitle("Navisight Viu") // Update title for ongoing state
                    .setContentText("Ongoing Call Service")
                    .build()
            startForeground(1, notification, newType)
        }
    }

    private fun handleToggleAudioDevice(incomingIntent: Intent) {
        val type = when(incomingIntent.getStringExtra("type")){
            RTCAudioManager.AudioDevice.EARPIECE.name -> RTCAudioManager.AudioDevice.EARPIECE
            RTCAudioManager.AudioDevice.SPEAKER_PHONE.name -> RTCAudioManager.AudioDevice.SPEAKER_PHONE
            else -> null
        }
        type?.let {
            rtcAudioManager.setDefaultAudioDevice(it)
            rtcAudioManager.selectAudioDevice(it)
            Log.d(TAG, "handleToggleAudioDevice: $it")
        }
    }

    private fun handleToggleVideo(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted",true)
        this.isPreviousCallStateVideo = !shouldBeMuted
        mainRepository.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted",true)
        mainRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        mainRepository.switchCamera()
    }

    // Removed endCallAndRestartRepository and deniedCallAndRestartRepository

    private fun handleSetupViews(incomingIntent: Intent) {
        Log.d(TAG, "Handlesetupviews triggered!")

        val isCaller = incomingIntent.getBooleanExtra("isCaller",false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall",true)
        val target = incomingIntent.getStringExtra("target")
        this.isPreviousCallStateVideo = isVideoCall
        mainRepository.setTarget(target!!)

        // Handle surface views here.
        // Set only to allow surface views if and only if hindi ito audio call
        if (isVideoCall) {
            localSurfaceView?.let {
                mainRepository.initLocalSurfaceView(it, isVideoCall)
            }
            remoteSurfaceView?.let {
                mainRepository.initRemoteSurfaceView(it)
            }
        } else {
            mainRepository.initLocalAudioOnly()
        }
        if (!isCaller){
            Log.d(TAG, "Starting call as callee (answering)")
            mainRepository.startCall()
        }

    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationBuilder = naviSightNotificationManager.buildWebrtcServiceStartNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                startForeground(1, notificationBuilder.build(), fgsType)
            } else {
                startForeground(1, notificationBuilder.build())
            }
            Log.d(TAG, "Notification should had popped up.")
        }
    }

    // Set to public so this will be accessible the second the callee does accept the call
    fun startCallTimeoutTimer() {
        stopCallTimeoutTimer() // Ensure any previous timer is stopped
        Log.d(TAG, "Starting 30-second call timeout timer.")
        // Post the runnable to execute after 30 seconds
        callTimeoutHandler.postDelayed(callTimeoutRunnable, CALL_TIMEOUT_MS)
    }

    // Set to public so this will be accessible the second the callee does accept the call
    fun stopCallTimeoutTimer() {
        Log.d(TAG, "Stopping call timeout timer.")
        // Remove the delayed runnable from the handler's queue
        callTimeoutHandler.removeCallbacks(callTimeoutRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onLatestEventReceived(data: DataModel) {
        Log.d(TAG, "MainService - OnLatestEventReceived found something: ${data.type}")
        when (data.type) {
            DataModelType.StartVideoCall,
            DataModelType.StartAudioCall -> {
                val callerId = data.sender
                if (callerId.isNullOrEmpty()) {
                    Log.e(TAG, "Incoming call event missing valid sender ID.")
                    return // Skip processing if sender is missing
                }
                currentIncomingCallerId = callerId
                startCallTimeoutTimer() // Begin timer for time out
                listener?.onCallReceived(data)
            }
            DataModelType.EmergencyStartAudioCall,
            DataModelType.EmergencyStartVideoCall -> {
                val callerId = data.sender
                if (callerId.isNullOrEmpty()) {
                    Log.e(TAG, "Incoming call event missing valid sender ID.")
                    return // Skip processing if sender is missing
                }
                currentIncomingCallerId = callerId
                listener?.onEmergencyCallReceived(data)
            }
            else -> Unit
        }
    }

    override fun endCall() {
        // Receive EndCall signal from remote peer
        Log.d(TAG, "Received EndCall signal from remote peer.")
        endCallCleanUp()
    }

    override fun deniedCall() {
        // Receive DeniedCall signal from remote peer
        Log.d(TAG, "Received DeniedCall signal from remote peer.")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent(BR_ACTION_DENIED_CALL))
        endCallCleanUp() // Clean up after receiving denial signal
    }

    interface Listener {
        fun onCallReceived(model: DataModel)
        fun onEmergencyCallReceived(model: DataModel)
        fun onCallAborted()
        fun onCallMissed(senderId: String)
    }

    override fun onConnectionFailure() {
        LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(Intent(BR_CONNECTION_FAILURE))
        connectionFailureHandler.postDelayed(connectionFailureRunnable, 20000L) // 20 secs.
    }

    override fun onAbortCallConnectionBased(targetId: String) {
        Log.d(TAG, "WebRTC connection failed/disconnected. Aborting call: $targetId")
        if (listener != null) {
            listener?.onCallAborted() // Notify UI/Activity
        }

        mainRepository.sendAbortCall() // Signal the other side about the abort
        endCallCleanUp() // Full cleanup
    }

    override fun onMissCall(targetId: String) {
        stopCallTimeoutTimer()
        Log.d(TAG, "Missed call logic executed for: $targetId")
        if (listener != null) {
            listener?.onCallMissed(targetId)
        }
        mainRepository.sendMissCall() // Sends MissCall signal to remote peer (if this was the timer running)
        endCallCleanUp()
    }

    override fun missCall() {
        Log.d(TAG, "Received MissCall signal from remote peer.")
        val currentUserType = mainRepository.getUserType()
        if (currentUserType == USER_TYPE_VIU) {
            showToastOnMainThreadAndTTS("Caregiver missed your call. Try again?")
        } else Toast.makeText(
            applicationContext,
            "VIU missed your call. Try again?",
            Toast.LENGTH_LONG).show()
        endCallCleanUp()
    }

    override fun abortCall() {
        Log.d(TAG, "Received AbortCall signal from remote peer.")
        handleAbortCall()
        endCallCleanUp()
    }

    interface EndAndDeniedCallListener {
        fun onCallEnded()
        fun onCallDenied()
    }

    override fun onConnectionEstablished() {
        Log.d(TAG, "WebRTC connection established. Notifying CallActivity.")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent(BR_CONNECTION_ESTABLISHED))
    }
}
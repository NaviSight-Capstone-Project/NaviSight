package edu.capstone.navisight.common.webrtc.service

import android.app.NotificationChannel
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
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import edu.capstone.navisight.R
import edu.capstone.navisight.MainActivity
import edu.capstone.navisight.common.TTSHelper
import edu.capstone.navisight.common.webrtc.vendor.RTCAudioManager
import edu.capstone.navisight.common.webrtc.service.MainServiceActions.*
import edu.capstone.navisight.common.webrtc.model.DataModel
import edu.capstone.navisight.common.webrtc.model.DataModelType
import edu.capstone.navisight.common.webrtc.model.isValid
import edu.capstone.navisight.common.webrtc.repository.MainRepository
import org.webrtc.SurfaceViewRenderer


class MainService : Service(), MainRepository.Listener {
    private val TAG = "MainServiceCountdown"
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

    private fun showToastOnMainThreadAndTTS(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
        TTSHelper.queueSpeak(applicationContext, message)
    }

        // This Runnable defines the action to take when the 30 seconds expire
    private val callTimeoutRunnable = Runnable {
        Log.w("calltimeout", "callTimeoutRunnable is now working")
        currentIncomingCallerId?.let { targetId ->
            Log.w("calltimeout", "Call to $targetId timed out after 30 seconds. Aborting.")
            // Call the existing abort function to dismiss the UI/popup
            onMissCall(targetId)
        }
        val intent2 = Intent("TARGET_MISSED_YOUR_CALL")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent2)
        currentIncomingCallerId = null
    }

    companion object {
        private const val TAG = "MainServiceCountdown"

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
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("CallSignal", "making mainservice")
        INSTANCE = this
        mainRepository = MainRepository.getInstance(applicationContext)
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CallSignal", "onStartCommand triggered")
        intent?.let { incomingIntent ->
            // Exception to this is missed calls.
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

    // Handle denied calls
    private fun handleDeniedCall() {
        mainRepository.sendEndCall()
        endCallAndRestartRepository()
    }

    private fun handleEndOrAbortCall() {
        Log.d("abortcall", "handling end or abort call")
        mainRepository.sendEndOrAbortCall()
        endCallAndRestartRepository()
    }

    // Handle abort calls
    private fun handleAbortCall() {
        Log.d("abortcall", "call aborted detected")
        listener?.onCallAborted()
    }

    private fun handleEndCall() {
        mainRepository.sendEndCall()
        endCallAndRestartRepository()
    }

    private fun handleStartService(incomingIntent: Intent) {
        //start our foreground service
        if (!isServiceRunning) {
            isServiceRunning = true
            email = incomingIntent.getStringExtra("email")
            startServiceWithNotification()

            //setup my clients
            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(email!!)

        }
    }

    private fun handleStopService() {
        Log.d("logoff", "Activating handleStopService()")
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
                mainRepository.toggleVideo(true)
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
                mainRepository.toggleVideo(false)
            }
        }
    }

    // Update the foreground service type
    private fun updateForegroundServiceType(newType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = notificationManager.activeNotifications
                .firstOrNull { it.id == 1 }?.notification
                ?: NotificationCompat.Builder(this, "channel1")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Navisight Viu")
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

    private fun endCallAndRestartRepository(){
        mainRepository.endCall()
        endAndDeniedCallListener?.onCallEnded()
        mainRepository.initWebrtcClient(email!!)
    }

    private fun deniedCallAndRestartRepository(){
        Log.d("deniedcall", " got denied call from mainservice.kt")
        mainRepository.endCall()
        endAndDeniedCallListener?.onCallEnded()
        mainRepository.initWebrtcClient(email!!)
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        Log.d("CallCheck", "Handlesetupviews triggered!")

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
            Log.d("CallCheck",
                "Audio Call: Skipping SurfaceView init., audio calls don't need this.")
        }
        if (!isCaller){
            Log.d("CallCheck", "Starting video call")
            mainRepository.startCall()
        }

    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
            )

            val notification = NotificationCompat.Builder(
                this, "channel1"
            ).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Welcome to NaviSight")
                .setContentText("NaviSight successfully booted! Live calling is now active")

            notificationManager.createNotificationChannel(notificationChannel)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                // Compliance for Android 12+ security :D
                val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

                startForeground(1, notification.build(), fgsType)
            } else {
                // For pre-API 34 versions (this is the old, two-argument call)
                startForeground(1, notification.build())
            }

            Log.d("ServiceCheck", "Notification should had popped up.")
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
        if (data.isValid()) {
            Log.d("CallSignal", "The data type found valid is: ${data.type}")
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
                else -> Unit
            }
        }
    }

    override fun endCall() {
        // Receive end call signal from remote peer
        endCallAndRestartRepository()
    }

    override fun deniedCall() {
        MainActivity.firstTimeLaunched.let {
            if (!it) {
                showToastOnMainThreadAndTTS("Your caregiver denied your call. Try again?")
                deniedCallAndRestartRepository()
            } else {
            }
        }
    }

    interface Listener {
        fun onCallReceived(model: DataModel)
        fun onCallAborted()
        fun onCallMissed(senderId: String)
    }

    override fun onAbortCallConnectionBased(targetId: String) {
        if (listener != null) {
            listener?.onCallAborted()
        }
        mainRepository.sendAbortCall()
    }

    override fun onMissCall(targetId: String) {
        stopCallTimeoutTimer()
        if (listener != null) {
            listener?.onCallMissed(targetId)
        }
        Log.d("misscall", "countdown expired. sending misscall status to caregiver")
        mainRepository.sendMissCall()
    }

    override fun missCall() {
        val currentUserType = mainRepository.getUserType()
        MainActivity.firstTimeLaunched.let {
            if (!it) {
                val textToSay =
                    if (currentUserType == "viu") "Caregiver missed your call. Try again?"
                    else "VIU missed your call. Try again?"
                showToastOnMainThreadAndTTS(textToSay)
                endCallAndRestartRepository()
            }
        }
    }

    override fun abortCall() {
        handleAbortCall()
    }

    interface EndAndDeniedCallListener {
        fun onCallEnded()
        fun onCallDenied()
    }
}
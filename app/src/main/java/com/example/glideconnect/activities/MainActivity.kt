package com.example.glideconnect.activities

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.glideconnect.R
import com.example.glideconnect.databinding.ActivityMainBinding
import com.example.glideconnect.media.RtcTokenBuilder2
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var localSurfaceView: SurfaceView
    private lateinit var remoteSurfaceView: SurfaceView
    private val PERMISSION_REQ_ID = 1
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private var channelName: String? = null

    private val appId = "978fbceeb2424cbb83868139c0ed0ee4"
    private val uid = 0
    private var isJoined = false
    private val appCertificate = "2a85bc87322447ca8adb8e112ebea4a1"

    private var isVideoEnabled = false
    private var isAudioEnabled = false

    private var agoraEngine: RtcEngine? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        val intent = intent
        channelName = intent.getStringExtra("CHANNEL NAME")

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }

        setupVideoSDKEngine()

        mainBinding.joinButton.setOnClickListener {
            joinChannel()
        }

        mainBinding.endCallButton.setOnClickListener {
            leaveChannel()
        }

        mainBinding.switchCameraButton.setOnClickListener {
            switchCamera()
        }

        mainBinding.videoButton.setOnClickListener {
            toggleVideo()
        }

        mainBinding.microphoneButton.setOnClickListener {
            toggleAudio()
        }

        val tokenBuilder = RtcTokenBuilder2()
        val timeStamp = (System.currentTimeMillis() / 1000 + 60).toInt()

        token = tokenBuilder.buildTokenWithUid(
            appId,
            appCertificate,
            channelName,
            uid,
            RtcTokenBuilder2.Role.ROLE_PUBLISHER,
            timeStamp,
            timeStamp
        )
    }

    private fun toggleAudio() {
        isAudioEnabled = !isAudioEnabled
        agoraEngine?.muteLocalAudioStream(isAudioEnabled)

        updateMicrophoneUi()

    }

    private fun updateMicrophoneUi() {
        val iconResId = if (isAudioEnabled) {
            R.drawable.ic_microphone_off
        } else {
            R.drawable.ic_microphone_on
        }
        mainBinding.microphoneButton.setImageResource(iconResId)
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        agoraEngine?.enableLocalVideo(isVideoEnabled)

        upDateVideoButtonUi()
    }

    private fun upDateVideoButtonUi() {
        val iconResId = if (isVideoEnabled) {
            R.drawable.ic_video_on
        } else {
            R.drawable.ic_video_off
        }
        mainBinding.videoButton.setImageResource(iconResId)
    }

    private fun switchCamera() {
        agoraEngine?.switchCamera()
    }

    //    A channel Will Be joined after clicking join button
    private fun joinChannel() {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()

            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView.visibility = View.VISIBLE
            agoraEngine?.startPreview()
            agoraEngine?.joinChannel(token, channelName, uid, options)
        } else {
            Toast.makeText(
                applicationContext,
                "Permissions were not granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //    To SetUp Video into Remote surface view
    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = mainBinding.remoteVideoView
        remoteSurfaceView.setZOrderMediaOverlay(true)

        agoraEngine?.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView.visibility = View.VISIBLE
    }

    //    To SetUp Video into our local surface view
    private fun setupLocalVideo() {
        localSurfaceView = mainBinding.localVideoView
        localSurfaceView.setZOrderMediaOverlay(true)
        agoraEngine?.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    //TO Leave the Channel Once End call is pressed
    private fun leaveChannel() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine?.leaveChannel()
            remoteSurfaceView.visibility = View.GONE
            localSurfaceView.visibility = View.GONE
            isJoined = false
            finish()
        }
    }

    // TO add All configuration and enable agora Engine to enable video
    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine?.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    // To Handle Various Events
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurfaceView.visibility = View.GONE }
        }
    }

    private fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }
}
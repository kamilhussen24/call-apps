package com.kamildex.kdexcall.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kamildex.kdexcall.databinding.ActivityCallBinding
import com.kamildex.kdexcall.sip.SipManager
import com.kamildex.kdexcall.utils.Prefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var isMuted = false
    private var isSpeaker = false
    private var isOnHold = false
    private var isRecording = false
    private var isDialpadVisible = false
    private var callDuration = 0
    private val handler = Handler(Looper.getMainLooper())
    private var recordingPath: String? = null

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (i?.action == "com.kamildex.kdexcall.CALL_ENDED") {
                finish()
            }
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            callDuration++
            val minutes = callDuration / 60
            val seconds = callDuration % 60
            binding.tvDuration.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val number = intent.getStringExtra("number") ?: 
            SipManager.getCurrentCall()?.remoteAddress?.username ?: "Unknown"
        binding.tvNumber.text = number

        handler.post(timerRunnable)
        setupListeners()

        // Auto-start recording if enabled
        if (Prefs.isRecordingEnabled(this)) startRecording()
    }

    private fun setupListeners() {
        binding.btnMute.setOnClickListener {
            isMuted = SipManager.toggleMute()
            binding.btnMute.alpha = if (isMuted) 0.5f else 1f
            binding.tvMuteLabel.text = if (isMuted) "Unmute" else "Mute"
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeaker = SipManager.toggleSpeaker(this)
            binding.btnSpeaker.alpha = if (isSpeaker) 1f else 0.5f
            binding.tvSpeakerLabel.text = if (isSpeaker) "Speaker On" else "Speaker"
        }

        binding.btnHold.setOnClickListener {
            if (isOnHold) { SipManager.resumeCall(); isOnHold = false }
            else { SipManager.holdCall(); isOnHold = true }
            binding.btnHold.alpha = if (isOnHold) 0.5f else 1f
            binding.tvHoldLabel.text = if (isOnHold) "Resume" else "Hold"
        }

        binding.btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else startRecording()
        }

        binding.btnDialpad.setOnClickListener {
            isDialpadVisible = !isDialpadVisible
            binding.dialpadSection.visibility = if (isDialpadVisible) View.VISIBLE else View.GONE
        }

        binding.btnEndCall.setOnClickListener {
            if (isRecording) stopRecording()
            SipManager.endCall()
            finish()
        }

        // Dialpad buttons
        val dtmfMap = mapOf(
            binding.dtmf0 to '0', binding.dtmf1 to '1', binding.dtmf2 to '2',
            binding.dtmf3 to '3', binding.dtmf4 to '4', binding.dtmf5 to '5',
            binding.dtmf6 to '6', binding.dtmf7 to '7', binding.dtmf8 to '8',
            binding.dtmf9 to '9', binding.dtmfStar to '*', binding.dtmfHash to '#'
        )
        dtmfMap.forEach { (btn, digit) ->
            btn.setOnClickListener {
                SipManager.sendDtmf(digit)
                binding.tvDtmfInput.append(digit.toString())
            }
        }
    }

    private fun startRecording() {
        val dir = getExternalFilesDir("recordings") ?: filesDir
        val fileName = "call_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.wav"
        recordingPath = File(dir, fileName).absolutePath
        recordingPath?.let { SipManager.startRecording(it) }
        isRecording = true
        binding.btnRecord.alpha = 1f
        binding.tvRecordLabel.text = "Stop Rec"
        binding.recordingIndicator.visibility = View.VISIBLE
    }

    private fun stopRecording() {
        SipManager.stopRecording()
        isRecording = false
        binding.btnRecord.alpha = 0.5f
        binding.tvRecordLabel.text = "Record"
        binding.recordingIndicator.visibility = View.GONE
    }

    private fun safeRegister(receiver: BroadcastReceiver, filter: IntentFilter) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else registerReceiver(receiver, filter)
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        safeRegister(callReceiver, IntentFilter("com.kamildex.kdexcall.CALL_ENDED"))
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(callReceiver) } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    override fun onBackPressed() { /* Prevent back during call */ }
}
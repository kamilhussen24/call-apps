package com.kamildex.kdexcall.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kamildex.kdexcall.databinding.ActivityIncomingCallBinding
import com.kamildex.kdexcall.sip.SipManager

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                "com.kamildex.kdexcall.CALL_ENDED",
                "com.kamildex.kdexcall.CALL_FAILED" -> finish()
                "com.kamildex.kdexcall.CALL_CONNECTED" -> {
                    startActivity(Intent(this@IncomingCallActivity, CallActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val number = intent.getStringExtra("caller_number") ?: "Unknown"
        binding.tvCallerNumber.text = number
        binding.tvCallerName.text = "Incoming Call"

        binding.btnAnswer.setOnClickListener {
            SipManager.acceptCall()
        }

        binding.btnDecline.setOnClickListener {
            SipManager.declineCall()
            finish()
        }
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
        safeRegister(callReceiver, IntentFilter().apply {
            addAction("com.kamildex.kdexcall.CALL_ENDED")
            addAction("com.kamildex.kdexcall.CALL_FAILED")
            addAction("com.kamildex.kdexcall.CALL_CONNECTED")
        })
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(callReceiver) } catch (e: Exception) {}
    }

    override fun onBackPressed() { /* Prevent back on incoming call */ }
}
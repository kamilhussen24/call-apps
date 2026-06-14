package com.kamildex.kdexcall.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kamildex.kdexcall.databinding.ActivitySetupBinding
import com.kamildex.kdexcall.sip.SipService
import com.kamildex.kdexcall.utils.Prefs

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var isConnecting = false

    private val sipReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                "com.kamildex.kdexcall.SIP_REGISTERED" -> {
                    isConnecting = false
                    setLoading(false)
                    startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                    finish()
                }
                "com.kamildex.kdexcall.SIP_FAILED" -> {
                    isConnecting = false
                    setLoading(false)
                    val reason = i.getStringExtra("reason") ?: "Connection failed"
                    Snackbar.make(binding.root, "Failed: $reason", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already configured, go to main
        if (Prefs.isConfigured(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()

        // Request permissions on first launch
        requestRequiredPermissions()
    }

    private fun requestRequiredPermissions() {
        val perms = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
        // Storage permission for call recordings
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            perms.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)

        val missing = perms.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty())
            androidx.core.app.ActivityCompat.requestPermissions(
                this, missing.toTypedArray(), 200
            )
    }

    override fun onRequestPermissionsResult(
        rc: Int, perms: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(rc, perms, results)
        // Continue regardless — permissions can be granted later
    }

    private fun setupListeners() {
        binding.btnConnect.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val domain = binding.etDomain.text.toString().trim()
            val port = binding.etPort.text.toString().trim().ifEmpty { "5060" }
            val transport = when (binding.rgTransport.checkedRadioButtonId) {
                binding.rbTcp.id -> "TCP"
                binding.rbTls.id -> "TLS"
                else -> "UDP"
            }

            if (username.isEmpty() || password.isEmpty() || domain.isEmpty()) {
                Snackbar.make(binding.root, "Please fill all required fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.saveConfig(this, username, password, domain, port, transport)
            isConnecting = true
            setLoading(true)

            ContextCompat.startForegroundService(this, Intent(this, SipService::class.java))
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnConnect.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConnect.text = if (loading) "Connecting..." else "Connect"
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
        safeRegister(sipReceiver, IntentFilter().apply {
            addAction("com.kamildex.kdexcall.SIP_REGISTERED")
            addAction("com.kamildex.kdexcall.SIP_FAILED")
        })
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(sipReceiver) } catch (e: Exception) {}
    }
}
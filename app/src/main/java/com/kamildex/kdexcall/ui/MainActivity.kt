package com.kamildex.kdexcall.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.kamildex.kdexcall.R
import com.kamildex.kdexcall.databinding.ActivityMainBinding
import com.kamildex.kdexcall.sip.SipService
import com.kamildex.kdexcall.ui.fragments.DialpadFragment
import com.kamildex.kdexcall.ui.fragments.RecentCallsFragment
import com.kamildex.kdexcall.ui.fragments.ContactsFragment
import com.kamildex.kdexcall.ui.fragments.RecordingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sipReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                "com.kamildex.kdexcall.SIP_REGISTERED" -> updateStatus(true)
                "com.kamildex.kdexcall.SIP_FAILED" -> updateStatus(false)
                "com.kamildex.kdexcall.INCOMING_CALL" -> {
                    startActivity(Intent(this@MainActivity, IncomingCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("caller_number", i.getStringExtra("number"))
                    })
                }
                "com.kamildex.kdexcall.CALL_CONNECTED" -> {
                    startActivity(Intent(this@MainActivity, CallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        requestPermissions()

        // Start service if not running
        ContextCompat.startForegroundService(this, Intent(this, SipService::class.java))
    }

    private fun setupTabs() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> DialpadFragment()
                1 -> RecentCallsFragment()
                2 -> ContactsFragment()
                else -> RecordingsFragment()
            }
        }

        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Dialpad"
                1 -> "Recents"
                2 -> "Contacts"
                else -> "Recordings"
            }
            tab.setIcon(when (position) {
                0 -> R.drawable.ic_dialpad
                1 -> R.drawable.ic_recent
                2 -> R.drawable.ic_contacts
                else -> R.drawable.ic_recording
            })
        }.attach()
    }

    private fun updateStatus(connected: Boolean) {
        binding.tvStatus.text = if (connected) "Connected" else "Disconnected"
        binding.statusDot.setBackgroundResource(
            if (connected) R.drawable.dot_green else R.drawable.dot_red
        )
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty())
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
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
            addAction("com.kamildex.kdexcall.INCOMING_CALL")
            addAction("com.kamildex.kdexcall.CALL_CONNECTED")
        })
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(sipReceiver) } catch (e: Exception) {}
    }
}
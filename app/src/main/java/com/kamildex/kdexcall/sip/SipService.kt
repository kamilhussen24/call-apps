package com.kamildex.kdexcall.sip

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kamildex.kdexcall.R
import com.kamildex.kdexcall.ui.IncomingCallActivity
import com.kamildex.kdexcall.ui.CallActivity
import com.kamildex.kdexcall.utils.CallLog
import com.kamildex.kdexcall.utils.Prefs
import org.linphone.core.Call

class SipService : Service() {

    companion object {
        const val CHANNEL_ID = "kdex_call_ch"
        const val NOTIF_ID = 2001
        const val INCOMING_NOTIF_ID = 2002
        var instance: SipService? = null
    }

    private var callStartTime = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannels()
        SipManager.init(this)
        SipManager.setListener(sipListener)
        startForeground(NOTIF_ID, buildIdleNotif())
        SipManager.register(this)
    }

    private val sipListener = object : SipManager.SipListener {
        override fun onRegistered() {
            updateNotif("Connected", "Ready to make and receive calls")
            sendBroadcast(Intent("com.kamildex.kdexcall.SIP_REGISTERED"))
        }
        override fun onRegistrationFailed(reason: String) {
            updateNotif("Connection Failed", reason)
            sendBroadcast(Intent("com.kamildex.kdexcall.SIP_FAILED").putExtra("reason", reason))
        }
        override fun onUnregistered() {
            updateNotif("Disconnected", "")
            sendBroadcast(Intent("com.kamildex.kdexcall.SIP_UNREGISTERED"))
        }
        override fun onIncomingCall(call: Call) {
            val number = call.remoteAddress?.username ?: "Unknown"
            showIncomingCallNotif(number)
            startActivity(Intent(this@SipService, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("caller_number", number)
            })
            sendBroadcast(Intent("com.kamildex.kdexcall.INCOMING_CALL").putExtra("number", number))
        }
        override fun onCallConnected(call: Call) {
            callStartTime = System.currentTimeMillis()
            cancelNotif(INCOMING_NOTIF_ID)
            startActivity(Intent(this@SipService, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            sendBroadcast(Intent("com.kamildex.kdexcall.CALL_CONNECTED"))
        }
        override fun onCallEnded(call: Call, duration: Int) {
            val number = call.remoteAddress?.username ?: "Unknown"
            val dir = if (call.dir == Call.Dir.Incoming) CallLog.CallDirection.INCOMING
                      else CallLog.CallDirection.OUTGOING
            CallLog.add(this@SipService, number, null, dir, duration)
            cancelNotif(INCOMING_NOTIF_ID)
            sendBroadcast(Intent("com.kamildex.kdexcall.CALL_ENDED").putExtra("duration", duration))
        }
        override fun onCallFailed(reason: String) {
            cancelNotif(INCOMING_NOTIF_ID)
            sendBroadcast(Intent("com.kamildex.kdexcall.CALL_FAILED").putExtra("reason", reason))
        }
    }

    private fun buildIdleNotif(): Notification {
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, com.kamildex.kdexcall.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KDex Call")
            .setContentText("Connecting...")
            .setSmallIcon(R.drawable.ic_call)
            .setContentIntent(openPi)
            .setOngoing(true).build()
    }

    private fun updateNotif(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, com.kamildex.kdexcall.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_call)
            .setContentIntent(openPi)
            .setOngoing(true).build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun showIncomingCallNotif(number: String) {
        val answerPi = PendingIntent.getActivity(this, 1,
            Intent(this, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("caller_number", number)
            }, PendingIntent.FLAG_IMMUTABLE)
        val declinePi = PendingIntent.getBroadcast(this, 2,
            Intent("com.kamildex.kdexcall.DECLINE_CALL"),
            PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, "kdex_incoming_ch")
            .setContentTitle("Incoming Call")
            .setContentText(number)
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(answerPi, true)
            .addAction(R.drawable.ic_call_end, "Decline", declinePi)
            .addAction(R.drawable.ic_call, "Answer", answerPi)
            .setAutoCancel(false).build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(INCOMING_NOTIF_ID, notif)
    }

    private fun cancelNotif(id: Int) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "KDex Call Service", NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) }
            )
            nm.createNotificationChannel(
                NotificationChannel("kdex_incoming_ch", "Incoming Calls", NotificationManager.IMPORTANCE_HIGH)
                    .apply { setShowBadge(true) }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        SipManager.destroy()
    }
}
package com.kamildex.kdexcall.sip

import android.content.Context
import android.util.Log
import com.kamildex.kdexcall.utils.Prefs
import org.linphone.core.*
import java.io.File

object SipManager {

    private const val TAG = "SipManager"
    private var core: Core? = null
    private var listener: SipListener? = null

    interface SipListener {
        fun onRegistered()
        fun onRegistrationFailed(reason: String)
        fun onUnregistered()
        fun onIncomingCall(call: Call)
        fun onCallConnected(call: Call)
        fun onCallEnded(call: Call, duration: Int)
        fun onCallFailed(reason: String)
    }

    fun setListener(l: SipListener) { listener = l }

    fun init(context: Context) {
        if (core != null) return
        try {
            val factory = Factory.instance()
            factory.setDebugMode(false, "KDexCall")

            val configDir = context.filesDir.absolutePath
            val dataDir = context.filesDir.absolutePath

            core = factory.createCore(null, null, context).apply {
                addListener(coreListener)
                start()
            }
            Log.d(TAG, "Core initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: ${e.message}")
        }
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core, account: Account,
            state: RegistrationState, message: String
        ) {
            Log.d(TAG, "Registration state: $state")
            when (state) {
                RegistrationState.Ok -> listener?.onRegistered()
                RegistrationState.Failed -> listener?.onRegistrationFailed(message)
                RegistrationState.Cleared -> listener?.onUnregistered()
                else -> {}
            }
        }

        override fun onCallStateChanged(
            core: Core, call: Call,
            state: Call.State, message: String
        ) {
            Log.d(TAG, "Call state: $state")
            when (state) {
                Call.State.IncomingReceived -> listener?.onIncomingCall(call)
                Call.State.Connected, Call.State.StreamsRunning -> listener?.onCallConnected(call)
                Call.State.End, Call.State.Released -> {
                    val duration = call.duration
                    listener?.onCallEnded(call, duration)
                }
                Call.State.Error -> listener?.onCallFailed(message)
                else -> {}
            }
        }
    }

    fun register(context: Context) {
        val core = core ?: return
        try {
            val username = Prefs.getSipUsername(context)
            val password = Prefs.getSipPassword(context)
            val domain = Prefs.getSipDomain(context)
            val port = Prefs.getSipPort(context)
            val transport = Prefs.getSipTransport(context)

            // Remove existing accounts
            core.accountList.forEach { core.removeAccount(it) }
            core.clearAllAuthInfo()

            val authInfo = Factory.instance().createAuthInfo(
                username, null, password, null, null, domain
            )
            core.addAuthInfo(authInfo)

            val accountParams = core.createAccountParams()
            val identity = Factory.instance().createAddress("sip:$username@$domain")
            accountParams.identityAddress = identity

            val serverAddr = Factory.instance().createAddress(
                "sip:$domain:$port;transport=${transport.lowercase()}"
            )
            accountParams.serverAddress = serverAddr
            accountParams.isRegisterEnabled = true
            accountParams.expires = 3600

            val account = core.createAccount(accountParams)
            core.addAccount(account)
            core.defaultAccount = account

        } catch (e: Exception) {
            listener?.onRegistrationFailed(e.message ?: "Registration error")
        }
    }

    fun call(number: String, domain: String): Boolean {
        return try {
            val address = core?.interpretUrl("sip:$number@$domain") ?: return false
            val params = core?.createCallParams(null) ?: return false
            params.isVideoEnabled = false
            core?.inviteAddressWithParams(address, params) != null
        } catch (e: Exception) { false }
    }

    fun acceptCall(): Boolean {
        return try {
            val call = core?.currentCall ?: return false
            val params = core?.createCallParams(call) ?: return false
            call.acceptWithParams(params) == 0
        } catch (e: Exception) { false }
    }

    fun declineCall() {
        try { core?.currentCall?.decline(Reason.Declined) } catch (e: Exception) {}
    }

    fun endCall() {
        try { core?.currentCall?.terminate() } catch (e: Exception) {}
    }

    fun toggleMute(): Boolean {
        val core = core ?: return false
        core.isMicEnabled = !core.isMicEnabled
        return !core.isMicEnabled
    }

    fun isMuted() = !(core?.isMicEnabled ?: true)

    fun toggleSpeaker(context: Context): Boolean {
        val core = core ?: return false
        val devices = core.audioDevices
        val currentOutput = core.outputAudioDevice
        val isSpeaker = currentOutput?.type == AudioDevice.Type.Speaker

        val target = if (isSpeaker) {
            devices.firstOrNull { it.type == AudioDevice.Type.Earpiece }
        } else {
            devices.firstOrNull { it.type == AudioDevice.Type.Speaker }
        }
        target?.let { core.outputAudioDevice = it }
        return !isSpeaker
    }

    fun isSpeakerOn(): Boolean =
        core?.outputAudioDevice?.type == AudioDevice.Type.Speaker

    fun holdCall() {
        try { core?.currentCall?.pause() } catch (e: Exception) {}
    }

    fun resumeCall() {
        try { core?.currentCall?.resume() } catch (e: Exception) {}
    }

    fun startRecording(filePath: String) {
        try {
            val params = core?.currentCall?.currentParams?.copy()
            params?.isRecording = true
            params?.recordFile = filePath
        } catch (e: Exception) {}
    }

    fun stopRecording() {
        try { core?.currentCall?.stopRecording() } catch (e: Exception) {}
    }

    fun sendDtmf(digit: Char) {
        try { core?.currentCall?.sendDtmf(digit) } catch (e: Exception) {}
    }

    fun getCurrentCall() = core?.currentCall

    fun unregister() {
        try {
            core?.defaultAccount?.let {
                val params = it.params.clone()
                params.isRegisterEnabled = false
                it.params = params
            }
        } catch (e: Exception) {}
    }

    fun destroy() {
        try { core?.stop(); core = null } catch (e: Exception) {}
    }
}
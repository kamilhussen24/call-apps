package com.kamildex.kdexcall.utils

import android.content.Context

object Prefs {
    private const val NAME = "kdex_call_prefs"
    private fun p(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getSipUsername(c: Context) = p(c).getString("sip_username", "") ?: ""
    fun getSipPassword(c: Context) = p(c).getString("sip_password", "") ?: ""
    fun getSipDomain(c: Context) = p(c).getString("sip_domain", "") ?: ""
    fun getSipPort(c: Context) = p(c).getString("sip_port", "5060") ?: "5060"
    fun getSipTransport(c: Context) = p(c).getString("sip_transport", "UDP") ?: "UDP"
    fun isConfigured(c: Context) = p(c).getBoolean("is_configured", false)
    fun isRecordingEnabled(c: Context) = p(c).getBoolean("recording_enabled", false)

    fun saveConfig(c: Context, username: String, password: String,
                   domain: String, port: String, transport: String) {
        p(c).edit().apply {
            putString("sip_username", username.trim())
            putString("sip_password", password.trim())
            putString("sip_domain", domain.trim())
            putString("sip_port", port.trim())
            putString("sip_transport", transport)
            putBoolean("is_configured", true)
            apply()
        }
    }

    fun setRecordingEnabled(c: Context, v: Boolean) =
        p(c).edit().putBoolean("recording_enabled", v).apply()

    fun clear(c: Context) = p(c).edit().clear().apply()
}
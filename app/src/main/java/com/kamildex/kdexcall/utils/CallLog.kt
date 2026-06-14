package com.kamildex.kdexcall.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class CallDirection { INCOMING, OUTGOING, MISSED }

data class CallEntry(
    val id: String,
    val number: String,
    val name: String?,
    val direction: CallDirection,
    val duration: Int,
    val time: String,
    val date: String,
    val timestamp: Long,
    val recordingPath: String? = null
)

object CallLog {
    private const val MAX = 100
    private fun p(c: Context) = c.getSharedPreferences("kdex_call_prefs", Context.MODE_PRIVATE)

    @Synchronized
    fun add(c: Context, number: String, name: String?, direction: CallDirection,
            duration: Int, recordingPath: String? = null) {
        val now = Date()
        val entry = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("number", number); put("name", name ?: "")
            put("direction", direction.name); put("duration", duration)
            put("time", SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now))
            put("date", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(now))
            put("timestamp", now.time); put("recording", recordingPath ?: "")
        }
        val arr = JSONArray().apply { put(entry) }
        getAll(c).take(MAX - 1).forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("number", e.number); put("name", e.name ?: "")
                put("direction", e.direction.name); put("duration", e.duration)
                put("time", e.time); put("date", e.date); put("timestamp", e.timestamp)
                put("recording", e.recordingPath ?: "")
            })
        }
        p(c).edit().putString("call_log", arr.toString()).apply()
    }

    fun getAll(c: Context): List<CallEntry> {
        return try {
            val arr = JSONArray(p(c).getString("call_log", "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                arr.getJSONObject(i).let { o ->
                    CallEntry(
                        id = o.getString("id"),
                        number = o.getString("number"),
                        name = o.getString("name").ifEmpty { null },
                        direction = CallDirection.valueOf(o.getString("direction")),
                        duration = o.getInt("duration"),
                        time = o.getString("time"),
                        date = o.getString("date"),
                        timestamp = o.getLong("timestamp"),
                        recordingPath = o.getString("recording").ifEmpty { null }
                    )
                }
            }
        } catch (e: Exception) { emptyList() }
    }

    fun clear(c: Context) = p(c).edit().remove("call_log").apply()

    fun formatDuration(seconds: Int): String {
        return if (seconds < 60) "${seconds}s"
        else "${seconds / 60}m ${seconds % 60}s"
    }
}
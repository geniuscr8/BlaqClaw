package com.blackclaw.android.security

import android.content.Context
import com.blackclaw.android.ClawApplication
import org.json.JSONArray
import org.json.JSONObject

/** Small bounded local audit trail for security actions; no package data leaves the device. */
object SecurityAuditLog {
    data class Entry(val action: String, val packageName: String, val outcome: String, val timestamp: Long)
    private const val PREFS = "security_audit"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 100

    @Synchronized
    fun record(action: String, packageName: String, outcome: String, context: Context = ClawApplication.instance) {
        val entries = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        entries.put(JSONObject().apply {
            put("action", action.take(40)); put("package", packageName.take(120))
            put("outcome", outcome.take(240)); put("timestamp", System.currentTimeMillis())
        })
        val trimmed = JSONArray()
        val start = (entries.length() - MAX_ENTRIES).coerceAtLeast(0)
        for (i in start until entries.length()) trimmed.put(entries.getJSONObject(i))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, trimmed.toString()).apply()
    }

    fun recent(context: Context = ClawApplication.instance): List<Entry> {
        val json = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        return (0 until json.length()).mapNotNull { index -> runCatching {
            val item = json.getJSONObject(index)
            Entry(item.optString("action"), item.optString("package"), item.optString("outcome"), item.optLong("timestamp"))
        }.getOrNull() }.asReversed()
    }

    fun clear(context: Context = ClawApplication.instance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}

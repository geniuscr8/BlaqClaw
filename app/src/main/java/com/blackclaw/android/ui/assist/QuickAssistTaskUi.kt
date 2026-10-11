package com.blackclaw.android.ui.assist

/** Friendly English labels for the transient Quick Assist status. */
object QuickAssistTaskReducer {
    fun toolLabel(tool: String): String = when {
        tool.contains("open_app_action") || tool == "open_app" -> "Opening app"
        tool.contains("play_music") -> "Playing music"
        tool.contains("send_message") || tool.contains("send_sms") -> "Sending message"
        tool.contains("make_call") -> "Starting call"
        tool.contains("appointment") || tool.contains("alarm") || tool.contains("reminder") || tool.contains("event") -> "Scheduling"
        tool.contains("web") || tool.contains("http") -> "Searching the web"
        tool.contains("screen") || tool.contains("ocr") -> "Analyzing screen"
        tool.contains("tap") || tool.contains("input") || tool.contains("swipe") || tool.contains("scroll") -> "Interacting with screen"
        tool.contains("notification") -> "Checking notifications"
        tool.contains("device_info") || tool.contains("battery") -> "Checking device"
        else -> tool.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }.ifBlank { "Running an action" }
    }
}

package com.blackclaw.android.assistant

/** Pure voice-input policy: English-first recognition and stable mic levels. */
internal object VoiceRecognitionPolicy {
    fun englishLocale(stored: String): String = when {
        stored.equals("en", true) -> "en-US"
        stored.startsWith("en-", true) -> stored
        else -> "en-US"
    }

    fun smoothRms(previous: Float, current: Float): Float =
        if (previous == 0f) current else (previous * 0.72f + current * 0.28f)
}

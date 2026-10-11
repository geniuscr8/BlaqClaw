package com.blackclaw.android.assistant

import java.util.Calendar

/**
 * JARVIS-style spoken personality for the voice assistant.
 *
 * Generates VARIED acknowledgements so BlaqClaw never sounds canned. Lines are
 * pooled by intent (wake ack vs command ack) and flavoured by time of day, with
 * the occasional dry-wit line — channeling the JARVIS-from-Iron-Man tone:
 * composed, courteous, quietly witty, always loyal ("jefe").
 *
 * Avoids repeating the immediately-previous line so it feels fresh.
 */
object JarvisVoice {

    @Volatile private var lastWake: String = ""
    @Volatile private var lastCmd: String = ""

    private fun hour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    private fun timeGreeting(): List<String> = when (hour()) {
        in 5..11 -> listOf(
            "Good morning.",
            "Good morning. Ready to start the day.",
            "Good morning. Shall we begin?",
            "Good morning. At your service.",
        )
        in 12..19 -> listOf(
            "Good afternoon.",
            "Good afternoon. How can I help?",
            "Good afternoon. At your service.",
        )
        else -> listOf(
            "Good evening.",
            "Good evening. Still awake and ready for you.",
            "At this hour and still on duty.",
        )
    }

    /** Pool of wake acknowledgements (said when the wake word comes with no command). */
    private val WAKE_BASE = listOf(
        "Go ahead.", "What do you need?", "At your service.", "I am here.",
        "Always ready for you.", "How can I help?", "I am listening.",
        "Your command.", "At your service.", "What is on your mind?",
        "Ready whenever you are.", "Of course.", "Certainly, go ahead.",
        "That is what I am here for.", "What happened?", "How are you?",
        "I am listening.", "Standing by.", "Shall we get to work?",
        "Present and ready.", "What can I do for you?", "You can count on me.",
        "Your assistant is here. Go ahead.", "Everything is under control. What do you need?",
        "Happy to help.",
    )

    /** Occasional dry-wit lines (used sparingly). */
    private val WAKE_WIT = listOf(
        "Another brilliant idea on the way?",
        "I hope this is not another alarm to silence… again.",
        "Your wishes are, literally, my instructions.",
        "I will try to contain my enthusiasm. Go ahead.",
        "Running at 100%, as usual. What shall we do?",
        "If I were human, the coffee would already be ready. Go ahead.",
    )

    /** Pool of command acknowledgements (said right before executing a task). */
    private val CMD_BASE = listOf(
        "Right away.", "On it.", "Immediately.", "I will handle it.",
        "Done.", "Of course.", "I will take care of it.", "Consider it done.",
        "Consider it resolved.", "I am on it.", "Working on it.", "As you wish.",
        "Immediately.", "On my way.", "Let us get to work.", "Understood.",
        "Processing your request.", "You can count on it.",
    )

    private val CMD_WIT = listOf(
        "Another impossible mission. Let us see what I can do.",
        "Easier than restarting the reactor. On it.",
        "A little digital magic… right away.",
        "I cannot promise miracles, but I will do something close.",
    )

    /** Acknowledgement when only the wake word was heard. */
    fun wakeAck(): String {
        // ~15% chance of a witty line, ~20% a time-aware greeting, else a base line.
        val roll = Math.random()
        val pool = when {
            roll < 0.15 -> WAKE_WIT
            roll < 0.35 -> timeGreeting()
            else -> WAKE_BASE
        }
        return pick(pool, lastWake).also { lastWake = it }
    }

    /** Acknowledgement said right before running a command. */
    fun commandAck(): String {
        val pool = if (Math.random() < 0.12) CMD_WIT else CMD_BASE
        return pick(pool, lastCmd).also { lastCmd = it }
    }

    private fun pick(pool: List<String>, avoid: String): String {
        if (pool.size <= 1) return pool.firstOrNull() ?: ""
        var choice = pool.random()
        var guard = 0
        while (choice == avoid && guard < 5) { choice = pool.random(); guard++ }
        return choice
    }
}

package com.blackclaw.android.conversation

import com.blackclaw.android.agent.TaskClassifier

/** Typed Conversation Engine decision used consistently by every surface. */
object ConversationRouter {
    enum class Mode { CONVERSE, READ, ACT }
    enum class Confirmation { NONE, REQUIRED }
    data class Decision(val mode: Mode, val confidence: Double, val confirmation: Confirmation,
                        val reason: String)

    private val readSignals = listOf(
        "read ", "show ", "check ", "what notifications", "battery", "calendar", "location", "clipboard",
    )
    private val destructive = listOf(
        "factory reset", "delete all", "uninstall", "send money", "transfer ", "pay ",
        "buy ", "purchase ", "publish ", "post ", "send ",
    )

    fun decide(text: String): Decision {
        val normalized = text.lowercase().trim()
        if (!TaskClassifier.isTask(text)) return Decision(Mode.CONVERSE, .92, Confirmation.NONE, "conversation")
        val mode = if (readSignals.any { it in normalized }) Mode.READ else Mode.ACT
        val confirmation = if (mode == Mode.ACT && destructive.any { it in normalized })
            Confirmation.REQUIRED else Confirmation.NONE
        val reason = when {
            confirmation == Confirmation.REQUIRED -> "This request may send, publish, purchase, or change something important."
            mode == Mode.READ -> "read-only request"
            else -> "requested action"
        }
        return Decision(mode, if (mode == Mode.READ) .86 else .88, confirmation, reason)
    }
}

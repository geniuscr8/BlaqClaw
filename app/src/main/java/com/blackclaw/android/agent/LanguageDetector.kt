package com.blackclaw.android.agent

/**
 * Simple language detector based on character/word patterns.
 * Used to ensure the agent responds in the same language the user writes in.
 *
 * Not a full NLP solution — just enough to tell English from English from
 * other Latin-script languages based on common word frequencies.
 */
object LanguageDetector {

    enum class Language(val code: String, val instruction: String) {
        ENGLISH("en", "Always respond in English."),
        UNKNOWN("", ""),
    }

    private val ENGLISH_MARKERS = setOf(
        "open", "send", "search", "set", "tell", "show", "close",
        "call", "write", "tomorrow", "today", "alarm", "reminder",
        "how", "what", "which", "where", "when", "why", "how much",
        "the", "a", "an", "is", "are", "was", "were", "have", "has",
        "for", "with", "that", "this", "from", "but", "not", "can",
        "will", "would", "could", "should", "also", "now", "then",
        "here", "there", "all", "nothing", "good", "bad", "yes", "no",
    )

    /**
     * Detect the most likely language of a user prompt.
     * Returns UNKNOWN if confidence is too low.
     */
    fun detect(text: String): Language {
        val words = text.lowercase().split(Regex("[\\s,.!?;:]+")).filter { it.length > 1 }
        if (words.size < 2) return Language.UNKNOWN

        var enScore = 0
        for (word in words) {
            if (word in ENGLISH_MARKERS) enScore++
        }

        return when {
            enScore >= 1 -> Language.ENGLISH
            else -> Language.UNKNOWN
        }
    }

    /**
     * Get a language instruction to inject into the system prompt.
     * Returns empty string if language is unclear (let the model decide).
     */
    fun getLanguageInstruction(text: String): String {
        val lang = detect(text)
        return if (lang != Language.UNKNOWN) "\n\n## Language\n${lang.instruction}\n" else ""
    }
}

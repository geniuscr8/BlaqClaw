package com.blackclaw.android.ui.assist

/** Pure language/presentation rules used by the phone-wide Quick Assist surface. */
internal object QuickAssistTextPolicy {
    fun isFarewell(command: String): Boolean {
        val text = command.lowercase().trim()
        val phrases = listOf(
            "gracias", "muchas gracias", "adiós", "adios", "hasta luego", "nos vemos",
            "eso es todo", "nada más", "nada mas", "ya está", "ya esta", "listo gracias",
            "chao", "bye", "thanks", "thank you", "that's all", "cállate", "callate", "ya no",
        )
        return text.split(' ').size <= 3 && phrases.any { text == it || text.startsWith(it) }
    }

    fun isGreeting(command: String): Boolean {
        val text = command.lowercase().trim().replace(Regex("[¿?!,.]"), "").trim()
        val greetings = listOf(
            "hola", "hey", "buenas", "buenos dias", "buenos días", "buenas tardes",
            "buenas noches", "que tal", "qué tal", "hello", "hi", "hey blaqclaw",
            "hola blaqclaw", "oye", "oye blaqclaw", "garra",
        )
        return text.split(' ').size <= 3 && greetings.any { text == it || text.startsWith(it) }
    }

    fun isScreenQuery(command: String): Boolean {
        val text = command.lowercase().trim()
        return text.contains("que hay en mi pantalla") || text.contains("qué hay en mi pantalla") ||
            text.contains("que ves en pantalla") || text.contains("qué ves en pantalla") ||
            text.contains("que estoy viendo") || text.contains("qué estoy viendo") ||
            text.contains("lee mi pantalla") || text.contains("leeme la pantalla") ||
            text.contains("que dice la pantalla") || text.contains("qué dice la pantalla") ||
            text.contains("what's on my screen") || text.contains("read my screen") ||
            text.contains("que app estoy usando") || text.contains("dónde estoy") ||
            text.contains("donde estoy")
    }

    fun friendlyError(raw: String): String {
        val error = raw.lowercase()
        return when {
            error.contains("no está instalada") || error.contains("not installed") || error.contains("no instalada") ->
                "That app is not installed. Should I install it from the Play Store?"
            error.contains("permission") || error.contains("permission") || error.contains("accesibilidad") ||
                error.contains("accessibility") -> "I need a permission for that. Check it in Settings."
            error.contains("not_found") || error.contains("no encontr") || error.contains("not found") ->
                "I could not find that element on screen. Should I try another way?"
            error.contains("network") || error.contains("timeout") || error.contains("conexión") || error.contains("conexion") ->
                "There was a connection problem."
            error.contains("rate") || error.contains("límite") || error.contains("limite") || error.contains("429") ->
                "The model is busy right now. Try again in a few seconds."
            error.contains("401") || error.contains("403") || error.contains("unauthor") ->
                "That model is no longer available; I switched to another free model. Try again."
            error.isBlank() -> "I could not complete that."
            else -> "I could not complete that: ${raw.take(120)}"
        }
    }

    fun recoveryFor(raw: String): QuickAssistRecovery? {
        val error = raw.lowercase()
        return when {
            error.contains("permission") || error.contains("permission") ||
                error.contains("accesibilidad") || error.contains("accessibility") -> QuickAssistRecovery.ACCESSIBILITY
            error.contains("network") || error.contains("timeout") || error.contains("conexión") ||
                error.contains("conexion") -> QuickAssistRecovery.CONNECTION
            error.isNotBlank() -> QuickAssistRecovery.RETRY
            else -> null
        }
    }

    /** Index just past the last sentence-ending punctuation after [from]. */
    fun sentenceBoundary(text: String, from: Int): Int {
        var last = -1
        var index = from.coerceIn(0, text.length)
        while (index < text.length) {
            val char = text[index]
            if (char == '.' || char == '!' || char == '?' || char == '\n' || char == '。') last = index + 1
            index++
        }
        return if (last - from >= 12) last else from
    }

    fun isAppLaunchTool(tool: String): Boolean {
        val name = tool.lowercase()
        return name.contains("open_app") || name.contains("open_url") || name.contains("play_music") ||
            name.contains("make_call") || name.contains("send_message") || name.contains("send_sms") ||
            name.contains("open_app_action")
    }

    fun stripReasoning(text: String): String {
        val reasoningPatterns = listOf(
            Regex("""(?i)^(respond[ií]|contest[eé]|ofrec[ií]|ayud[oé]|salud[oé]|pregunt[oé])\s+(al|a la|el|la)\s+usuario.*"""),
            Regex("""(?i)^(debo|voy a|necesito|tengo que|puedo|quiero)\s+(responder|contestar|ayudar|saludar|preguntar|decir).*"""),
            Regex("""(?i)^el usuario (quiere|pide|dice|necesita|solicita).*"""),
            Regex("""(?i)^(i should|i will|i need to|i can|the user wants|the user is).*"""),
            Regex("""(?i)^(let me|first,? i|now i|then i|ok,? so).*"""),
        )
        val cleaned = text.lines().filter { line ->
            val trimmed = line.trim()
            trimmed.isEmpty() || reasoningPatterns.none { it.containsMatchIn(trimmed) }
        }
        return cleaned.joinToString("\n").trim().ifBlank { text }
    }
}

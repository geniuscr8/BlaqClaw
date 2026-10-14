package com.blackclaw.android.agent

import java.util.Locale

/**
 * Keeps explicit task/todo requests inside BlaqClaw's native task surfaces.
 *
 * Small models often answer "use Todoist" when the user asks to create a task,
 * even though the app already has a native todo, scheduler, and automation
 * profile store. This guard is deliberately narrow: it only activates when the
 * user combines a creation verb with a task/todo noun, and it does not intercept
 * generic questions about tasks.
 */
internal class TaskCreationGuard private constructor(private val match: Match?) {

    data class Match(
        val taskText: String,
        val kind: Kind,
        val allowedTools: Set<String>,
        val requiredAction: String,
    )

    enum class Kind { TODO, SCHEDULED, AUTOMATION }

    private var attempted = false
    private var successful = false

    fun buildPromptSection(): String {
        val task = match ?: return ""
        val destination = when (task.kind) {
            Kind.TODO -> "assistant_note o kb_add_todo"
            Kind.SCHEDULED -> "schedule_task (or assistant_reminder if it should only notify)"
            Kind.AUTOMATION -> "automation_profile; use automation_rule only for a simple rule"
        }
        return """

        ## Task Creation Guard
        The user explicitly asked BlaqClaw to create something. Do NOT recommend
        external apps such as Todoist, Google Tasks, Tasker, or Notion, and do not
        answer with a tutorial. Create it in BlaqClaw using $destination.
        Request: "${task.taskText}"
        Required action: ${task.requiredAction}
        Available native tools: ${task.allowedTools.joinToString(", ")}.
        For automation_profile, a preview is expected before activation; explain
        the preview and ask for confirmation instead of claiming it was activated.
        """.trimIndent()
    }

    fun shouldBlockTextOnlyCompletion(responseText: String? = null): Boolean {
        if (match == null || successful) return false
        // A short, concrete clarification is valid when the user omitted the
        // only required detail (for example, the time for a scheduled task).
        // Recommendations/tutorials are never treated as clarification.
        if (responseText != null && isClarification(responseText)) return false
        return true
    }

    fun maybeBlockFinish(): String? {
        val task = match ?: return null
        if (successful) return null
        return "[System Guard] The user asked to create a ${task.kind.name.lowercase(Locale.ROOT)} in BlaqClaw. " +
            "Do not finish with an app recommendation. Use one of: ${task.allowedTools.joinToString(", ")}. " +
            task.requiredAction
    }

    fun recordToolAttempt(toolName: String) {
        if (match?.allowedTools?.contains(toolName) == true) attempted = true
    }

    fun recordToolResult(toolName: String, success: Boolean) {
        if (match?.allowedTools?.contains(toolName) == true) {
            attempted = true
            if (success) successful = true
        }
    }

    private fun isClarification(response: String): Boolean {
        if (attempted) return false
        val normalized = normalize(response)
        if (normalized.length > 260) return false
        if (listOf("todoist", "google tasks", "tasker", "notion", "trello", "app externa", "otra app")
                .any { normalized.contains(it) }) return false
        return (response.contains('?') || response.contains('¿')) &&
            listOf("hora", "fecha", "titulo", "texto", "contenido", "descripcion", "cuando", "que tarea")
                .any { normalized.contains(normalize(it)) }
    }

    companion object {
        fun fromTask(task: String): TaskCreationGuard {
            // Spanish command aliases are intentionally unsupported. This keeps
            // the native task guard aligned with the English-only task router.
            if (!InputLanguagePolicy.isEnglish(task)) return TaskCreationGuard(null)
            return TaskCreationGuard(parse(task))
        }

        private fun parse(task: String): Match? {
            val normalized = normalize(task)
            if (!hasTaskNoun(normalized) || !hasCreationVerb(normalized)) return null

            val automation = listOf(
                "when ", "if ", "cuando ", "si ", "al conect", "al entrar", "al salir", "cada vez",
                "automat", "tasker", "macro", "regla", "perfil", "if then", "si-entonces",
            ).any { normalized.contains(it) }
            if (automation) {
                return Match(
                    task.trim(),
                    Kind.AUTOMATION,
                    setOf("automation_profile", "automation_rule"),
                    "Create the profile/rule with the complete trigger and action; if a detail is missing, ask only for that detail.",
                )
            }

            val scheduled = listOf(
                " a las ", " a la ", " en ", " mañana", "manana", "hoy ", "cada ",
                "diario", "diaria", "semanal", "repet", "recordatorio", "programa",
                "schedule", "tomorrow", "remind",
            ).any { normalized.contains(it) }
            if (scheduled) {
                return Match(
                    task.trim(),
                    Kind.SCHEDULED,
                    setOf("schedule_task", "assistant_reminder"),
                    "Schedule the execution with the full text, date/time, and requested recurrence.",
                )
            }

            return Match(
                task.trim(),
                Kind.TODO,
                setOf("assistant_note", "kb_add_todo"),
                "Save the title and detail as a native BlaqClaw pending task.",
            )
        }

        private val TASK_NOUNS = Regex("\\b(tarea|tareas|todo|pendiente|pendientes|to[- ]do|task|tasks)\\b")
        private val CREATION_VERBS = Regex(
            "\\b(crea|crear|creame|agrega|agregar|anota|anotar|apunta|apuntar|guarda|guardar|" +
                "programa|programar|agenda|agendar|create|add|save|schedule|make)\\b"
        )

        private fun hasTaskNoun(text: String): Boolean = TASK_NOUNS.containsMatchIn(text)

        private fun hasCreationVerb(text: String): Boolean = CREATION_VERBS.containsMatchIn(text)

        private fun normalize(value: String): String {
            val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
            return value.lowercase(Locale.ROOT)
                .map { accents[it] ?: if (it.isLetterOrDigit() || it.isWhitespace() || it == '-' || it == '?') it else ' ' }
                .joinToString("")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}

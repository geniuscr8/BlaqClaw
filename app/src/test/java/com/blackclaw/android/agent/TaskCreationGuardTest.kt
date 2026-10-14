package com.blackclaw.android.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCreationGuardTest {

    @Test
    fun `native todo request cannot finish as an app recommendation`() {
        val guard = TaskCreationGuard.fromTask("create a task: buy milk")

        assertTrue(guard.shouldBlockTextOnlyCompletion("Puedes usar Todoist para eso."))
        assertTrue(guard.buildPromptSection().contains("assistant_note"))
        assertTrue(guard.maybeBlockFinish().orEmpty().contains("BlaqClaw"))

        guard.recordToolAttempt("assistant_note")
        guard.recordToolResult("assistant_note", success = true)
        assertFalse(guard.shouldBlockTextOnlyCompletion("Tarea guardada."))
        assertFalse(guard.maybeBlockFinish().orEmpty().isNotEmpty())
    }

    @Test
    fun `wifi event task selects automation path`() {
        val guard = TaskCreationGuard.fromTask("create a task when I connect to Wi-Fi")

        assertTrue(guard.buildPromptSection().contains("automation_profile"))
        assertTrue(guard.shouldBlockTextOnlyCompletion("Use Tasker or MacroDroid."))
        guard.recordToolResult("automation_profile", success = true)
        assertFalse(guard.shouldBlockTextOnlyCompletion("Preview created."))
    }

    @Test
    fun `recurring English trigger selects automation path`() {
        val guard = TaskCreationGuard.fromTask("create a task whenever I connect to Wi-Fi")

        assertTrue(guard.buildPromptSection().contains("automation_profile"))
    }

    @Test
    fun `generic task question is not guarded`() {
        val guard = TaskCreationGuard.fromTask("what is a scheduled task")

        assertFalse(guard.shouldBlockTextOnlyCompletion("Es una tarea que se ejecuta más tarde."))
        assertTrue(guard.buildPromptSection().isEmpty())
    }
}

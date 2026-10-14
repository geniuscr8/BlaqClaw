package com.blackclaw.android.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskContinuationDetectorTest {
    @Test
    fun `resumes a task that stopped at the iteration limit`() {
        val messages = listOf(
            ChatMessage(ChatMessage.Role.USER, "Add 30 fake contacts"),
            ChatMessage(ChatMessage.Role.ASSISTANT, "Error: maximum iteration limit reached (60)"),
        )

        assertTrue(TaskContinuationDetector.isContinuationRequest("continue", messages))
        assertTrue(TaskContinuationDetector.buildPrompt("continue").contains("current state"))
    }

    @Test
    fun `routes short replies after a tool task back to the agent`() {
        val messages = listOf(
            ChatMessage(ChatMessage.Role.USER, "Add fake contacts"),
            ChatMessage(ChatMessage.Role.SYSTEM, "Create Contacts..."),
            ChatMessage(ChatMessage.Role.TOOL_GROUP, "", toolSteps = listOf(
                ToolStep("Create Contacts", "FAILED — permission denied", success = false),
            )),
            ChatMessage(ChatMessage.Role.ASSISTANT, "The contacts could not be saved."),
        )

        assertTrue(TaskContinuationDetector.isContinuationRequest("continue", messages))
        assertTrue(TaskContinuationDetector.isContinuationRequest("yes", messages))
        assertTrue(TaskContinuationDetector.isContinuationRequest("okay", messages))
    }

    @Test
    fun `does not route acknowledgements after normal chat`() {
        val messages = listOf(
            ChatMessage(ChatMessage.Role.USER, "¿Qué significa esta palabra?"),
            ChatMessage(ChatMessage.Role.ASSISTANT, "Es una explicación normal."),
        )

        assertFalse(TaskContinuationDetector.isContinuationRequest("sí", messages))
        assertFalse(TaskContinuationDetector.isContinuationRequest("ok", messages))
        assertFalse(TaskContinuationDetector.isContinuationRequest("otros 30", messages))
    }

    @Test
    fun `does not hijack normal conversation`() {
        val messages = listOf(
            ChatMessage(ChatMessage.Role.USER, "¿Qué significa esta palabra?"),
            ChatMessage(ChatMessage.Role.ASSISTANT, "Es una explicación normal."),
        )

        assertFalse(TaskContinuationDetector.isContinuationRequest("continua", messages))
        assertFalse(TaskContinuationDetector.isContinuationRequest("continua mañana", emptyList()))
    }
}

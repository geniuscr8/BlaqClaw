package com.blackclaw.android.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIntentRegressionTest {

    @Test
    fun `notification mentions do not read device notifications`() {
        val contextualMentions = listOf(
            "what is a notification",
            "the word notification appears often",
            "let us discuss Android notifications",
            "my notifications are annoying",
            "summarize this article about notifications",
            "show me how to create notifications",
            "show notifications from this article",
            "I want to learn to read notifications from Kotlin",
        )

        contextualMentions.forEach { prompt ->
            assertNull(
                "Must not execute get_notifications for: $prompt",
                DirectDeviceDataGuard.deterministicToolCall(prompt),
            )
        }
    }

    @Test
    fun `bare and conceptual notification mentions stay in chat`() {
        listOf(
            "notification",
            "what is a notification",
            "the word notification appears often",
            "let us discuss Android notifications",
            "my notifications are annoying",
        ).forEach { prompt ->
            assertFalse("Expected CHAT: $prompt", TaskClassifier.isTask(prompt))
        }
    }

    @Test
    fun `explicit notification data requests still use the fast path`() {
        listOf(
            "read my notifications",
            "check my notifications",
            "what notifications do I have",
            "are there new notifications",
            "check my notifications",
            "show me current notifications",
            "how many notifications",
            "what notifications do i have",
            "show me my notifications",
            "I want to see my notifications",
            "show me my notifications",
        ).forEach { prompt ->
            val call = DirectDeviceDataGuard.deterministicToolCall(prompt)
            assertNotNull("Expected notification tool call for: $prompt", call)
            assertTrue(call?.toolName == "get_notifications")
            assertTrue("Expected TASK: $prompt", TaskClassifier.isTask(prompt))
        }
    }

    @Test
    fun `negated notification requests never execute the tool`() {
        listOf(
            "no leas mis notificaciones",
            "nunca revises mis notificaciones",
            "don't read my notifications",
            "do not check my notifications",
        ).forEach { prompt ->
            assertNull(
                "Must not execute a notification tool for negated request: $prompt",
                DirectDeviceDataGuard.deterministicToolCall(prompt),
            )
        }
    }
}

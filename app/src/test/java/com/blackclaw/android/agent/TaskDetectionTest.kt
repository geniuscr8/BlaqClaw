package com.blackclaw.android.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [DefaultAgentService.isTaskLike] — the router that decides whether a
 * message is an actionable task (→ agent loop with tools) or just chat.
 */
class TaskDetectionTest {

    @Test
    fun englishAlarmIsTask() {
        assertTrue(DefaultAgentService.isTaskLike("set an alarm at 7"))
        assertTrue(DefaultAgentService.isTaskLike("set an alarm for tomorrow"))
    }

    @Test
    fun englishReminderIsTask() {
        assertTrue(DefaultAgentService.isTaskLike("remind me to call the dentist"))
        assertTrue(DefaultAgentService.isTaskLike("remind me in 30 minutes"))
    }

    @Test
    fun englishMessageIsTask() {
        assertTrue(DefaultAgentService.isTaskLike("send a message to Mom"))
        assertTrue(DefaultAgentService.isTaskLike("send a WhatsApp message to John"))
    }

    @Test
    fun englishOpenAppIsTask() {
        assertTrue(DefaultAgentService.isTaskLike("open YouTube"))
        assertTrue(DefaultAgentService.isTaskLike("open the camera"))
    }

    @Test
    fun englishDeviceControlIsTask() {
        assertTrue(DefaultAgentService.isTaskLike("turn on the flashlight"))
        assertTrue(DefaultAgentService.isTaskLike("increase the volume"))
        assertTrue(DefaultAgentService.isTaskLike("turn off Wi-Fi"))
    }

    @Test
    fun englishNoteIsTask() {
        assertTrue(DefaultAgentService.isTaskLike("note that I have a meeting on Monday"))
        assertTrue(DefaultAgentService.isTaskLike("create a note"))
    }

    @Test
    fun englishStillWorks() {
        assertTrue(DefaultAgentService.isTaskLike("open whatsapp"))
        assertTrue(DefaultAgentService.isTaskLike("set an alarm at 7"))
        assertTrue(DefaultAgentService.isTaskLike("send a message to mom"))
    }

    @Test
    fun deviceNounsTrigger() {
        assertTrue(DefaultAgentService.isTaskLike("how much battery do I have"))
        assertTrue(DefaultAgentService.isTaskLike("read my notifications"))
    }

    @Test
    fun casualChatIsNotTask() {
        assertFalse(DefaultAgentService.isTaskLike("hola qué tal"))
        assertFalse(DefaultAgentService.isTaskLike("cuéntame un chiste"))
        assertFalse(DefaultAgentService.isTaskLike("gracias"))
    }

    @Test
    fun pureQuestionIsNotTask() {
        assertFalse(DefaultAgentService.isTaskLike("quién fue Einstein"))
        assertFalse(DefaultAgentService.isTaskLike("por qué el cielo es azul"))
    }
}

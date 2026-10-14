package com.blackclaw.android.ui.assist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAssistTextPolicyTest {
    @Test fun `farewells and greetings stay conversational`() {
        assertTrue(QuickAssistTextPolicy.isFarewell("thanks"))
        assertTrue(QuickAssistTextPolicy.isGreeting("Hello BlaqClaw!"))
        assertFalse(QuickAssistTextPolicy.isFarewell("thanks for checking tomorrow's weather"))
    }

    @Test fun `screen questions are recognized in English`() {
        assertTrue(QuickAssistTextPolicy.isScreenQuery("What is on my screen?"))
        assertTrue(QuickAssistTextPolicy.isScreenQuery("read my screen"))
        assertFalse(QuickAssistTextPolicy.isScreenQuery("tell me a joke"))
    }

    @Test fun `failure classification returns useful recovery`() {
        assertEquals(QuickAssistRecovery.ACCESSIBILITY, QuickAssistTextPolicy.recoveryFor("Accessibility permission is missing"))
        assertEquals(QuickAssistRecovery.CONNECTION, QuickAssistTextPolicy.recoveryFor("network timeout"))
        assertEquals(QuickAssistRecovery.RETRY, QuickAssistTextPolicy.recoveryFor("element not found"))
        assertEquals(null, QuickAssistTextPolicy.recoveryFor(""))
    }

    @Test fun `reasoning lines are removed without losing answer`() {
        val cleaned = QuickAssistTextPolicy.stripReasoning("I should answer the user briefly.\nReady, chief.")
        assertEquals("Ready, chief.", cleaned)
    }

    @Test fun `sentence boundary waits for a useful chunk`() {
        assertEquals(0, QuickAssistTextPolicy.sentenceBoundary("Hola.", 0))
        assertEquals(17, QuickAssistTextPolicy.sentenceBoundary("Esta frase sirve. Otra", 0))
    }

    @Test fun `app launch tools are detected`() {
        assertTrue(QuickAssistTextPolicy.isAppLaunchTool("open_app_action"))
        assertTrue(QuickAssistTextPolicy.isAppLaunchTool("send_message"))
        assertFalse(QuickAssistTextPolicy.isAppLaunchTool("get_device_info"))
    }
}

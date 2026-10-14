package com.blackclaw.android.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaskParserTest {
    @Test
    fun sendMessageCommandRoutesToDirectTool() {
        val parsed = TaskParser.parse("send hi to Girlfriend on WhatsApp")
        assertNotNull(parsed)
        assertEquals("send_message", parsed!!.action)
        assertEquals("hi", parsed.toolParams!!["message"])
        assertEquals("Girlfriend", parsed.toolParams!!["contact"])
        assertEquals("WhatsApp", parsed.toolParams!!["app"])
    }

    @Test
    fun contextualMessageFallsThroughToAgent() {
        assertNull(TaskParser.parse("send that to Girlfriend on WhatsApp"))
    }

    @Test
    fun emailDoesNotRouteToMessagingTool() {
        assertNull(TaskParser.parse("send email to user@example.com"))
    }

    @Test
    fun oldSpanishFastPathsAreNoLongerRecognized() {
        assertNull(TaskParser.parse("abre el autoclicker"))
        assertNull(TaskParser.parse("pon bad bunny"))
        assertNull(TaskParser.parse("llévame al walmart más cercano"))
    }
}

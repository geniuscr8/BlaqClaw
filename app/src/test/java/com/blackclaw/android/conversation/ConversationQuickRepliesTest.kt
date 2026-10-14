package com.blackclaw.android.conversation

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationQuickRepliesTest {
    @Test fun `answers broad automation capability immediately`() {
        val reply = ConversationQuickReplies.replyFor("Can you schedule tasks?")
        assertTrue(reply?.startsWith("Yes.") == true)
        assertTrue(ConversationQuickReplies.replyFor("Can you create automations?")?.startsWith("Yes.") == true)
    }

    @Test fun `does not intercept an actionable request`() {
        assertNull(ConversationQuickReplies.replyFor("Can you schedule a task for tomorrow at 9?"))
        assertNull(ConversationQuickReplies.replyFor("Can you turn off the flashlight?"))
        assertNull(ConversationQuickReplies.replyFor("Can you run ls?"))
    }

    @Test fun `answers general capability question`() {
        assertTrue(ConversationQuickReplies.replyFor("What can you do?")?.isNotBlank() == true)
        assertTrue(ConversationQuickReplies.replyFor("What can BlaqClaw do?")?.isNotBlank() == true)
    }
}

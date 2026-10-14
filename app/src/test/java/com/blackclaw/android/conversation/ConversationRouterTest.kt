package com.blackclaw.android.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationRouterTest {
    @Test fun `routes ordinary conversation without tools`() {
        assertEquals(ConversationRouter.Mode.CONVERSE, ConversationRouter.decide("What do you think of this idea?").mode)
    }

    @Test fun `routes device reads separately from actions`() {
        assertEquals(ConversationRouter.Mode.READ, ConversationRouter.decide("Read my notifications").mode)
        assertEquals(ConversationRouter.Mode.ACT, ConversationRouter.decide("Turn on the flashlight").mode)
    }

    @Test fun `destructive acts require confirmation`() {
        val decision = ConversationRouter.decide("Uninstall WhatsApp")
        assertEquals(ConversationRouter.Mode.ACT, decision.mode)
        assertEquals(ConversationRouter.Confirmation.REQUIRED, decision.confirmation)
    }

    @Test fun `destructive verb with destructive object requires confirmation`() {
        val decision = ConversationRouter.decide("Delete all data on the phone")
        assertEquals(ConversationRouter.Mode.ACT, decision.mode)
        assertEquals(ConversationRouter.Confirmation.REQUIRED, decision.confirmation)
    }

    @Test fun `send money requires confirmation`() {
        val decision = ConversationRouter.decide("Send money to Carlos")
        assertEquals(ConversationRouter.Confirmation.REQUIRED, decision.confirmation)
    }

    @Test fun `normal actions do not require confirmation`() {
        val decision = ConversationRouter.decide("Open Spotify")
        assertEquals(ConversationRouter.Mode.ACT, decision.mode)
        assertEquals(ConversationRouter.Confirmation.NONE, decision.confirmation)
    }

    @Test fun `battery query routes as READ`() {
        assertEquals(ConversationRouter.Mode.READ, ConversationRouter.decide("Check my battery").mode)
    }

    @Test fun `english commands route correctly`() {
        assertEquals(ConversationRouter.Mode.ACT, ConversationRouter.decide("Turn on the flashlight").mode)
        assertEquals(ConversationRouter.Mode.READ, ConversationRouter.decide("Check my battery").mode)
    }

    @Test fun `greeting is conversation not task`() {
        assertEquals(ConversationRouter.Mode.CONVERSE, ConversationRouter.decide("Hello, how are you?").mode)
    }

    @Test fun `capability question is conversation not an action`() {
        assertEquals(ConversationRouter.Mode.CONVERSE, ConversationRouter.decide("Can you schedule tasks?").mode)
        assertEquals(ConversationRouter.Mode.ACT, ConversationRouter.decide("Schedule a task for tomorrow at 9").mode)
    }

    @Test fun `explanations stay conversation while device reads remain actions`() {
        assertEquals(ConversationRouter.Mode.CONVERSE, ConversationRouter.decide("How can I schedule tasks?").mode)
        assertEquals(ConversationRouter.Mode.CONVERSE, ConversationRouter.decide("Why would I open WhatsApp?").mode)
        assertEquals(ConversationRouter.Mode.READ, ConversationRouter.decide("Check my battery").mode)
    }

    @Test fun `delete all requires confirmation`() {
        val decision = ConversationRouter.decide("Delete all data")
        assertEquals(ConversationRouter.Confirmation.REQUIRED, decision.confirmation)
    }
}

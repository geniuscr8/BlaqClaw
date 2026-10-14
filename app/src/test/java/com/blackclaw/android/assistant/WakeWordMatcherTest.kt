package com.blackclaw.android.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for fuzzy wake-word detection — the key to "Hey BlaqClaw" working
 * despite the speech recognizer mishearing a non-dictionary word.
 */
class WakeWordMatcherTest {

    @Test
    fun exactMatchExtractsCommand() {
        val m = WakeWordMatcher.match("blaqclaw send a message to Mom", "blaqclaw")
        assertNotNull(m)
        assertEquals("send a message to mom", m!!.command)
    }

    @Test
    fun spacedMishearingMatches() {
        // STT often hears "blaq claw" as two words
        val m = WakeWordMatcher.match("blaq claw set an alarm", "blaqclaw")
        assertNotNull(m)
        assertEquals("set an alarm", m!!.command)
    }

    @Test
    fun commonMishearingBlackLaw() {
        val m = WakeWordMatcher.match("black law open whatsapp", "blaqclaw")
        assertNotNull(m)
        assertEquals("open whatsapp", m!!.command)
    }

    @Test
    fun fuzzyMishearingBlaclo() {
        val m = WakeWordMatcher.match("blaclo enciende la luz", "blaqclaw")
        assertNotNull(m)
        assertEquals("enciende la luz", m!!.command)
    }

    @Test
    fun wakeWordOnlyGivesEmptyCommand() {
        val m = WakeWordMatcher.match("blaqclaw", "blaqclaw")
        assertNotNull(m)
        assertEquals("", m!!.command)
    }

    @Test
    fun absentWakeWordReturnsNull() {
        assertNull(WakeWordMatcher.match("hola qué tal el día", "blaqclaw"))
    }

    @Test
    fun commaAfterWakeWordIsStripped() {
        val m = WakeWordMatcher.match("blaqclaw, call John", "blaqclaw")
        assertNotNull(m)
        assertEquals("call john", m!!.command)
    }

    @Test
    fun customWakeWordWorks() {
        val m = WakeWordMatcher.match("jarvis apaga la tele", "jarvis")
        assertNotNull(m)
        assertEquals("apaga la tele", m!!.command)
    }

    @Test
    fun customWakeWordFuzzy() {
        // slight mishearing of a custom word
        val m = WakeWordMatcher.match("jarbis baja el volumen", "jarvis")
        assertNotNull(m)
        assertEquals("baja el volumen", m!!.command)
    }

    @Test
    fun levenshteinBasics() {
        assertEquals(0, WakeWordMatcher.levenshtein("abc", "abc"))
        assertEquals(1, WakeWordMatcher.levenshtein("abc", "abd"))
        assertEquals(3, WakeWordMatcher.levenshtein("abc", "xyz"))
    }

    @Test
    fun unrelatedWordNotFalseTriggered() {
        // "blanco" should NOT trigger "blaqclaw" (too different / different meaning)
        val m = WakeWordMatcher.match("el coche blanco es bonito", "blaqclaw")
        assertNull(m)
    }

}

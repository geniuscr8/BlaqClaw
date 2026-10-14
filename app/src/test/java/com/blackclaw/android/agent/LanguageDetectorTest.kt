package com.blackclaw.android.agent

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the pure language detection logic.
 */
class LanguageDetectorTest {

    @Test
    fun nonEnglishInputDoesNotEnableSpanishMode() {
        assertEquals(LanguageDetector.Language.UNKNOWN,
            LanguageDetector.detect("open whatsapp y manda un mensaje"))
    }

    @Test
    fun accentedInputDoesNotEnableSpanishMode() {
        assertEquals(LanguageDetector.Language.UNKNOWN,
            LanguageDetector.detect("qué día es"))
    }

    @Test
    fun detectsEnglishFromCommonWords() {
        assertEquals(LanguageDetector.Language.ENGLISH,
            LanguageDetector.detect("open whatsapp and send a message"))
    }

    @Test
    fun detectsEnglishQuestion() {
        assertEquals(LanguageDetector.Language.ENGLISH,
            LanguageDetector.detect("what is the weather today"))
    }

    @Test
    fun shortInputIsUnknown() {
        assertEquals(LanguageDetector.Language.UNKNOWN,
            LanguageDetector.detect("ok"))
    }

    @Test
    fun accentsDoNotChangeEnglishOnlyDetection() {
        val result = LanguageDetector.detect("qué día es")
        assertEquals(LanguageDetector.Language.UNKNOWN, result)
    }

    @Test
    fun nonEnglishInputDoesNotGetLanguageTrainingInstruction() {
        assertEquals("", LanguageDetector.getLanguageInstruction("qué día es"))
    }

    @Test
    fun englishInstructionContainsRespondInEnglish() {
        val instruction = LanguageDetector.getLanguageInstruction("open the camera and take a photo")
        assert(instruction.contains("English"))
    }

    @Test
    fun unknownLanguageGivesEmptyInstruction() {
        assertEquals("", LanguageDetector.getLanguageInstruction("ok"))
    }

    @Test
    fun englishCommandVerbs() {
        listOf(
            "search for cat videos",
            "send a message to Mom",
            "call John",
            "set an alarm",
        ).forEach {
            assertEquals("Failed for: $it", LanguageDetector.Language.ENGLISH,
                LanguageDetector.detect(it))
        }
    }
}

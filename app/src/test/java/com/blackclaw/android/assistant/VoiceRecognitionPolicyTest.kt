package com.blackclaw.android.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecognitionPolicyTest {
    @Test fun `recognition is English first`() {
        assertEquals("en-US", VoiceRecognitionPolicy.englishLocale("es-ES"))
        assertEquals("en-GB", VoiceRecognitionPolicy.englishLocale("en-GB"))
    }

    @Test fun `rms smoothing reduces sudden noise spikes`() {
        val smoothed = VoiceRecognitionPolicy.smoothRms(2f, 12f)
        assertTrue(smoothed in 2f..12f)
        assertTrue(smoothed < 12f)
    }
}

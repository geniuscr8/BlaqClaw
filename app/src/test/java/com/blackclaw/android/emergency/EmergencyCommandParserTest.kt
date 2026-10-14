package com.blackclaw.android.emergency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyCommandParserTest {
    @Test fun `both camera variants are reinforced`() {
        listOf(
            "activate discreet mode with both cameras",
            "start silent mode with both cameras",
            "enable discreet mode with front and back",
            "start emergency with both cameras",
        ).forEach { command ->
            assertEquals(command, EmergencyCameras.BOTH, EmergencyCommandParser.parse(command)?.cameras)
        }
    }

    @Test fun `discreet defaults to rear camera and stays silent`() {
        val parsed = EmergencyCommandParser.parse("activate discreet mode")!!
        assertEquals(EmergencyMode.DISCREET, parsed.mode)
        assertEquals(EmergencyCameras.BACK, parsed.cameras)
        assertTrue(parsed.silent)
        assertTrue(parsed.sendLocation)
    }

    @Test fun `location can be explicitly disabled`() {
        val parsed = EmergencyCommandParser.parse("activate discreet mode with front camera, no location")!!
        assertEquals(EmergencyCameras.FRONT, parsed.cameras)
        assertFalse(parsed.sendLocation)
    }

    @Test fun `ordinary conversation is not intercepted`() {
        assertNull(EmergencyCommandParser.parse("What does emergency mode mean?"))
    }
}

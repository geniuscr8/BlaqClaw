package com.blackclaw.android.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityConfirmationPolicyTest {
    @Test fun `destructive security actions require confirmation`() {
        assertTrue(SecurityConfirmationPolicy.requiresConfirmation("uninstall"))
        assertTrue(SecurityConfirmationPolicy.requiresConfirmation("neutralize"))
        assertFalse(SecurityConfirmationPolicy.requiresConfirmation("stop"))
        assertFalse(SecurityConfirmationPolicy.requiresConfirmation("settings"))
    }
}

package com.blackclaw.android.ui.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLaunchSetupTest {
    @Test
    fun firstLaunchOpensSetupEvenWhenPermissionsAreReady() {
        assertTrue(FirstLaunchSetup.shouldOpen(false, false, true))
    }

    @Test
    fun incompleteSetupReopensWhenAnEssentialPermissionIsMissing() {
        assertTrue(FirstLaunchSetup.shouldOpen(false, true, false))
    }

    @Test
    fun completedSetupGoesStraightToChat() {
        assertFalse(FirstLaunchSetup.shouldOpen(false, true, true))
    }

    @Test
    fun taskLaunchesBypassSetup() {
        assertFalse(FirstLaunchSetup.shouldOpen(true, false, false))
    }
}

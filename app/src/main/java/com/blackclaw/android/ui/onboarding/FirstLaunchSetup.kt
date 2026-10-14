package com.blackclaw.android.ui.onboarding

/**
 * Decides whether a normal app launch should open first-launch setup.
 * Task and deep-link launches must never be interrupted.
 */
internal object FirstLaunchSetup {
    fun shouldOpen(
        hasLaunchTask: Boolean,
        onboardingCompleted: Boolean,
        essentialPermissionsReady: Boolean,
    ): Boolean {
        if (hasLaunchTask) return false
        return !onboardingCompleted || !essentialPermissionsReady
    }
}

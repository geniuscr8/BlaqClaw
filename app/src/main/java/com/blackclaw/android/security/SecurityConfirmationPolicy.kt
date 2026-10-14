package com.blackclaw.android.security

/** Sensitive app actions require explicit confirmation in model tool calls. */
object SecurityConfirmationPolicy {
    private val sensitive = setOf("neutralize", "disable", "uninstall", "overlay_off", "overlay")
    fun requiresConfirmation(action: String): Boolean = action.lowercase() in sensitive
}

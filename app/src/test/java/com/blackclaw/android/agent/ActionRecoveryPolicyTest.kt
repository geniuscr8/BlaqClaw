package com.blackclaw.android.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionRecoveryPolicyTest {
    @Test
    fun failedUiActionsAreRecoverable() {
        assertTrue(ActionRecoveryPolicy.isRecoverable("tap_node"))
        assertTrue(ActionRecoveryPolicy.isRecoverable("open_app"))
        assertTrue(ActionRecoveryPolicy.isRecoverable("input_text"))
        assertFalse(ActionRecoveryPolicy.isRecoverable("get_screen_info"))
    }

    @Test
    fun tapHintRequiresFreshStateAndDifferentTarget() {
        val hint = ActionRecoveryPolicy.hint("tap", "node not found")
        assertTrue(hint.contains("get_screen_info"))
        assertTrue(hint.contains("different target"))
        assertTrue(hint.contains("node not found"))
    }

    @Test
    fun inputHintVerifiesFocus() {
        val hint = ActionRecoveryPolicy.hint("input_text", null)
        assertTrue(hint.contains("editable field"))
        assertTrue(hint.contains("node ID"))
    }
}

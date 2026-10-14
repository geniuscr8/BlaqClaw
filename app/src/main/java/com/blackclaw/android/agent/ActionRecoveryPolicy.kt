package com.blackclaw.android.agent

/** Deterministic recovery guidance for failed device actions. */
internal object ActionRecoveryPolicy {

    fun isRecoverable(toolName: String): Boolean = toolName in setOf(
        "phone_click_node", "phone_tap", "tap", "tap_node", "tap_ocr",
        "find_and_tap", "open_app", "open_url", "scroll_to_find", "swipe",
        "phone_swipe", "input_text", "type_text", "long_press", "phone_long_press"
    )

    fun hint(toolName: String, error: String?): String {
        val detail = error?.trim()?.takeIf { it.isNotEmpty() }?.let { " Error: $it" } ?: ""
        val guidance = when (toolName) {
            "tap", "tap_node", "tap_ocr", "find_and_tap", "phone_tap", "phone_click_node" ->
                "The last tap did not complete. Refresh the screen with get_screen_info and choose a different target or node ID; do not repeat the same coordinates blindly."
            "open_app", "open_url" ->
                "The launch failed. Verify the package name or URL and the current foreground app, then retry only with corrected parameters."
            "scroll_to_find", "swipe", "phone_swipe" ->
                "The navigation action did not produce the expected result. Refresh the screen and try a different direction, distance, or target."
            "input_text", "type_text" ->
                "The text may not have reached the field. Refresh the screen, verify that an editable field is focused, and retry once using its node ID if available."
            else ->
                "The last device action failed. Inspect the current screen and choose a different action instead of repeating it unchanged."
        }
        return "[System Recovery] $guidance$detail"
    }
}

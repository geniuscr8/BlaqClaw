package com.blackclaw.android.emergency

enum class EmergencyMode { EMERGENCY, DISCREET }
enum class EmergencyCameras { NONE, FRONT, BACK, BOTH }

data class EmergencyStartOptions(
    val mode: EmergencyMode,
    val cameras: EmergencyCameras,
    val sendLocation: Boolean,
    val recipient: String = "",
) {
    val silent: Boolean get() = mode == EmergencyMode.DISCREET
}

/** Deterministic English parser used before the LLM for safety commands. */
object EmergencyCommandParser {
    fun parse(command: String): EmergencyStartOptions? {
        val text = normalize(command)
        val isEmergency = Regex("\\bemergency\\b").containsMatchIn(text)
        val isDiscreet = Regex("\\b(discreet|silent)\\b").containsMatchIn(text)
        if (!isEmergency && !isDiscreet) return null
        if (!Regex("\\b(start|activate|enable)\\b").containsMatchIn(text)) return null

        val both = listOf(
            "front and back", "both cameras",
        ).any(text::contains)
        val front = Regex("\\b(front|selfie)\\b").containsMatchIn(text)
        val back = Regex("\\b(back|rear|main)\\b").containsMatchIn(text)
        val explicitlyNoCamera = Regex("\\b(no camera|audio only)\\b").containsMatchIn(text)
        val cameras = when {
            explicitlyNoCamera -> EmergencyCameras.NONE
            both || (front && back) -> EmergencyCameras.BOTH
            front -> EmergencyCameras.FRONT
            back -> EmergencyCameras.BACK
            isDiscreet -> EmergencyCameras.BACK
            else -> EmergencyCameras.BACK
        }
        val noLocation = Regex("\\b(no location|do not send location|do not share location)\\b").containsMatchIn(text)
        return EmergencyStartOptions(
            mode = if (isDiscreet) EmergencyMode.DISCREET else EmergencyMode.EMERGENCY,
            cameras = cameras,
            sendLocation = !noLocation,
        )
    }

    private fun normalize(value: String): String = java.text.Normalizer
        .normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

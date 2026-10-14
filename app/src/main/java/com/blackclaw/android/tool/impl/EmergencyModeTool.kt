package com.blackclaw.android.tool.impl

import com.blackclaw.android.ClawApplication
import com.blackclaw.android.emergency.EmergencyConfig
import com.blackclaw.android.emergency.EmergencyService
import com.blackclaw.android.emergency.EmergencyMode
import com.blackclaw.android.emergency.EmergencyCameras
import com.blackclaw.android.emergency.EmergencyStartOptions
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

class EmergencyModeTool : BaseTool() {
    override fun getName() = "emergency_mode"
    override fun getDisplayName() = "Emergency mode"
    override fun getDescriptionEN() =
        "Start, stop, or inspect emergency/discreet protection. Emergency sends location every five minutes and records evidence. Discreet starts silently with front, back, or both concurrent cameras when supported; Android privacy indicators and a neutral BlaqClaw foreground notification remain visible."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = listOf(
        ToolParameter("action", "string", "start | stop | status", true),
        ToolParameter("mode", "string", "emergency | discreet. Default emergency.", false),
        ToolParameter("cameras", "string", "none | front | back | both. 'Ambas cámaras' MUST map to both.", false),
        ToolParameter("send_location", "boolean", "Send initial and five-minute location updates. Default true.", false),
        ToolParameter("recipient", "string", "Optional phone override for this session; otherwise trusted contact.", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val action = requireString(params, "action").lowercase()
        val context = ClawApplication.instance
        return when (action) {
            "start", "activar", "iniciar" -> {
                if (!EmergencyConfig.isReady) {
                    ToolResult.error("First configure a trusted contact in Settings → Emergency mode.")
                } else {
                    val mode = when (optionalString(params, "mode", "emergency").lowercase()) {
                        "discreet", "discreto", "discreta" -> EmergencyMode.DISCREET
                        else -> EmergencyMode.EMERGENCY
                    }
                    val cameras = when (optionalString(params, "cameras", "back").lowercase()) {
                        "both", "ambas", "ambos", "two", "2" -> EmergencyCameras.BOTH
                        "front", "frontal", "selfie" -> EmergencyCameras.FRONT
                        "none", "ninguna", "audio" -> EmergencyCameras.NONE
                        else -> EmergencyCameras.BACK
                    }
                    val options = EmergencyStartOptions(mode, cameras,
                        optionalBoolean(params, "send_location", true), optionalString(params, "recipient", ""))
                    if (EmergencyService.start(context, options)) {
                        ToolResult.success(if (mode == EmergencyMode.DISCREET) "Discreet mode is active."
                            else "Emergency mode is starting. You have 5 seconds to cancel it from the notification.")
                    } else ToolResult.error("Could not start protection mode.")
                }
            }
            "stop", "detener", "cancelar" -> {
                EmergencyService.stop(context)
                ToolResult.success("Emergency mode stopped.")
            }
            "status", "estado" -> ToolResult.success(
                "Protection: ${if (EmergencyService.isActive) "active" else "inactive"}. " +
                    "Mode: ${EmergencyService.activeMode?.name?.lowercase() ?: "none"}. " +
                    "Requested cameras: ${EmergencyService.activeCameras.name.lowercase()}. " +
                    "Configuration: ${if (EmergencyConfig.isReady) "ready" else "incomplete"}."
            )
            else -> ToolResult.error("action must be start, stop, or status")
        }
    }
}

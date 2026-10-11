package com.blackclaw.android.tool.impl

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

/** Trigger a haptic / vibration pattern. */
class VibrateTool : BaseTool() {
    override fun getName() = "vibrate"
    override fun getDisplayName() = "Vibrar"
    override fun getDescriptionEN() =
        "Vibrate the device. pattern: 'short' (50ms), 'medium' (200ms), 'long' (500ms), " +
        "'double' or 'triple'. Or pass duration_ms for a custom length."
    override fun getDescriptionCN() = getDescriptionEN()

    override fun getParameters() = listOf(
        ToolParameter("pattern", "string", "short|medium|long|double|triple. Optional.", false),
        ToolParameter("duration_ms", "integer", "Override duration in ms (1..3000)", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val ctx = ClawApplication.instance
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return ToolResult.error("This device has no vibrator.")

        val customMs = optionalInt(params, "duration_ms", 0)
        val pattern = optionalString(params, "pattern", "medium").lowercase()

        return try {
            if (customMs in 1..3000) {
                vibrator.vibrate(VibrationEffect.createOneShot(customMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                ToolResult.success("Vibration ${customMs}ms")
            } else when (pattern) {
                "short" -> { vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)); ToolResult.success("Short vibration") }
                "medium", "" -> { vibrator.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE)); ToolResult.success("Medium vibration") }
                "long" -> { vibrator.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE)); ToolResult.success("Long vibration") }
                "double" -> { vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1)); ToolResult.success("Double vibration") }
                "triple" -> { vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80, 80, 80), -1)); ToolResult.success("Triple vibration") }
                else -> ToolResult.error("pattern must be short|medium|long|double|triple")
            }
        } catch (e: Exception) {
            ToolResult.error("Vibration failed: ${e.message}")
        }
    }
}

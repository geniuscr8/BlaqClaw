package com.blackclaw.android.tool.impl

import android.content.Intent
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.game.GameAutoclickerOverlay
import com.blackclaw.android.game.GameAutomationRuntime
import com.blackclaw.android.game.GameControlPolicy
import com.blackclaw.android.game.GameMacroStore
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

class GameRecordMacroTool : BaseTool() {
    override fun getName() = "game_record_macro"
    override fun getDisplayName() = "Grabar macro de juego"
    override fun getDescriptionEN() =
        "Open the visible accessibility autoclicker editor over a game. It records configured taps and " +
        "swipes without ADB or Shizuku and saves named macros from the floating panel."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "opens a floating editor to place and save taps/swipes without ADB"
    override fun getParameters() = listOf(
        ToolParameter("operation", "string", "open/start, close o status", true),
        ToolParameter("name", "string", "Nombre de la macro para start", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        return when (requireString(params, "operation").lowercase()) {
        "start", "open" -> {
            val packageName = GameScreenState.revealAndGetGamePackage()
                ?: return ToolResult.error("Could not identify the foreground game.")
            if (!GameScreenState.isGame(packageName)) return ToolResult.error("$packageName is not classified as a game.")
            GameAutoclickerOverlay.open(packageName).fold(
                onSuccess = { ToolResult.success("✓ Autoclicker opened over the game. Tap Record, place taps/swipes, enter a name, and tap Save.") },
                onFailure = { ToolResult.error(it.message ?: "Could not open the autoclicker.") },
            )
        }
        "close", "stop", "cancel" -> { GameAutoclickerOverlay.close(); ToolResult.success("Autoclicker closed.") }
        "status" -> ToolResult.success(if (GameAutoclickerOverlay.isOpen()) "The floating autoclicker is open." else "The floating autoclicker is closed.")
            else -> ToolResult.error("operation must be open, close, or status.")
        }
    }
}

class GameMacroTool : BaseTool() {
    override fun getName() = "game_macro"
    override fun getDisplayName() = "Macros de juego"
    override fun getDescriptionEN() =
        "List, play, pause, resume, restart, stop or delete named game macros. It can launch the macro's " +
        "game automatically and supports bounded loops up to 30 minutes."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "list, play, stop, or delete touch macros for games"
    override fun getParameters() = listOf(
        ToolParameter("operation", "string", "list, play, pause, resume, restart, stop, status, or delete", true),
        ToolParameter("name", "string", "Name for play/delete", false),
        ToolParameter("repeat", "integer", "Repetitions 1..100", false),
        ToolParameter("loop", "boolean", "Repeat in a loop until the time limit", false),
        ToolParameter("max_duration_minutes", "integer", "Maximum duration 1..30 minutes", false),
        ToolParameter("speed", "number", "Velocidad 0.25..3.0", false),
        ToolParameter("confirmed", "boolean", "Explicit confirmation for risky actions", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        return when (requireString(params, "operation").lowercase()) {
        "list" -> {
            val macros = GameMacroStore.list()
            if (macros.isEmpty()) ToolResult.success("No saved game macros.")
            else ToolResult.success(macros.joinToString("\n") { "• ${it.name} · ${it.gestures.size} gestures · ${it.packageName}" })
        }
        "play" -> {
            val name = requireString(params, "name")
            val macro = GameMacroStore.find(name) ?: return ToolResult.error("Macro '$name' not found.")
            val repeat = optionalInt(params, "repeat", 1).coerceIn(1, 100)
            val loop = optionalBoolean(params, "loop", false)
            val maxDurationMinutes = optionalInt(params, "max_duration_minutes", 10).coerceIn(1, 30)
            val speed = params["speed"]?.toString()?.toDoubleOrNull()?.coerceIn(0.25, 3.0) ?: 1.0
            if ((loop || repeat > 1 || GameControlPolicy.requiresConfirmation(macro.name)) &&
                !optionalBoolean(params, "confirmed", false)) {
                return ToolResult.error("The macro may repeat impactful actions. Ask for explicit confirmation.")
            }
            val foreground = GameScreenState.revealAndGetGamePackage()
            if (foreground != macro.packageName) {
                val launch = ClawApplication.instance.packageManager.getLaunchIntentForPackage(macro.packageName)
                    ?: return ToolResult.error("Could not find installed app ${macro.packageName}.")
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                ClawApplication.instance.startActivity(launch)
                Thread.sleep(1_200L)
            }
            GameAutomationRuntime.playMacro(macro, repeat, speed, loop, maxDurationMinutes * 60_000L).fold(
                onSuccess = { ToolResult.success("▶ Macro '${macro.name}' started ${if (loop) "in a loop (max ${maxDurationMinutes} min)" else "x$repeat"} at ${speed}x. You can pause, resume, restart, or stop it.") },
                onFailure = { ToolResult.error(it.message ?: "Could not start the macro.") },
            )
        }
        "stop" -> if (GameAutomationRuntime.stop()) ToolResult.success("Stopping game automation.")
            else ToolResult.success("No automation is active.")
        "pause" -> if (GameAutomationRuntime.pause()) ToolResult.success("Automation paused.")
            else ToolResult.error("No active automation can be paused.")
        "resume" -> if (GameAutomationRuntime.resume()) ToolResult.success("Automation resumed.")
            else ToolResult.error("No automation is paused.")
        "restart" -> if (GameAutomationRuntime.restart()) ToolResult.success("Automation restarted from the first gesture.")
            else ToolResult.error("No automation is active.")
        "status" -> {
            val s = GameAutomationRuntime.status()
            ToolResult.success(if (s.running) "${s.label}: ${s.completedActions} actions · ${s.message}" else "Inactive. Last status: ${s.message}")
        }
        "delete" -> {
            val name = requireString(params, "name")
            if (GameMacroStore.delete(name)) ToolResult.success("Macro '$name' deleted.")
            else ToolResult.error("Macro '$name' not found.")
        }
            else -> ToolResult.error("operation must be list, play, pause, resume, restart, stop, status, or delete.")
        }
    }
}

class GameAutoclickerTool : BaseTool() {
    override fun getName() = "game_autoclicker"
    override fun getDisplayName() = "Autoclicker de juego"
    override fun getDescriptionEN() =
        "Open/close the floating no-ADB autoclicker, or control a repeated point for up to 30 minutes. " +
        "Playback stops when the game leaves foreground. Start always requires confirmed=true."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "autoclicker visible y limitado para un punto normalizado dentro del juego"
    override fun getParameters() = listOf(
        ToolParameter("operation", "string", "open, close, start, pause, resume, restart, stop, or status", true),
        ToolParameter("x", "integer", "Normalized X 0..1000", false),
        ToolParameter("y", "integer", "Normalized Y 0..1000", false),
        ToolParameter("interval_ms", "integer", "Interval 50..5000 ms", false),
        ToolParameter("duration_seconds", "integer", "Duration 1..1800 seconds", false),
        ToolParameter("confirmed", "boolean", "Explicit user confirmation", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        return when (requireString(params, "operation").lowercase()) {
        "open" -> {
            val packageName = GameScreenState.revealAndGetGamePackage()
                ?: return ToolResult.error("Open a game first to show the autoclicker.")
            GameAutoclickerOverlay.open(packageName).fold(
                onSuccess = { ToolResult.success("Floating autoclicker opened. No ADB or Shizuku required.") },
                onFailure = { ToolResult.error(it.message ?: "Could not open the autoclicker.") },
            )
        }
        "close" -> { GameAutoclickerOverlay.close(); ToolResult.success("Floating autoclicker closed.") }
        "start" -> {
            if (!optionalBoolean(params, "confirmed", false)) return ToolResult.error("Confirm explicitly before starting repeated taps.")
            val packageName = GameScreenState.revealAndGetGamePackage()
                ?: return ToolResult.error("Could not identify the foreground game.")
            if (!GameScreenState.isGame(packageName)) return ToolResult.error("$packageName is not classified as a game.")
            val x = requireInt(params, "x"); val y = requireInt(params, "y")
            runCatching { GameControlPolicy.toPixel(x, 1001); GameControlPolicy.toPixel(y, 1001) }
                .getOrElse { return ToolResult.error("x/y deben estar entre 0 y 1000.") }
            val interval = optionalLong(params, "interval_ms", 250L).coerceIn(50L, 5_000L)
            val duration = optionalLong(params, "duration_seconds", 180L).coerceIn(1L, 1_800L) * 1_000L
            GameAutomationRuntime.startAutoclicker(packageName, x, y, interval, duration).fold(
                onSuccess = { ToolResult.success("▶ Autoclicker started at [$x,$y], every ${interval}ms for ${duration / 1000}s.") },
                onFailure = { ToolResult.error(it.message ?: "Could not start the autoclicker.") },
            )
        }
        "stop" -> if (GameAutomationRuntime.stop()) ToolResult.success("Stopping autoclicker.") else ToolResult.success("It is not active.")
        "pause" -> if (GameAutomationRuntime.pause()) ToolResult.success("Autoclicker paused.") else ToolResult.error("Could not pause it.")
        "resume" -> if (GameAutomationRuntime.resume()) ToolResult.success("Autoclicker resumed.") else ToolResult.error("It was not paused.")
        "restart" -> if (GameAutomationRuntime.restart()) ToolResult.success("Autoclicker restarted.") else ToolResult.error("It is not active.")
        "status" -> {
            val s = GameAutomationRuntime.status()
            ToolResult.success(if (s.running) "${s.label}: ${s.completedActions} taps · ${s.message}" else "Autoclicker inactivo.")
        }
            else -> ToolResult.error("operation must be open, close, start, pause, resume, restart, stop, or status.")
        }
    }
}

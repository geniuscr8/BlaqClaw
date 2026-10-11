package com.blackclaw.android.tool.impl

import com.blackclaw.android.agent.DemonstrationRecorder
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

/**
 * Tools that let the user teach BlaqClaw a flow by demonstration.
 *
 * "Aprende lo que voy a hacer" → start_demo
 * ...the agent performs the steps...
 * "Guárdalo como 'rutina X'" → save_demo(name="rutina X")
 */

class StartDemoTool : BaseTool() {
    override fun getName() = "start_demo"
    override fun getDisplayName() = "Learn by demonstration"
    override fun getDescriptionEN() =
        "Start recording the actions you're about to perform so they can be saved as a reusable " +
        "routine. Use when the user says 'aprende esto', 'mira cómo lo hago', 'graba esto'. " +
        "After recording, call save_demo to store it."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "start recording actions to learn a routine by demonstration"
    override fun getParameters() = listOf(
        ToolParameter("label", "string", "Optional label for what you're learning.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val label = optionalString(params, "label", "")
        DemonstrationRecorder.start(label)
        return ToolResult.success("🎬 Recording. Perform the actions, then say 'save it as [name]'. " +
            "Estoy capturando cada paso replicable.")
    }
}

class SaveDemoTool : BaseTool() {
    override fun getName() = "save_demo"
    override fun getDisplayName() = "Save demonstration"
    override fun getDescriptionEN() =
        "Save the actions recorded since start_demo as a reusable routine. Provide a name. " +
        "Optionally schedule it with trigger_time (HH:MM)."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "guarda lo grabado como una rutina reutilizable"
    override fun getParameters() = listOf(
        ToolParameter("name", "string", "Name for the new routine.", true),
        ToolParameter("trigger_time", "string", "Optional HH:MM to auto-run daily.", false),
        ToolParameter("icon", "string", "Optional emoji icon.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        if (!DemonstrationRecorder.isRecording() && DemonstrationRecorder.currentSteps().isEmpty()) {
            return ToolResult.error("No hay nada grabado. Usa start_demo primero.")
        }
        val name = requireString(params, "name").trim()
        val triggerTime = optionalString(params, "trigger_time", "")
        val icon = optionalString(params, "icon", "🎬")
        val saved = DemonstrationRecorder.saveAsRoutine(name, icon, triggerTime)
            ?: return ToolResult.error("No repeatable steps were captured. Try again with concrete actions.")
        val sched = if (triggerTime.isNotBlank()) " It will run automatically at $triggerTime." else ""
        return ToolResult.success("✓ Routine '$saved' saved from demonstration.$sched Say 'run $saved' to repeat it.")
    }
}

class CancelDemoTool : BaseTool() {
    override fun getName() = "cancel_demo"
    override fun getDisplayName() = "Cancel demonstration"
    override fun getDescriptionEN() = "Stop and discard the current demonstration recording."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "discard the current demonstration recording"
    override fun getParameters() = emptyList<ToolParameter>()
    override fun execute(params: Map<String, Any>): ToolResult {
        DemonstrationRecorder.stop()
        return ToolResult.success("Recording discarded.")
    }
}

/**
 * Save the LAST completed task as a reusable routine — frictionless learning.
 * The recorder passively buffers every task's replayable steps, so the user can
 * say "guarda lo último como rutina X" even without announcing it beforehand.
 */
class SaveLastAsRoutineTool : BaseTool() {
    override fun getName() = "save_last_as_routine"
    override fun getDisplayName() = "Save the latest flow as a routine"
    override fun getDescriptionEN() =
        "Save the steps of the LAST task you just performed as a reusable routine, WITHOUT having " +
        "recorded it in advance. Use when the user says 'guarda lo que hiciste', 'guarda lo último " +
        "como rutina X', 'repite esto la próxima vez'. Optional trigger_time to run it daily."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "save the latest executed flow as a routine (without announcing it first)"
    override fun getParameters() = listOf(
        ToolParameter("name", "string", "Name for the routine.", true),
        ToolParameter("trigger_time", "string", "Optional 'HH:MM' to run it daily.", false),
        ToolParameter("icon", "string", "Optional emoji icon.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val name = requireString(params, "name").trim()
        if (name.isEmpty()) return ToolResult.error("Dame un nombre para la rutina.")
        val triggerTime = optionalString(params, "trigger_time", "")
        val icon = optionalString(params, "icon", "🐾")
        val saved = DemonstrationRecorder.saveLastAsRoutine(name, icon, triggerTime)
            ?: return ToolResult.error(
                "There are no repeatable steps from the latest flow. This works immediately after " +
                "BlaqClaw haga una tarea con acciones (abrir apps, tocar, escribir).")
        val sched = if (triggerTime.isNotBlank()) " It will run at $triggerTime." else ""
        return ToolResult.success("Saved the latest flow as routine '$saved'.$sched Ask me to 'run $saved' whenever you want.")
    }
}

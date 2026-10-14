package com.blackclaw.android.tool.impl

import com.blackclaw.android.adb.PrivilegedShell
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

/**
 * Run a shell command with adb-shell-equivalent privileges via whichever
 * backend is live: Shizuku, or BlaqClaw's own self-paired ADB over loopback.
 *
 * Refuses to run when no privileged backend is ready; the agent should fall
 * back to accessibility-based tools in that case.
 *
 * We don't sandbox the command string. The user opted into Shizuku/ADB, which
 * means they explicitly granted shell-level access to BlaqClaw.
 */
class ShellExecTool : BaseTool() {
    override fun getName() = "shell_exec"
    override fun getDisplayName() = "Shell"
    override fun getDescriptionEN() =
        "Run an adb-shell command via Shizuku or self-paired ADB. Requires one of those " +
        "to be active. Useful for: input tap/swipe (faster than accessibility), " +
        "am force-stop, dumpsys, getprop, settings get/put. Output is captured (max ~8KB)."
    override fun getDescriptionCN() = getDescriptionEN()

    override fun getParameters() = listOf(
        ToolParameter("command", "string",
            "Run a shell command. Example: 'input tap 500 800', 'am force-stop com.x', 'getprop ro.product.model'.",
            true),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        if (!PrivilegedShell.isAvailable()) {
            return ToolResult.error(
                "No privileged access. Ask the user to enable Shizuku or pair ADB " +
                "(Settings → Pro Mode), or use an accessibility-based tool."
            )
        }
        val cmd = requireString(params, "command").trim()
        if (cmd.isEmpty()) return ToolResult.error("Command is empty")
        val out = PrivilegedShell.exec(cmd) ?: return ToolResult.error("Shell failed (timeout or error)")
        return ToolResult.success(if (out.length > 8000) out.take(8000) + "\n…[truncado]" else out)
    }
}

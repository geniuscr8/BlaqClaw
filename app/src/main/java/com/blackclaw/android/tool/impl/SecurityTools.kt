package com.blackclaw.android.tool.impl

import com.blackclaw.android.ClawApplication
import com.blackclaw.android.security.AdCulpritDetector
import com.blackclaw.android.security.AppRiskScanner
import com.blackclaw.android.security.SecurityActions
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

/**
 * Antimalware tools. The AI uses these when the user says things like "una app
 * me llena de anuncios", "revisa si tengo algo malicioso", "bloquea esa app".
 */

/** Scan installed apps and report the riskiest ones with reasons. */
class SecurityScanTool : BaseTool() {
    override fun getName() = "security_scan"
    override fun getDisplayName() = "Escaneo de seguridad"
    override fun getDescriptionEN() =
        "Scan installed apps for risky traits (overlay/ads permission, accessibility, device-admin, " +
        "ability to install apps, dangerous permissions, sideloaded origin, hidden apps) and return " +
        "the riskiest ones with reasons. Use for 'is anything malicious?' / 'check my apps'."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "scan installed apps and report the riskiest ones"
    override fun getParameters() = listOf(
        ToolParameter("limit", "integer", "How many high-risk apps to return. Default 12.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val limit = optionalInt(params, "limit", 12).coerceIn(1, 40)
        val risks = AppRiskScanner.scan().filter { it.level != AppRiskScanner.Level.LOW }.take(limit)
        if (risks.isEmpty()) return ToolResult.success("No apps with notable risk signals found.")
        return ToolResult.success(buildString {
            appendLine("Apps con mayor riesgo (${risks.size}):")
            risks.forEach { r ->
                appendLine("• ${r.label} [${r.pkg}] — riesgo ${r.level} (${r.score})")
                r.reasons.take(4).forEach { appendLine("    - $it") }
            }
            appendLine("Para bloquear/desinstalar una: block_app(package=\"...\", action=\"neutralize|uninstall|disable\").")
        })
    }
}

/** Identify the app most likely spamming ads / drawing overlays. */
class FindAdCulpritTool : BaseTool() {
    override fun getName() = "find_ad_culprit"
    override fun getDisplayName() = "Detectar app de anuncios"
    override fun getDescriptionEN() =
        "Find which installed app is most likely spamming ads or drawing pop-up overlays. " +
        "Ranks apps that can draw over other apps, boosting the current foreground app and recently " +
        "installed ones. Use when the user says an app keeps showing ads. Then offer block_app."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "detect which app is showing ads/overlays"
    override fun getParameters() = emptyList<ToolParameter>()
    override fun execute(params: Map<String, Any>): ToolResult {
        val suspects = AdCulpritDetector.detect().take(6)
        if (suspects.isEmpty())
            return ToolResult.success("No suspicious apps with overlay permission detected. " +
                "The ad may come from a website or the foreground app itself.")
        return ToolResult.success(buildString {
            appendLine("Possible ad sources (most likely first):")
            suspects.forEach { s ->
                appendLine("• ${s.label} [${s.pkg}] — confianza ${s.confidence}%")
                s.reasons.take(3).forEach { appendLine("    - $it") }
            }
            appendLine("Sugerencia: block_app(package=\"${suspects.first().pkg}\", action=\"neutralize\") " +
                "to revoke the overlay and stop it, or action=\"uninstall\" to remove it.")
        })
    }
}

/** Take action against a problem app. */
class BlockAppTool : BaseTool() {
    override fun getName() = "block_app"
    override fun getDisplayName() = "Bloquear app"
    override fun getDescriptionEN() =
        "Act on a problem app. action: 'neutralize' (revoke overlay + force-stop — best for ad spam), " +
        "'stop' (force-stop), 'overlay_off' (revoke draw-over permission), 'disable' (keep installed but " +
        "inert), 'uninstall' (opens the system uninstaller, user confirms), 'settings' (open its app info). " +
        "Privileged actions need Shizuku/ADB; otherwise the matching system screen is opened."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "block, stop, or uninstall a problematic app"
    override fun getParameters() = listOf(
        ToolParameter("package", "string", "Package name de la app a tratar.", true),
        ToolParameter("action", "string",
            "neutralize | stop | overlay_off | disable | uninstall | settings. Default neutralize.", false),
    )
    override fun execute(params: Map<String, Any>): ToolResult {
        val pkg = requireString(params, "package").trim()
        if (pkg.isEmpty()) return ToolResult.error("Package is empty")
        SecurityActions.protectionReason(pkg)?.let { return ToolResult.error("Action blocked: $it") }
        val action = optionalString(params, "action", "neutralize").lowercase().trim()
        val msg = when (action) {
            "neutralize", "block" -> SecurityActions.neutralize(pkg)
            "stop", "force_stop" -> SecurityActions.forceStop(pkg)
            "overlay_off", "overlay" -> SecurityActions.revokeOverlay(pkg)
            "disable" -> SecurityActions.disableApp(pkg)
            "uninstall" -> SecurityActions.uninstall(pkg)
            "settings" -> { SecurityActions.openAppSettings(pkg); "Opened settings for $pkg." }
            else -> return ToolResult.error("Invalid action: $action")
        }
        return ToolResult.success(msg)
    }
}

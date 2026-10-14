package com.blackclaw.android.tool.impl

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.WindowManager
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.adb.PrivilegedShell
import com.blackclaw.android.game.GameControlPolicy
import com.blackclaw.android.game.GameControlSession
import com.blackclaw.android.perception.ScreenCapturePermissionActivity
import com.blackclaw.android.perception.ScreenCaptureService
import com.blackclaw.android.perception.ScreenOcr
import com.blackclaw.android.service.ClawAccessibilityService
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

internal object GameScreenState {
    fun foregroundPackage(): String? = runCatching {
        ClawAccessibilityService.getConnectedInstance(2_000L)
            ?.rootInActiveWindow?.packageName?.toString()
    }.getOrNull()

    fun isGame(packageName: String): Boolean {
        if (GameControlPolicy.knownGameName(packageName) != null) return true
        return runCatching {
            val info = ClawApplication.instance.packageManager.getApplicationInfo(packageName, 0)
            info.category == ApplicationInfo.CATEGORY_GAME
        }.getOrDefault(false)
    }

    /**
     * The Power-button assistant and main chat are Activities, so they temporarily become
     * the accessibility foreground. If they were opened over a game, close that surface
     * before capturing; otherwise OCR would analyze BlaqClaw instead of the game.
     */
    fun revealAndGetGamePackage(): String? {
        val current = foregroundPackage()
        if (current != null && isGame(current)) return current
        val previous = ClawAccessibilityService.getLastExternalPackage()
        if (previous.isNullOrBlank() || !isGame(previous)) return current
        val service = ClawAccessibilityService.getConnectedInstance(2_000L) ?: return current
        service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        try { Thread.sleep(450L) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        val revealed = foregroundPackage()
        return if (revealed != null && isGame(revealed)) revealed else current
    }

    fun frameHash(bitmap: Bitmap): Long {
        val samples = IntArray(64)
        var index = 0
        for (row in 0 until 8) {
            val y = ((row + 0.5) * bitmap.height / 8.0).toInt().coerceIn(0, bitmap.height - 1)
            for (column in 0 until 8) {
                val x = ((column + 0.5) * bitmap.width / 8.0).toInt().coerceIn(0, bitmap.width - 1)
                val color = bitmap.getPixel(x, y)
                val red = color shr 16 and 0xff
                val green = color shr 8 and 0xff
                val blue = color and 0xff
                samples[index++] = (red * 299 + green * 587 + blue * 114) / 1000
            }
        }
        return GameControlPolicy.perceptualHash(samples)
    }

    fun physicalSize(): Pair<Int, Int> {
        val wm = ClawApplication.instance.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    fun normalizedOcr(bitmap: Bitmap, limit: Int = 35): String {
        val blocks = ScreenOcr.recognize(bitmap)
        if (blocks.isEmpty()) return "(OCR: no text; use normalized coordinates and observe changes)"
        return blocks.take(limit).joinToString("\n") { block ->
            val nx = (block.centerX() * 1000 / bitmap.width).coerceIn(0, 1000)
            val ny = (block.centerY() * 1000 / bitmap.height).coerceIn(0, 1000)
            "[$nx,$ny] ${block.text}"
        }
    }
}

class GameObserveTool : BaseTool() {
    override fun getName() = "game_observe"
    override fun getDisplayName() = "Observar juego"
    override fun getDescriptionEN() =
        "Observe the current game frame before acting. Returns the foreground game, a visual frame " +
        "fingerprint and OCR labels using stable normalized coordinates from 0 to 1000. Always call " +
        "this before game_action and again after navigation or an unexpected result."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "observes a game and returns text/normalized coordinates before acting"
    override fun getParameters() = emptyList<ToolParameter>()

    override fun execute(params: Map<String, Any>): ToolResult {
        if (!ScreenCaptureService.isRunning()) {
            ScreenCapturePermissionActivity.start(ClawApplication.instance)
            return ToolResult.error("Concede captura de pantalla, espera 2 segundos y vuelve a observar.")
        }
        val packageName = GameScreenState.revealAndGetGamePackage()
            ?: return ToolResult.error("Could not identify the foreground app; check Accessibility.")
        val bitmap = ScreenCaptureService.captureBitmap()
            ?: return ToolResult.error("The capture does not have a frame yet. Wait and retry.")
        val hash = GameScreenState.frameHash(bitmap)
        GameControlSession.observe(packageName, hash)
        val gameName = GameControlPolicy.knownGameName(packageName) ?: packageName
        val gameStatus = if (GameScreenState.isGame(packageName)) "juego detectado" else "app no clasificada como juego"
        val (width, height) = GameScreenState.physicalSize()
        return ToolResult.success(
            "Modo juego · $gameName ($gameStatus)\n" +
                "Package: $packageName\nPhysical screen: ${width}x$height\n" +
                "Frame: ${java.lang.Long.toUnsignedString(hash, 16)}\n" +
                "Coordenadas OCR normalizadas [x,y] 0..1000:\n${GameScreenState.normalizedOcr(bitmap)}"
        )
    }
}

class GameActionTool : BaseTool() {
    override fun getName() = "game_action"
    override fun getDisplayName() = "Game action"
    override fun getDescriptionEN() =
        "Perform one verified tap or swipe in the currently observed game using normalized 0..1000 " +
        "coordinates. Requires a recent game_observe on the same foreground package. Set confirmed=true " +
        "only after the user confirms attacks, ranked matches, upgrades, purchases or currency spending."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getBrief() = "perform a normalized tap/swipe in a game and verify that the frame changed"
    override fun getParameters() = listOf(
        ToolParameter("action", "string", "tap o swipe", true),
        ToolParameter("x", "integer", "X inicial normalizada 0..1000", true),
        ToolParameter("y", "integer", "Y inicial normalizada 0..1000", true),
        ToolParameter("end_x", "integer", "Final X for swipe, 0..1000", false),
        ToolParameter("end_y", "integer", "Final Y for swipe, 0..1000", false),
        ToolParameter("duration_ms", "integer", "Swipe duration, 50..3000", false),
        ToolParameter("action_label", "string", "Brief description: open shop, attack, move map...", true),
        ToolParameter("confirmed", "boolean", "Explicit confirmation for risky actions", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val packageName = GameScreenState.revealAndGetGamePackage()
            ?: return ToolResult.error("Could not identify the foreground app.")
        GameControlSession.validate(packageName)?.let { return ToolResult.error(it) }
        if (!GameScreenState.isGame(packageName)) {
            return ToolResult.error("$packageName is not classified as a game; I will not send blind input.")
        }

        val label = requireString(params, "action_label").trim()
        if (GameControlPolicy.requiresConfirmation(label) && !optionalBoolean(params, "confirmed", false)) {
            return ToolResult.error("'$label' may spend resources or start a match. Ask for explicit confirmation.")
        }
        val action = requireString(params, "action").lowercase().trim()
        val normalizedX = requireInt(params, "x")
        val normalizedY = requireInt(params, "y")
        val (width, height) = GameScreenState.physicalSize()
        val x = runCatching { GameControlPolicy.toPixel(normalizedX, width) }
            .getOrElse { return ToolResult.error(it.message ?: "Invalid X") }
        val y = runCatching { GameControlPolicy.toPixel(normalizedY, height) }
            .getOrElse { return ToolResult.error(it.message ?: "Invalid Y") }
        val before = GameControlSession.current()?.frameHash ?: 0L

        val dispatched = when (action) {
            "tap" -> dispatchTap(x, y)
            "swipe" -> {
                val endX = runCatching { GameControlPolicy.toPixel(requireInt(params, "end_x"), width) }
                    .getOrElse { return ToolResult.error("end_x is required and must be between 0 and 1000") }
                val endY = runCatching { GameControlPolicy.toPixel(requireInt(params, "end_y"), height) }
                    .getOrElse { return ToolResult.error("end_y is required and must be between 0 and 1000") }
                dispatchSwipe(x, y, endX, endY, optionalInt(params, "duration_ms", 350).coerceIn(50, 3000))
            }
            else -> return ToolResult.error("action must be 'tap' or 'swipe'.")
        }
        if (!dispatched) return ToolResult.error("Could not send the gesture; enable Accessibility or ADB/Shizuku.")

        try { Thread.sleep(450L) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        val bitmap = ScreenCaptureService.captureBitmap()
        val after = bitmap?.let(GameScreenState::frameHash)
        GameControlSession.recordAction(after)
        val change = after?.let { GameControlPolicy.changedPercent(before, it) }
        val verification = if (change == null) "no verification frame" else "visual change: $change%"
        return ToolResult.success("Action '$label' sent in $packageName ($verification). Observe again if the result is unclear.")
    }

    private fun dispatchTap(x: Int, y: Int): Boolean {
        if (PrivilegedShell.isAvailable() && PrivilegedShell.execFast("input tap $x $y")) return true
        return ClawAccessibilityService.getConnectedInstance(2_000L)?.performTap(x, y) == true
    }

    private fun dispatchSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int): Boolean {
        if (PrivilegedShell.isAvailable() &&
            PrivilegedShell.execFast("input swipe $x1 $y1 $x2 $y2 $durationMs")) return true
        return ClawAccessibilityService.getConnectedInstance(2_000L)
            ?.performSwipe(x1, y1, x2, y2, durationMs.toLong()) == true
    }
}

package com.blackclaw.android.tool.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.cards.AssistCard
import com.blackclaw.android.cards.AssistCardCodec
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Best-effort location read.
 *  1. Try the most recent cached fix from any provider (cheap, no GPS spin-up).
 *  2. If none, request a single update with a short timeout.
 *
 * No fancy fused-provider dependency — we use the platform LocationManager so the
 * app stays Play-Services-free. Callers should already hold ACCESS_FINE_LOCATION
 * or ACCESS_COARSE_LOCATION at runtime; this returns a clear error otherwise.
 */
class GetLocationTool : BaseTool() {
    override fun getName() = "get_location"
    override fun getDisplayName() = "Location"
    override fun getDescriptionEN() =
        "Read the device's current location (latitude, longitude, accuracy). " +
        "Uses the last known fix when available, else waits up to 8 seconds for a fresh one. " +
        "Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION at runtime."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = emptyList<ToolParameter>()

    override fun execute(params: Map<String, Any>): ToolResult {
        val ctx = ClawApplication.instance
        val fineGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            return ToolResult.error("Location permission is missing. Grant it in Settings > Apps > BlaqClaw > Permissions.")
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return ToolResult.error("Location service is unavailable.")

        // Try cached fix first
        val providers = lm.getProviders(true)
        if (providers.isEmpty()) {
            return ToolResult.error("No active location providers (is GPS off?).")
        }
        var best: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best.accuracy) best = loc
            } catch (_: SecurityException) {}
        }
        if (best != null && (System.currentTimeMillis() - best.time) < 5L * 60_000L) {
            return located(best)
        }

        // Request a fresh fix with timeout
        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                       else providers.first()
        val latch = CountDownLatch(1)
        var fresh: Location? = null
        val listener = android.location.LocationListener { loc ->
            fresh = loc
            latch.countDown()
        }
        return try {
            try {
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            } catch (e: SecurityException) {
                return ToolResult.error("Runtime permission revoked: ${e.message}")
            }
            val ok = latch.await(8, TimeUnit.SECONDS)
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
            val result = fresh ?: best
            if (result != null) located(result)
            else if (!ok) ToolResult.error("Timed out without a location fix.")
            else ToolResult.error("Could not obtain location.")
        } catch (e: Exception) {
            ToolResult.error("Location error: ${e.message}")
        }
    }

    /**
     * The same sentence as before, plus a card the UI can draw as a map.
     *
     * The coordinates were already here and were only ever formatted into text, so the
     * panel had a location it could not show on a map.
     */
    private fun located(loc: Location): ToolResult = ToolResult.successWithCards(
        data = formatLocation(loc),
        cards = AssistCardCodec.encode(
            listOf(
                AssistCard.Place(
                    name = "Your location",
                    lat = loc.latitude,
                    lon = loc.longitude,
                    detail = "Accuracy ±%.0f m · %s".format(loc.accuracy, loc.provider ?: "?"),
                )
            )
        ),
    )

    private fun formatLocation(loc: Location): String {
        val ageSec = ((System.currentTimeMillis() - loc.time) / 1000L).coerceAtLeast(0)
        return "Location: %.6f, %.6f (accuracy ±%.0fm, %ds ago, provider %s)".format(
            loc.latitude, loc.longitude, loc.accuracy, ageSec, loc.provider ?: "?"
        )
    }
}

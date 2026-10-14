package com.blackclaw.android.tool.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Sunrise / sunset via sunrise-sunset.org (no API key). Uses last-known location. */
class SunInfoTool : BaseTool() {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override fun getName() = "sun_info"
    override fun getDisplayName() = "Sun information"
    override fun getDescriptionEN() =
        "Sunrise, sunset, and civil twilight for the device location (or manually provided lat/lon)."
    override fun getDescriptionCN() = getDescriptionEN()

    override fun getParameters() = listOf(
        ToolParameter("lat", "number", "Latitude (optional). Uses GPS if omitted.", false),
        ToolParameter("lon", "number", "Longitud (opcional).", false),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val lat = (params["lat"] as? Number)?.toDouble() ?: params["lat"]?.toString()?.toDoubleOrNull()
        val lon = (params["lon"] as? Number)?.toDouble() ?: params["lon"]?.toString()?.toDoubleOrNull()
        val (la, lo) = if (lat != null && lon != null) Pair(lat, lon)
                      else (lastKnown() ?: return ToolResult.error("No GPS or lat/lon available."))

        val url = "https://api.sunrise-sunset.org/json?lat=$la&lng=$lo&formatted=0"
        val req = Request.Builder().url(url).get().build()
        return try {
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return ToolResult.error("HTTP ${r.code}")
                val body = r.body?.string() ?: return ToolResult.error("Empty response")
                val json = JSONObject(body)
                val results = json.optJSONObject("results") ?: return ToolResult.error("Invalid format")
                val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                val outFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                fun fmt(key: String): String {
                    val raw = results.optString(key) ?: return "?"
                    return runCatching { outFmt.format(df.parse(raw) ?: Date()) }.getOrDefault(raw)
                }
                ToolResult.success(
                    "Sunrise: ${fmt("sunrise")}\n" +
                    "Solar noon: ${fmt("solar_noon")}\n" +
                    "Sunset: ${fmt("sunset")}\n" +
                    "Civil twilight: ${fmt("civil_twilight_begin")} → ${fmt("civil_twilight_end")}\n" +
                    "Day length: ${results.optInt("day_length") / 3600}h ${(results.optInt("day_length") % 3600) / 60}m"
                )
            }
        } catch (e: Exception) {
            ToolResult.error("Failed: ${e.message}")
        }
    }

    private fun lastKnown(): Pair<Double, Double>? {
        val ctx = ClawApplication.instance
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var best: Location? = null
        for (p in lm.getProviders(true)) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best.accuracy) best = loc
            } catch (_: SecurityException) {}
        }
        return best?.let { Pair(it.latitude, it.longitude) }
    }
}

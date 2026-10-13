package com.blackclaw.android.tool.impl

import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Returns the device's public IP + ASN/country/city via ipapi.co (no key, generous free tier). */
class PublicIpTool : BaseTool() {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override fun getName() = "public_ip"
    override fun getDisplayName() = "Public IP"
    override fun getDescriptionEN() =
        "Returns the device public IP plus country, city, and provider (via ipapi.co, no API key)."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = emptyList<ToolParameter>()

    override fun execute(params: Map<String, Any>): ToolResult {
        val req = Request.Builder().url("https://ipapi.co/json/").get().build()
        return try {
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return ToolResult.error("HTTP ${r.code}")
                val body = r.body?.string() ?: return ToolResult.error("empty response")
                val json = JSONObject(body)
                val ip = json.optString("ip", "?")
                val city = json.optString("city")
                val country = json.optString("country_name")
                val org = json.optString("org")
                val tz = json.optString("timezone")
                ToolResult.success(buildString {
                    append("IP: ").append(ip).append("\n")
                    if (city.isNotBlank()) append("Ciudad: ").append(city).append("\n")
                    if (country.isNotBlank()) append("Country: ").append(country).append("\n")
                    if (org.isNotBlank()) append("Operador: ").append(org).append("\n")
                    if (tz.isNotBlank()) append("Zona horaria: ").append(tz)
                })
            }
        } catch (e: Exception) {
            ToolResult.error("Failed: ${e.message}")
        }
    }
}

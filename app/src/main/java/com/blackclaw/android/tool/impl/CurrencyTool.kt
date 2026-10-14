package com.blackclaw.android.tool.impl

import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Currency conversion via exchangerate.host (free, no API key). */
class CurrencyTool : BaseTool() {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override fun getName() = "currency_convert"
    override fun getDisplayName() = "Currency conversion"
    override fun getDescriptionEN() =
        "Converts between currencies (e.g. EUR → USD, USD → JPY). Uses current exchange rates."
    override fun getDescriptionCN() = getDescriptionEN()

    override fun getParameters() = listOf(
        ToolParameter("amount", "number", "Numeric amount.", true),
        ToolParameter("from", "string", "ISO code (USD, EUR, JPY, GBP, etc.).", true),
        ToolParameter("to", "string", "Destination ISO code.", true),
    )

    override fun execute(params: Map<String, Any>): ToolResult {
        val amount = (params["amount"] as? Number)?.toDouble()
            ?: params["amount"]?.toString()?.toDoubleOrNull()
            ?: return ToolResult.error("Invalid amount")
        val from = requireString(params, "from").uppercase()
        val to = requireString(params, "to").uppercase()
        val url = "https://api.frankfurter.app/latest?amount=$amount&from=$from&to=$to"
        val req = Request.Builder().url(url).get().build()
        return try {
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return ToolResult.error("HTTP ${r.code}")
                val body = r.body?.string() ?: return ToolResult.error("Empty response")
                val json = JSONObject(body)
                val rates = json.optJSONObject("rates") ?: return ToolResult.error("Unexpected format")
                val converted = rates.optDouble(to, Double.NaN)
                if (converted.isNaN()) return ToolResult.error("Unknown currency pair $from→$to")
                val date = json.optString("date", "")
                ToolResult.success("%.2f $from = %.2f $to (exchange rate from $date)".format(amount, converted))
            }
        } catch (e: Exception) {
            ToolResult.error("Currency conversion failed: ${e.message}")
        }
    }
}

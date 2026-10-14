package com.blackclaw.android.tool.impl

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.cards.AssistCard
import com.blackclaw.android.cards.AssistCardCodec
import com.blackclaw.android.cards.SummaryKind
import com.blackclaw.android.tool.BaseTool
import com.blackclaw.android.tool.ToolParameter
import com.blackclaw.android.tool.ToolResult

/**
 * Detailed power / battery telemetry: voltage, current, temperature, technology,
 * health, charge counter, energy counter. Goes deeper than get_device_info(battery).
 */
class PowerInfoTool : BaseTool() {
    override fun getName() = "power_info"
    override fun getDisplayName() = "Power"
    override fun getDescriptionEN() =
        "Detailed battery telemetry: capacity, voltage, current, temperature, " +
        "health, technology, charging plug type, energy counter."
    override fun getDescriptionCN() = getDescriptionEN()
    override fun getParameters() = emptyList<ToolParameter>()

    override fun execute(params: Map<String, Any>): ToolResult {
        val ctx = ClawApplication.instance
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val sb = StringBuilder()

        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        sb.append("Charge: ").append(pct).append("%\n")

        val current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
        sb.append("Current: ").append(current).append(" mA\n")

        val avgCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) / 1000
        sb.append("Average current: ").append(avgCurrent).append(" mA\n")

        val charge = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000
        sb.append("Charge counter: ").append(charge).append(" mAh\n")

        var temperature: String? = null
        var health: String? = null
        var plug: String? = null
        var chargingStatus: String? = null
        intent?.let {
            val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            if (voltage > 0) sb.append("Voltage: ").append(voltage).append(" mV\n")
            val tempRaw = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            temperature = "%.1f°C".format(tempRaw / 10.0)
            sb.append("Temperature: ").append(temperature).append("\n")
            val tech = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            if (!tech.isNullOrBlank()) sb.append("Technology: ").append(tech).append("\n")
            health = when (it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "High voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }
            sb.append("Health: ").append(health).append("\n")
            plug = when (it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC charger"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Not connected"
            }
            sb.append("Connection: ").append(plug).append("\n")
            chargingStatus = when (it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
                else -> "Unknown"
            }
            sb.append("Status: ").append(chargingStatus)
        }
        val detail = listOfNotNull(chargingStatus, plug, temperature, health).joinToString(" · ")
        return ToolResult.successWithCards(
            sb.toString(),
            AssistCardCodec.encode(listOf(AssistCard.Summary(
                kind = SummaryKind.BATTERY,
                label = "Battery",
                value = "$pct%",
                detail = detail,
            ))),
        )
    }
}

package com.blackclaw.android.security

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.blackclaw.android.AppCapabilityCoordinator
import com.blackclaw.android.AppRequirement
import com.blackclaw.android.ServiceBindingState

data class PermissionHealth(val name: String, val granted: Boolean, val required: Boolean, val remedy: String)

/** One consistent permission-health snapshot for onboarding and diagnostics. */
object PermissionDiagnostics {
    fun snapshot(context: Context): List<PermissionHealth> {
        val capabilities = AppCapabilityCoordinator.snapshot(context)
        val exactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return listOf(
            PermissionHealth("Accessibility", capabilities.accessibilityState == ServiceBindingState.READY, true, AppRequirement.ACCESSIBILITY.name),
            PermissionHealth("Notifications", notifications, true, "Open notification settings"),
            PermissionHealth("Notification access", capabilities.notificationAccessState == ServiceBindingState.READY, false, AppRequirement.NOTIFICATION_ACCESS.name),
            PermissionHealth("Display over other apps", capabilities.overlayGranted, false, AppRequirement.OVERLAY.name),
            PermissionHealth("Exact alarms", exactAlarm, false, "Allow exact alarms in Settings"),
            PermissionHealth("Battery optimization", capabilities.batteryOptimizationIgnored, false, AppRequirement.BATTERY_OPTIMIZATION.name),
        )
    }
}

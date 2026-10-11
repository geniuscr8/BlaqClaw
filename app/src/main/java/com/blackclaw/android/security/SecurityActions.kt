package com.blackclaw.android.security

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.blackclaw.android.ClawApplication
import com.blackclaw.android.adb.PrivilegedShell
import com.blackclaw.android.utils.XLog

/**
 * The actions BlackClaw can take against a misbehaving app. Where a privileged
 * backend (Shizuku / self-paired ADB) is available we act silently and for real
 * (`appops`, `am force-stop`, `pm disable-user`); otherwise we open the right
 * system screen so the user can do it in one tap. Uninstall always goes through
 * the system dialog (Android forbids silent uninstall of user apps).
 */
object SecurityActions {

    private const val TAG = "SecurityActions"
    private const val SELF = "com.blackclaw.android"

    /** Stop the ads NOW: revoke the overlay permission and force-stop the app. */
    fun neutralize(pkg: String): String {
        guard(pkg)?.let { return it }
        if (PrivilegedShell.isAvailable()) {
            val r1 = PrivilegedShell.exec("appops set $pkg SYSTEM_ALERT_WINDOW ignore")
            val r2 = PrivilegedShell.exec("am force-stop $pkg")
            XLog.i(TAG, "neutralize $pkg: appops=$r1 stop=$r2")
            val overlay = PrivilegedShell.exec("cmd appops get $pkg SYSTEM_ALERT_WINDOW")
            val running = PrivilegedShell.exec("pidof $pkg")?.trim().orEmpty()
            val verified = overlay?.contains("ignore", ignoreCase = true) == true && running.isBlank()
            return "Blocked $pkg: revoked its overlay and force-stopped it. " +
                if (verified) "Verified: permission blocked and process stopped."
                else "Android accepted the command; scan again to confirm the ads stopped."
        }
        openOverlaySettings(pkg)
        return "No privileged access (Shizuku/ADB). I opened the overlay settings for $pkg " +
            "so you can disable 'Display over other apps'. Enable Pro Mode too so I can do it automatically."
    }

    fun forceStop(pkg: String): String {
        guard(pkg)?.let { return it }
        if (!PrivilegedShell.isAvailable()) { openAppSettings(pkg); return "No privileges: I opened $pkg settings for 'Force stop'." }
        PrivilegedShell.exec("am force-stop $pkg")
        val running = PrivilegedShell.exec("pidof $pkg")?.trim().orEmpty()
        return if (running.isBlank()) "Force-stopped and verified $pkg."
        else "Android received the command, but $pkg still has a process; check its settings."
    }

    fun revokeOverlay(pkg: String): String {
        guard(pkg)?.let { return it }
        if (!PrivilegedShell.isAvailable()) { openOverlaySettings(pkg); return "No privileges: I opened the overlay settings for $pkg." }
        PrivilegedShell.exec("appops set $pkg SYSTEM_ALERT_WINDOW ignore")
        return "Revoked the overlay permission for $pkg."
    }

    /** Disable the app (keeps it installed but inert). Needs privileges. */
    fun disableApp(pkg: String): String {
        guard(pkg)?.let { return it }
        if (!PrivilegedShell.isAvailable()) { openAppSettings(pkg); return "No privileges: I cannot disable it; I opened its settings." }
        val r = PrivilegedShell.exec("pm disable-user --user 0 $pkg")
        return if (r != null) "Disabled $pkg (it remains installed but inactive)." else "Could not disable $pkg."
    }

    /** Uninstall — always via the system dialog (user confirms). */
    fun uninstall(pkg: String): String {
        guard(pkg)?.let { return it }
        return try {
            ClawApplication.instance.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opened the uninstaller for $pkg. The user must confirm."
        } catch (e: Exception) {
            "Could not open the uninstaller: ${e.message}"
        }
    }

    fun openAppSettings(pkg: String) {
        if (!SecurityPolicy.isValidPackageName(pkg)) return
        runCatching {
            ClawApplication.instance.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun openOverlaySettings(pkg: String) {
        val opened = runCatching {
            ClawApplication.instance.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
        if (!opened) openAppSettings(pkg)
    }

    fun protectionReason(pkg: String): String? = SecurityPolicy.protectionReason(ClawApplication.instance, pkg)

    private fun guard(pkg: String): String? = protectionReason(pkg)?.let {
        "Action blocked for $pkg: $it. I will not modify critical apps unless the user does it from Settings."
    }
}

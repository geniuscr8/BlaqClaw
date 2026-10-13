package com.blackclaw.android.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackclaw.android.AppCapabilityCoordinator
import com.blackclaw.android.AppCapabilitySnapshot
import com.blackclaw.android.base.BaseActivity
import com.blackclaw.android.automation.AutomationProfileStore
import com.blackclaw.android.agent.TaskHistoryStore
import com.blackclaw.android.memory.UserMemoryStore
import com.blackclaw.android.ui.chat.BlaqClawColors
import com.blackclaw.android.ui.chat.ThemeManager
import com.blackclaw.android.ui.chat.ThemeManager.toComposeColors
import com.blackclaw.android.ui.design.ClawGlassBackdrop
import com.blackclaw.android.ui.design.ClawGlassCard
import com.blackclaw.android.utils.BlaqClawBackup
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/** One place for setup, recovery, privacy, history, templates, and integrations. */
class ControlCenterActivity : BaseActivity() {
    private var pendingExport: Uri? = null
    private var pendingImport: Uri? = null
    private var refresh = mutableStateOf(0)

    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        pendingExport = uri
        refresh.value++
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
        refresh.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val colors = with(ThemeManager) { ThemeManager.getColors().toComposeColors() }
        setContent {
            val tick by refresh
            val context = LocalContext.current
            val capabilities = remember(tick) { AppCapabilityCoordinator.snapshot(context) }
            var message by remember { mutableStateOf<String?>(null) }
            var passphrase by remember { mutableStateOf("") }
            var showPassphrase by remember { mutableStateOf(false) }
            val export = pendingExport
            val import = pendingImport
            if (export != null || import != null) showPassphrase = true

            ControlCenterScreen(
                colors = colors,
                capabilities = capabilities,
                message = message,
                onBack = { finish() },
                onRefresh = { refresh.value++ },
                onOpenSetup = { startActivity(Intent(this, com.blackclaw.android.ui.onboarding.OnboardingActivity::class.java)) },
                onOpenVoice = { startActivity(Intent(this, VoiceSettingsActivity::class.java)) },
                onOpenModel = { startActivity(Intent(this, LlmConfigActivity::class.java)) },
                onOpenMemory = { startActivity(Intent(this, MemoryPrivacyActivity::class.java)) },
                onOpenTools = { startActivity(Intent(this, com.blackclaw.android.ui.tools.ToolBrowserActivity::class.java)) },
                onOpenTelegram = { ChannelConfigActivity.start(this, ChannelConfigActivity.ChannelType.TELEGRAM) },
                onCreateBackup = { createBackup.launch("BlaqClaw-backup-${System.currentTimeMillis()}.bcbackup") },
                onRestoreBackup = { openBackup.launch(arrayOf("application/octet-stream", "application/json", "*/*")) },
                onInstallTemplate = { name ->
                    val result = installTemplate(name)
                    message = if (result) "$name template added as a disabled draft." else "Could not add $name template."
                    refresh.value++
                },
            )

            if (showPassphrase) {
                AlertDialog(
                    onDismissRequest = {
                        showPassphrase = false; pendingExport = null; pendingImport = null; passphrase = ""
                    },
                    title = { Text(if (export != null) "Encrypt backup" else "Unlock backup") },
                    text = {
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("Passphrase (8+ characters)") },
                            singleLine = true,
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            runCatching {
                                require(passphrase.length >= 8) { "Use at least 8 characters." }
                                if (export != null) {
                                    contentResolver.openOutputStream(export)?.use { it.write(BlaqClawBackup.create(passphrase.toCharArray())) }
                                    message = "Encrypted backup saved."
                                } else if (import != null) {
                                    val summary = contentResolver.openInputStream(import)?.use { BlaqClawBackup.restore(it.readBytes(), passphrase.toCharArray()) }
                                        ?: error("Could not read the backup file.")
                                    message = "Restored ${summary.memories} memories, ${summary.profiles} templates, and ${summary.rules} rules."
                                }
                            }.onFailure { message = it.message ?: "Backup operation failed." }
                            showPassphrase = false; pendingExport = null; pendingImport = null; passphrase = ""
                            refresh.value++
                        }) { Text(if (export != null) "Encrypt and save" else "Restore") }
                    },
                    dismissButton = { TextButton(onClick = { showPassphrase = false }) { Text("Cancel") } },
                )
            }
        }
    }

    private fun installTemplate(name: String): Boolean {
        val text = when (name) {
            "Morning briefing" -> "Read my calendar, weather, and important notifications."
            "Bedtime mode" -> "Enable my bedtime routine and summarize anything important for tomorrow."
            "Commute check" -> "Check traffic and my next calendar event."
            else -> return false
        }
        return AutomationProfileStore.create(
            AutomationProfileStore.Profile(
                id = UUID.randomUUID().toString().take(8),
                name = name,
                description = "Built-in BlaqClaw starter template",
                triggers = listOf(AutomationProfileStore.Trigger(AutomationProfileStore.TriggerType.MANUAL)),
                actions = listOf(AutomationProfileStore.Action(
                    AutomationProfileStore.ActionType.AGENT_TASK,
                    mapOf<String, Any>("text" to text),
                    requireConfirmation = true,
                )),
            ),
            enable = false,
        ).isSuccess
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlCenterScreen(
    colors: BlaqClawColors,
    capabilities: AppCapabilitySnapshot,
    message: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenModel: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenTelegram: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onInstallTemplate: (String) -> Unit,
) {
    val healthy = listOf(
        capabilities.accessibilityStatusLabel == "Enabled",
        capabilities.notificationAccessStatusLabel == "Connected",
        capabilities.overlayGranted,
        capabilities.batteryOptimizationIgnored,
    ).count { it }
    val history = remember { TaskHistoryStore.all().take(5) }
    val memoryCount = remember { UserMemoryStore.all().size }
    val profileCount = remember { AutomationProfileStore.list().size }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Control Center", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary) } },
                actions = { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh", tint = colors.accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        ClawGlassBackdrop(colors = colors) {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                message?.let { Text(it, color = colors.accent, fontSize = 13.sp) }
                SectionCard(colors, Icons.Outlined.CheckCircle, "Setup health", "$healthy of 4 core checks ready") {
                    HealthRow("Accessibility", capabilities.accessibilityStatusLabel, capabilities.canRunInteractiveTask, colors)
                    HealthRow("Notification access", capabilities.notificationAccessStatusLabel, capabilities.canRunMonitor, colors)
                    HealthRow("Overlay window", if (capabilities.overlayGranted) "Enabled" else "Needs setup", capabilities.overlayGranted, colors)
                    HealthRow("Battery", if (capabilities.batteryOptimizationIgnored) "Unrestricted" else "Restricted", capabilities.batteryOptimizationIgnored, colors)
                    Button(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) { Text("Open guided setup") }
                }
                SectionCard(colors, Icons.Outlined.Build, "Quick diagnostics", "Repair common configuration issues") {
                    ActionRow("Voice and offline model", "Check microphone and local voice model", colors, onOpenVoice)
                    ActionRow("AI model configuration", "Cloud, local, and automatic mode", colors, onOpenModel)
                    ActionRow("Memory and privacy", "$memoryCount saved memory items", colors, onOpenMemory)
                }
                SectionCard(colors, Icons.Outlined.Lock, "Encrypted backup", "Portable backup protected by your passphrase") {
                    Text("Back up settings, memories, and automation drafts. Secrets are never exported.", color = colors.textSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onCreateBackup, modifier = Modifier.weight(1f)) { Text("Export") }
                        OutlinedButton(onClick = onRestoreBackup, modifier = Modifier.weight(1f)) { Text("Import") }
                    }
                }
                SectionCard(colors, Icons.Outlined.Extension, "Automation templates", "$profileCount saved profiles · new templates start disabled") {
                    listOf("Morning briefing", "Bedtime mode", "Commute check").forEach { name ->
                        ActionRow(name, "Add a reviewable starter workflow", colors) { onInstallTemplate(name) }
                    }
                }
                SectionCard(colors, Icons.Outlined.History, "Action history", "Recent tasks are stored locally") {
                    if (history.isEmpty()) Text("No completed tasks yet.", color = colors.textSecondary, fontSize = 13.sp)
                    history.forEach { entry ->
                        Text("${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.t))} · ${entry.task}", color = colors.textPrimary, fontSize = 12.sp)
                        Text(entry.outcome, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                SectionCard(colors, Icons.Outlined.Security, "Privacy dashboard", "Local-first visibility for your data") {
                    Text("Memories, task history, and automation drafts stay encrypted on this device.", color = colors.textSecondary, fontSize = 13.sp)
                    Text("Cloud AI is used only when you configure a cloud model or remote channel.", color = colors.textSecondary, fontSize = 13.sp)
                    ActionRow("Open privacy controls", "Review and delete stored memory", colors, onOpenMemory)
                }
                SectionCard(colors, Icons.Outlined.Cloud, "Integrations", "Connect services without leaving BlaqClaw") {
                    ActionRow("Tool catalog", "Browse available device and web integrations", colors, onOpenTools)
                    ActionRow("Telegram bot", "Configure remote access", colors, onOpenTelegram)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(colors: BlaqClawColors, icon: ImageVector, title: String, subtitle: String, content: @Composable () -> Unit) {
    ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = colors.accent)
                Spacer(Modifier.padding(4.dp))
                Column {
                    Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun HealthRow(label: String, status: String, okay: Boolean, colors: BlaqClawColors) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = colors.textPrimary, fontSize = 13.sp)
        Text(status, color = if (okay) colors.accent else colors.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, colors: BlaqClawColors, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp)) {
        Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
    }
}

package com.blackclaw.android.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.blackclaw.android.assistant.VoiceInputManager
import com.blackclaw.android.assistant.VoskModelManager
import com.blackclaw.android.assistant.WhisperMode
import com.blackclaw.android.base.BaseActivity
import com.blackclaw.android.ui.chat.ThemeManager
import com.blackclaw.android.ui.chat.ThemeManager.toComposeColors
import com.blackclaw.android.ui.onboarding.PermissionExplanationDialog
import com.blackclaw.android.ui.onboarding.PermissionTopic

/**
 * Settings screen for hands-free voice mode: enable/disable + offline model
 * status. The offline model ships bundled in the APK and unpacks automatically;
 * this screen lets the user re-prepare it or see the current backend.
 */
class VoiceSettingsActivity : BaseActivity() {

    private val assistantStatus = mutableStateOf<com.blackclaw.android.ui.assist.AssistantRole.Status?>(null)
    private val voiceEnabledState = mutableStateOf(VoiceInputManager.wakeEnabled)
    private val micPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enableVoiceMode() else disableVoiceMode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        assistantStatus.value = com.blackclaw.android.ui.assist.AssistantRole.status(this)
        val permissionTheme = with(ThemeManager) { ThemeManager.getColors().toComposeColors() }
        setContent {
            var showMicEducation by remember { mutableStateOf(false) }
            VoiceSettingsScreen(
                onBack = { finish() },
                enabled = voiceEnabledState.value,
                onEnabledChange = { requested ->
                    if (!requested) disableVoiceMode()
                    else if (hasMicPermission()) enableVoiceMode()
                    else showMicEducation = true
                },
                assistantStatus = assistantStatus.value,
                onOpenAssistantSettings = {
                    com.blackclaw.android.ui.assist.AssistantRole.openSettings(this)
                },
            )
            if (showMicEducation) {
                PermissionExplanationDialog(
                    topic = PermissionTopic.MICROPHONE,
                    colors = permissionTheme,
                    onDismiss = {
                        showMicEducation = false
                        disableVoiceMode()
                    },
                    onContinue = {
                        showMicEducation = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        assistantStatus.value = com.blackclaw.android.ui.assist.AssistantRole.status(this)
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun enableVoiceMode() {
        voiceEnabledState.value = true
        VoiceInputManager.wakeEnabled = true
        runCatching { com.blackclaw.android.service.VoiceWakeService.start(this) }
    }

    private fun disableVoiceMode() {
        voiceEnabledState.value = false
        VoiceInputManager.wakeEnabled = false
        VoiceInputManager.stopWakeLoop()
        runCatching { com.blackclaw.android.service.VoiceWakeService.stop(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSettingsScreen(
    onBack: () -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    assistantStatus: com.blackclaw.android.ui.assist.AssistantRole.Status?,
    onOpenAssistantSettings: () -> Unit,
) {
    val theme = ThemeManager.getColors()
    val bg = Color(theme.bg)
    val surface = Color(theme.aiBubble)
    val accent = Color(theme.sendColor)
    val textPrimary = Color(theme.aiText)
    val textSecondary = Color(theme.toolDefault)

    var whisper by remember { mutableStateOf(WhisperMode.enabled) }
    var modelReady by remember { mutableStateOf(VoskModelManager.isReady()) }
    var preparing by remember { mutableStateOf(VoskModelManager.preparing) }
    var progress by remember { mutableStateOf(0) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Poll preparation progress.
    LaunchedEffect(preparing) {
        while (preparing) {
            kotlinx.coroutines.delay(500)
            modelReady = VoskModelManager.isReady()
            preparing = VoskModelManager.preparing
        }
        modelReady = VoskModelManager.isReady()
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Voice mode", color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Set BlaqClaw as the phone's default assistant (assist gesture).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenAssistantSettings)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐾", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Phone assistant", color = textPrimary, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                assistantStatus?.isReady == true -> "BlaqClaw is your active assistant ✓"
                                assistantStatus?.needsRepair == true -> "BlaqClaw is selected, but Android is still using another service"
                                else -> "Set it as default to invoke it with the gesture or button"
                            },
                            color = if (assistantStatus?.isReady == true) accent else textSecondary, fontSize = 12.sp,
                            lineHeight = 16.sp)
                    }
                    Text("Configure", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (assistantStatus?.needsRepair == true) {
                        "Android role and voice service are out of sync. Tap here and select BlaqClaw again in Settings."
                    } else {
                        "When invoked (hold the Home button or swipe from the corner), " +
                            "BlaqClaw appears even over the lock screen and listens for your command."
                    },
                    color = textSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Mic, null, tint = accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Enable voice mode", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (modelReady) "Say \"claw\" + your command (offline, no beep)"
                        else "Say \"BlaqClaw\" + your command (online)",
                        color = textSecondary, fontSize = 13.sp,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent),
                )
            }

            // Show the full-screen panel when invoked by voice.
            var panelOnWake by remember { mutableStateOf(VoiceInputManager.panelOnWake) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🪟", fontSize = 22.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mostrar panel al hablar", color = textPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("When you say the wake word, opens the animated floating panel. " +
                        "When disabled, it responds only in the background.",
                        color = textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Switch(
                    checked = panelOnWake,
                    onCheckedChange = { panelOnWake = it; VoiceInputManager.panelOnWake = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent),
                )
            }

            // Offline model status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Recognition language", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                var activeLang by remember { mutableStateOf(VoskModelManager.activeLang) }
                var enReady by remember { mutableStateOf(VoskModelManager.isReady(VoskModelManager.Lang.EN)) }
                var dlEn by remember { mutableStateOf(false) }
                var dlProg by remember { mutableStateOf(0) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(VoskModelManager.Lang.EN to "English (claw)").forEach { (lang, label) ->
                        val selected = activeLang == lang
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) accent.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(10.dp))
                                .clickable {
                                    if (lang == VoskModelManager.Lang.EN && !enReady) {
                                        // need download first
                                    } else {
                                        VoskModelManager.activeLang = lang
                                        activeLang = lang
                                        if (VoiceInputManager.wakeEnabled) {
                                            runCatching { com.blackclaw.android.service.VoiceWakeService.start(ctx) }
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(label, color = if (selected) accent else textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                        }
                    }
                }
                if (!enReady) {
                    if (dlEn) {
                        Text("Downloading English model… $dlProg%", color = textSecondary, fontSize = 12.sp)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accent)
                    } else {
                        Box(
                            modifier = Modifier
                                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .clickable {
                                    dlEn = true
                                    VoskModelManager.download(VoskModelManager.Lang.EN,
                                        onProgress = { dlProg = it },
                                        onDone = { ok ->
                                            dlEn = false
                                            enReady = ok
                                            if (ok) {
                                                VoskModelManager.activeLang = VoskModelManager.Lang.EN
                                                activeLang = VoskModelManager.Lang.EN
                                            }
                                        })
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("⬇ Download English (~40MB)", color = accent, fontSize = 13.sp)
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Text("English offline recognition", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val statusText = when {
                    modelReady -> "✓ Ready — works without internet or a beep (wake word: \"claw\")"
                    preparing -> "⏳ Preparing the model… $progress%"
                    else -> "The offline model is not ready. The system recognizer is used meanwhile (online)."
                }
                Text(statusText, color = textSecondary, fontSize = 13.sp)

                if (preparing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = accent,
                    )
                } else if (!modelReady) {
                    Box(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .clickable {
                                preparing = true
                                VoskModelManager.download(
                                    onProgress = { progress = it },
                                    onDone = { preparing = false; modelReady = VoskModelManager.isReady() },
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text("Prepare offline model", color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Whisper mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Whisper mode", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "If you whisper, it replies in a whisper (like Alexa). Requires the offline model.",
                        color = textSecondary, fontSize = 13.sp,
                    )
                }
                Switch(
                    checked = whisper,
                    onCheckedChange = { whisper = it; WhisperMode.enabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent),
                )
            }

            // Whisper calibration
            var calibrating by remember { mutableStateOf(false) }
            var calibMsg by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Calibrate normal voice", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    calibMsg.ifBlank { "Improve whisper detection: press and speak normally for 2 seconds." },
                    color = textSecondary, fontSize = 13.sp,
                )
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = if (calibrating) 0.08f else 0.15f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !calibrating) {
                            calibrating = true
                            calibMsg = "🎙 Listening… speak normally"
                            VoiceInputManager.calibrateWhisper { rms ->
                                calibrating = false
                                calibMsg = if (rms > 0) "✓ Calibrado (nivel ${rms.toInt()})" else "I did not hear you; try again"
                                // Restart the service if voice mode is on.
                                if (VoiceInputManager.wakeEnabled) {
                                    runCatching { com.blackclaw.android.service.VoiceWakeService.start(ctx) }
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(if (calibrating) "Listening…" else "Calibrate now", color = accent, fontWeight = FontWeight.Bold)
                }
            }

            // Help
            Text(
                "Voice mode listens in the background while BlaqClaw is open. " +
                "With the offline model, no internet or beep is needed. " +
                "Examples: \"claw, set an alarm for 7\", \"claw, send Mom a message\".",
                color = textSecondary, fontSize = 12.sp,
            )
        }
    }
}

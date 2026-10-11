package com.blackclaw.android.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.blackclaw.android.base.BaseActivity
import com.blackclaw.android.emergency.EmergencyConfig
import com.blackclaw.android.emergency.EmergencyEvidenceVault
import com.blackclaw.android.emergency.EmergencyCameraController
import com.blackclaw.android.emergency.EmergencyCameras
import com.blackclaw.android.emergency.EmergencyMode
import com.blackclaw.android.emergency.EmergencyStartOptions
import com.blackclaw.android.emergency.EmergencyService
import com.blackclaw.android.ui.chat.ThemeManager
import com.blackclaw.android.ui.chat.ThemeManager.toComposeColors
import com.blackclaw.android.ui.onboarding.PermissionBundleExplanationDialog
import com.blackclaw.android.ui.onboarding.PermissionTopic

@OptIn(ExperimentalMaterial3Api::class)
class EmergencySettingsActivity : BaseActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionTheme = with(ThemeManager) { ThemeManager.getColors().toComposeColors() }
        setContent {
            var name by remember { mutableStateOf(EmergencyConfig.contactName) }
            var phone by remember { mutableStateOf(EmergencyConfig.phone) }
            var message by remember { mutableStateOf(EmergencyConfig.message) }
            var audio by remember { mutableStateOf(EmergencyConfig.recordAudio) }
            var torch by remember { mutableStateOf(EmergencyConfig.lowLightTorch) }
            var saved by remember { mutableStateOf(false) }
            var active by remember { mutableStateOf(EmergencyService.isActive) }
            var pendingPermissionRequest by remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) }
            var evidenceItems by remember { mutableStateOf(EmergencyEvidenceVault.listEvidence(this@EmergencySettingsActivity)) }
            val evidenceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                evidenceItems = EmergencyEvidenceVault.listEvidence(this@EmergencySettingsActivity)
            }
            val cameraCapability = remember { runCatching { EmergencyCameraController.inspect(this@EmergencySettingsActivity) }.getOrNull() }
            var cameraChoice by remember { mutableStateOf(EmergencyCameras.BACK) }
            val formReady = phone.filter(Char::isDigit).length >= 7 && message.isNotBlank()

            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF9D7CFF),
                secondary = Color(0xFF39C8E8),
                background = Color(0xFF07050C),
                surface = Color(0xFF181222),
                surfaceVariant = Color(0xFF241B33),
                onPrimary = Color.White,
                onBackground = Color(0xFFF4EEFF),
                onSurface = Color(0xFFF4EEFF),
            )) {
                Scaffold(
                    containerColor = Color(0xFF07050C),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("Personal protection", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Emergency and discreet mode", style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF998DAD))
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF100B18)),
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = Color(0xFA100B18),
                            shadowElevation = 18.dp,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!formReady && !active) {
                                    Text(
                                        "Complete and save the contact to enable activation.",
                                        color = Color(0xFFFFC46B),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                if (active) {
                                    Button(
                                        onClick = {
                                            EmergencyService.stop(this@EmergencySettingsActivity)
                                            active = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB62F45)),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                    ) { Text("STOP PROTECTION") }
                                } else {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Button(
                                            onClick = {
                                                active = EmergencyService.start(
                                                    this@EmergencySettingsActivity,
                                                    EmergencyStartOptions(EmergencyMode.EMERGENCY, cameraChoice, true),
                                                )
                                            },
                                            enabled = formReady,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF7C4DDB),
                                                disabledContainerColor = Color(0xFF2D2638),
                                            ),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                        ) { Text("EMERGENCY") }
                                        OutlinedButton(
                                            onClick = {
                                                active = EmergencyService.start(
                                                    this@EmergencySettingsActivity,
                                                    EmergencyStartOptions(EmergencyMode.DISCREET, cameraChoice, true),
                                                )
                                            },
                                            enabled = formReady,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCCBAFF)),
                                        ) { Text("DISCREET MODE") }
                                    }
                                }
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).background(
                        Brush.verticalGradient(listOf(Color(0xFF17102A), Color(0xFF07050C), Color(0xFF050409))))) {
                      Column(
                          Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 16.dp)
                              .verticalScroll(rememberScrollState()),
                          verticalArrangement = Arrangement.spacedBy(14.dp),
                      ) {
                        ElevatedCard(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xD9211930))) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.Warning, null, tint = Color(0xFFFFB45C))
                                Text(
                                    "Emergency gives you 5 seconds to cancel, then updates your location every 5 minutes. Discreet mode starts without voice, sound, or vibration, using a neutral notification and Android indicators.",
                                    color = Color(0xFFE5DDEE),
                                )
                            }
                        }
                        Text("CONTACT AND MESSAGE", color = Color(0xFF9D7CFF),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(name, { name = it; saved = false }, label = { Text("Trusted contact name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(phone, { phone = it; saved = false }, label = { Text("Phone with country code") }, supportingText = { Text("Example: +1 555 000 0000") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(message, { message = it; saved = false }, label = { Text("Emergency message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Evidence audio", color = Color(0xFFF4EEFF))
                                Text("Protected 30-second segments.", color = Color(0xFFA89DB9), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(audio, { audio = it; saved = false })
                        }
                        Text("CAMERAS", color = Color(0xFF9D7CFF),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = cameraChoice == EmergencyCameras.FRONT,
                                onClick = { cameraChoice = EmergencyCameras.FRONT },
                                label = { Text("Front") },
                                enabled = cameraCapability?.frontId != null,
                            )
                            FilterChip(
                                selected = cameraChoice == EmergencyCameras.BACK,
                                onClick = { cameraChoice = EmergencyCameras.BACK },
                                label = { Text("Rear") },
                                enabled = cameraCapability?.backId != null,
                            )
                            FilterChip(
                                selected = cameraChoice == EmergencyCameras.BOTH,
                                onClick = { cameraChoice = EmergencyCameras.BOTH },
                                label = { Text("Both") },
                                enabled = cameraCapability?.concurrentFrontBack == true,
                            )
                        }
                        Text(
                            when {
                                cameraCapability == null -> "Could not check camera capabilities."
                                cameraCapability.concurrentFrontBack -> "This phone officially reports support for simultaneous front and rear cameras."
                                else -> "Android does not report simultaneous cameras; if you request both, BlaqClaw will use the rear camera and record that choice."
                            },
                            color = Color(0xFFA89DB9), style = MaterialTheme.typography.bodySmall,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Low-light torch", color = Color(0xFFF4EEFF))
                                Text(
                                    "Turns on the rear flash while recording. Without it, night video will be dark, but the light is visible to people nearby. It is not used in discreet mode.",
                                    color = Color(0xFFA89DB9), style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(torch, { torch = it; saved = false })
                        }
                        Button(
                            onClick = {
                                EmergencyConfig.contactName = name
                                EmergencyConfig.phone = phone
                                EmergencyConfig.message = message
                                EmergencyConfig.recordAudio = audio
                                EmergencyConfig.lowLightTorch = torch
                                saved = true
                                pendingPermissionRequest = audio to (cameraChoice != EmergencyCameras.NONE)
                            },
                            enabled = formReady,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text(if (saved) "Configuration saved" else "SAVE AND PREPARE") }

                        ElevatedCard(shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xD91A1424))) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Protected files", color = Color(0xFFF4EEFF), fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (evidenceItems.isEmpty()) "No segments have been saved yet."
                                    else "${evidenceItems.size} protected segments · ${formatEvidenceBytes(evidenceItems.sumOf { it.bytes })}.",
                                    color = Color(0xFFA89DB9), style = MaterialTheme.typography.bodySmall,
                                )
                                Text("Complete files are never attached to the AI context.",
                                    color = Color(0xFFA89DB9), style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = {
                                        evidenceLauncher.launch(Intent(
                                            this@EmergencySettingsActivity,
                                            EmergencyEvidenceActivity::class.java,
                                        ))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                ) { Text("VIEW EVIDENCE") }
                            }
                        }

                        Text(
                            "Android and BlaqClaw show indicators while the camera or microphone is active. SMS costs may apply depending on your carrier.",
                            color = Color(0xFF8F849F),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(32.dp))
                      }
                    }
                }
            }

            pendingPermissionRequest?.let { (needsAudio, needsCamera) ->
                val topics = buildList {
                    add(PermissionTopic.SMS)
                    add(PermissionTopic.LOCATION)
                    if (needsAudio) add(PermissionTopic.MICROPHONE)
                    if (needsCamera) add(PermissionTopic.CAMERA)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(PermissionTopic.NOTIFICATIONS)
                }
                PermissionBundleExplanationDialog(
                    topics = topics,
                    colors = permissionTheme,
                    title = "Personal protection permissions",
                    onDismiss = { pendingPermissionRequest = null },
                    onContinue = {
                        pendingPermissionRequest = null
                        requestEmergencyPermissions(needsAudio, needsCamera)
                    },
                )
            }
        }
    }

    private fun requestEmergencyPermissions(audio: Boolean, camera: Boolean) {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (audio) permissions += Manifest.permission.RECORD_AUDIO
        if (camera) permissions += Manifest.permission.CAMERA
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions += Manifest.permission.POST_NOTIFICATIONS
        val missing = permissions.distinct().filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }
}

private fun formatEvidenceBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    else -> "${bytes / 1024} KB"
}

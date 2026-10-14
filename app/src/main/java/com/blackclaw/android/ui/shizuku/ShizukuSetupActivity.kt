package com.blackclaw.android.ui.shizuku

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.blackclaw.android.base.BaseActivity
import com.blackclaw.android.shizuku.ShizukuManager
import com.blackclaw.android.ui.chat.BlaqClawColors
import com.blackclaw.android.ui.chat.ThemeManager
import com.blackclaw.android.ui.chat.ThemeManager.toComposeColors
import kotlinx.coroutines.delay

class ShizukuSetupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val tc = ThemeManager.getColors()
        window.statusBarColor = tc.toolbarBg
        val colors = with(ThemeManager) { tc.toComposeColors() }
        setContent {
            ShizukuSetupScreen(
                colors = colors,
                onBack = { finish() },
                onOpenPlay = { openMarket("moe.shizuku.privileged.api") },
                onOpenShizuku = { openShizukuApp() },
                onRequestPermission = { ShizukuManager.requestPermission() },
            )
        }
    }

    private fun openMarket(pkg: String) {
        val tryGoogle = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webFallback = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { startActivity(tryGoogle) } catch (_: Exception) {
            try { startActivity(webFallback) } catch (_: Exception) {}
        }
    }

    private fun openShizukuApp() {
        val pkg = "moe.shizuku.privileged.api"
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) startActivity(launch)
        else openMarket(pkg)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShizukuSetupScreen(
    colors: BlaqClawColors,
    onBack: () -> Unit,
    onOpenPlay: () -> Unit,
    onOpenShizuku: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    val state = remember(refreshTick) { ShizukuManager.state(ctx) }

    // Auto-refresh every 1.5 s while the screen is visible so the state
    // reflects what the user just did in the Shizuku app.
    LaunchedEffect(Unit) {
        while (true) { delay(1500); refreshTick++ }
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Shizuku", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("Quick actions for BlaqClaw",
                            fontSize = 11.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(Icons.Default.Refresh, "Refrescar", tint = colors.textSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            StatusCard(state, colors, onOpenPlay, onOpenShizuku, onRequestPermission)

            // Why Shizuku card
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = colors.accent,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("What is it for?",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "With Shizuku active, BlaqClaw can:\n" +
                        "• Tap and swipe ~10× faster\n" +
                        "• Work inside games and apps with Surface views\n" +
                        "• Truly force-stop apps\n" +
                        "• Run useful shell commands (am, dumpsys, settings…)\n\n" +
                        "If you do not install it, nothing changes: BlaqClaw continues to work with Accessibility.",
                        fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp,
                    )
                }
            }

            // How-to setup tabs
            if (state == ShizukuManager.State.NOT_INSTALLED || state == ShizukuManager.State.INSTALLED_OFF) {
                Text(
                    "HOW TO ENABLE IT",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = colors.textTertiary, letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 6.dp),
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.surface,
                    contentColor = colors.accent,
                    modifier = Modifier.padding(horizontal = 14.dp).clip(RoundedCornerShape(12.dp)),
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Without PC", fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Smartphone, null, modifier = Modifier.size(16.dp)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("With PC", fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Computer, null, modifier = Modifier.size(16.dp)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (selectedTab == 0) WirelessAdbInstructions(colors) else PcInstructions(colors)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusCard(
    state: ShizukuManager.State,
    colors: BlaqClawColors,
    onOpenPlay: () -> Unit,
    onOpenShizuku: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val (icon, title, subtitle, action, actionColor) = when (state) {
        ShizukuManager.State.NOT_INSTALLED -> Quintuple(
            Icons.Default.Warning,
            "Not installed",
            "Install the Shizuku app to get started.",
            "Install Shizuku", colors.accent,
        )
        ShizukuManager.State.INSTALLED_OFF -> Quintuple(
            Icons.Default.Warning,
            "Installed · service off",
            "Open Shizuku and start the service (instructions below).",
            "Open Shizuku", colors.accent,
        )
        ShizukuManager.State.RUNNING_NO_PERM -> Quintuple(
            Icons.Default.Warning,
            "Service running · permission needed",
            "Grant BlaqClaw access to Shizuku.",
            "Grant permission", colors.accent,
        )
        ShizukuManager.State.READY -> Quintuple(
            Icons.Default.Check,
            "Todo listo",
            "BlaqClaw can use quick actions through Shizuku.",
            null, Color(0xFF22C55E),
        )
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, actionColor.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(actionColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = actionColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary)
                    Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
                }
            }
            if (action != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        when (state) {
                            ShizukuManager.State.NOT_INSTALLED -> onOpenPlay()
                            ShizukuManager.State.INSTALLED_OFF -> onOpenShizuku()
                            ShizukuManager.State.RUNNING_NO_PERM -> onRequestPermission()
                            else -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(action, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WirelessAdbInstructions(colors: BlaqClawColors) {
    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        StepCard(1, "Enable Developer options",
            "Settings → About phone → tap \"Build number\" 7 times.",
            colors)
        StepCard(2, "Enable wireless debugging",
            "Settings → System → Developer options → " +
            "enable \"Wireless debugging\". Accept the prompt. " +
            "This option is available starting with Android 11.",
            colors)
        StepCard(3, "Pair once",
            "Tap \"Wireless debugging\" to enter. Tap " +
            "\"Pair device with pairing code\". " +
            "Note the port and code shown.",
            colors)
        StepCard(4, "Start Shizuku",
            "Open the Shizuku app → \"Use wireless debugging\" tab → " +
            "enter the code and port shown earlier. If it asks for auto-pairing, " +
            "use it. You will see \"Service running\" in green.",
            colors)
        StepCard(5, "Grant permission to BlaqClaw",
            "Return here. The status above should say \"Service running · " +
            "permission needed\". Tap \"Grant permission\" and accept the popup.",
            colors)
        Surface(
            color = colors.aiBubble, shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            Text(
                "⚠️ You must repeat steps 3–4 after each phone reboot " +
                "(an Android limitation, not a BlaqClaw limitation). " +
                "BlaqClaw's permission is retained.",
                fontSize = 11.sp, color = colors.textSecondary, lineHeight = 16.sp,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun PcInstructions(colors: BlaqClawColors) {
    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        StepCard(1, "Connect the phone to the PC",
            "USB with USB debugging enabled (Settings → System → Developer " +
            "options → \"USB debugging\").",
            colors)
        StepCard(2, "Install adb on your PC",
            "If you do not have it, download Google's Platform Tools " +
            "(developer.android.com/tools/releases/platform-tools).",
            colors)
        StepCard(3, "Check that adb sees the phone",
            "Terminal/CMD: `adb devices`. Your device should be listed as \"device\".",
            colors)
        StepCard(4, "Start Shizuku from your PC",
            "Ejecuta:\n" +
            "    adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh\n\n" +
            "(Algunas builds usan /storage/emulated/0/... — la ruta correcta sale en " +
                "the Shizuku app itself, under \"Start via ADB\".)",
            colors)
        StepCard(5, "BlaqClaw permission",
            "Return to this screen and tap \"Grant permission\".",
            colors)
        Surface(
            color = colors.aiBubble, shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            Text(
                "✅ This method is more stable: the service stays active even if " +
                "you reboot the phone (as long as you do not power it off).",
                fontSize = 11.sp, color = colors.textSecondary, lineHeight = 16.sp,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun StepCard(num: Int, title: String, body: String, colors: BlaqClawColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$num", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = colors.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary)
            Text(body, fontSize = 12.sp, color = colors.textSecondary,
                lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E,
)

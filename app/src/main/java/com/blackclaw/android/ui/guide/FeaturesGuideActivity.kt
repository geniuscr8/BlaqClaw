package com.blackclaw.android.ui.guide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * A scrollable "what BlaqClaw can do" guide with concrete example phrases for
 * each capability — so users actually discover voice, smart home, remote PC,
 * routines, habits, free models, etc.
 */
class FeaturesGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { FeaturesGuideScreen(onBack = { finish() }) }
    }
}

private data class Feature(
    val emoji: String,
    val title: String,
    val desc: String,
    val examples: List<String>,
)

private val FEATURES = listOf(
    Feature("📲", "Control your apps",
        "Ask for actions in popular apps and BlaqClaw jumps straight to the useful screen with deep links, then completes the rest. Supports 60+ apps: Uber, DiDi, Cabify, Uber Eats, Rappi, DoorDash, Glovo, Spotify, YouTube, Maps, Waze, WhatsApp, Telegram, Instagram, X, Amazon, Mercado Libre, AliExpress, eBay, Netflix, Disney+, Booking, Airbnb, Gmail, and more. Accessibility can operate ANY app on screen.",
        listOf("\"get me an Uber to the airport\"", "\"order sushi on Uber Eats\"", "\"find a keyboard on Amazon\"", "\"navigate home\"")),
    Feature("🐾", "Phone assistant",
        "Set it as the default assistant (Settings → Voice mode → Phone assistant) and invoke it with the Home gesture or button, even over the lock screen. An animated floating panel appears, and you can have a conversation without repeating the wake word. Tap the orb to interrupt it.",
        listOf("Hold the Home button → speak", "\"what time is it?\" → \"will it rain tomorrow?\"", "Say \"thanks\" to close")),
    Feature("📅", "Calendar and agenda",
        "Tell it about an appointment and it adds it to the calendar, rings an alarm at the right time, and shows it in the agenda — now or weeks from now. View everything in Calendar (Month/Agenda) and reschedule with a tap.",
        listOf("\"I have a meeting at 7\"", "\"I have a doctor appointment at 5 PM in three weeks\"", "\"what do I have today?\"")),
    Feature("🗓️", "Scheduled tasks and automations",
        "Create a task from Automations with a guided flow: describe what you want, choose whether BlaqClaw acts or only alerts you, select a time or date, and choose whether it repeats. You can also create Tasker-style profiles triggered by Wi-Fi, notifications, location, or time.",
        listOf("\"remind me to check the weather in 30 minutes\"", "\"check my agenda every Monday at 9\"", "\"when I connect to home Wi-Fi, alert me\"")),
    Feature("🎵", "Music in your player",
        "Play music in the player you use (Musicolet, Spotify, YouTube Music, Poweramp…), not just one specific app. It works offline with your local music.",
        listOf("\"play Bad Bunny\"", "\"play lofi in YouTube Music\"", "\"use Musicolet for music\"")),
    Feature("🎤", "Hands-free voice mode",
        "Enable voice mode in Settings. Say the wake word and your command. It works in the background without internet (offline model included).",
        listOf("\"claw, set an alarm for 7\"", "\"claw, how much battery do I have?\"", "\"claw, send Mom a message\"")),
    Feature("⏰", "Native assistant",
        "Alarms, reminders, notes, events, and finances inside the app — without switching to other apps.",
        listOf("\"remind me to call the dentist tomorrow at 5\"", "\"set a challenge alarm to wake me up\"", "\"note that I have a meeting on Monday\"")),
    Feature("🤖", "Proactive assistant",
        "Monitor your notifications and act only when appropriate: set appointment alarms, record charges, and alert you about important things. Configure it in Settings → Proactive.",
        listOf("Detects \"meeting tomorrow at 9 AM\" → sets an 8:30 alarm", "Detects a charge → records it in finances")),
    Feature("🌐", "Web search",
        "Ask about anything current and search for a real answer.",
        listOf("\"what movies are released this week?\"", "\"bitcoin price\"", "\"today's news\"")),
    Feature("🏠", "Smart home",
        "Control lights, plugs, and devices through webhooks (Home Assistant, IFTTT). Configure them first.",
        listOf("\"turn on the living room light\"", "\"turn off the coffee maker\"")),
    Feature("📶", "Automatic local / cloud mode",
        "In Settings → Model → Automatic, BlaqClaw uses your cloud model when internet is available and switches to the downloaded local model when offline. You can see the active model in the chat selector.",
        listOf("Enable \"Automatic\" in the model selector", "Offline → respond with your local model", "Online → use your configured cloud model")),
    Feature("👁️", "See and understand the screen",
        "Read text with OCR, even in games and custom surfaces. Vision-capable models receive the original image; otherwise OCR text is used as a fallback. Accessibility can also be combined with the screen to tap the correct element.",
        listOf("\"read what is on the screen\"", "\"find the Continue button and tap it\"", "Attach an image and ask what it contains")),
    Feature("💻", "Control your PC",
        "Connect to your computer over SSH and diagnose or fix problems from your phone.",
        listOf("\"connect to my PC at 192.168.1.10 as angel\"", "\"my PC Wi-Fi is not working, fix it\"", "\"watch the log and alert me if there is an error\"")),
    Feature("⌨️", "Termux-style terminal",
        "Includes a persistent local terminal for you and BlaqClaw. It works without root or Shizuku for app-environment commands, with optional self-ADB, Shizuku, or Wi-Fi ADB for privileged actions.",
        listOf("\"open the terminal\"", "\"list the files in my workspace\"", "\"connect to the remote device over ADB\"")),
    Feature("📚", "Offline library",
        "Query compatible ZIM files without internet. BlaqClaw detects local libraries, searches the index, and returns only relevant passages instead of inventing an answer.",
        listOf("\"query my offline Wikipedia: what is TCP?\"", "\"search this in the ZIM library\"")),
    Feature("⚡", "Routines",
        "Create multi-step sequences that run at a scheduled time or when requested. Save, test, edit, and limit how often they run.",
        listOf("\"create a morning routine: alarm at 7, weather, calendar\"", "\"run my morning routine\"", "\"save these steps as routine X\"")),
    Feature("📊", "Habits and wellness",
        "Track habits with streaks and record mood or sleep.",
        listOf("\"I drank 2 glasses of water\"", "\"I exercised for 30 minutes\"", "\"I slept 7 hours and slept well\"")),
    Feature("🧠", "Gets to know you",
        "Learns your preferences and routines over time, with a JARVIS-style personality.",
        listOf("\"remember that my boss is named Carlos\"", "\"what meetings do I have this week?\"")),
    Feature("💸", "Free models",
        "In Settings → Model → BlaqClaw Free, you have free cloud models without an account or API key. Update the catalog from the chat model selector, or import compatible local models from Downloads, Documents, or AI folders.",
        listOf("Choose BlaqClaw Free → Update models", "Download or import a compatible local model", "Combine it with Automatic mode")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturesGuideScreen(onBack: () -> Unit) {
    val bg = Color(0xFF0A0A0F)
    val surface = Color(0xFF141420)
    val accent = Color(0xFF00D4FF)
    val textPrimary = Color(0xFFC8D0E8)
    val textSecondary = Color(0xFF7A80A0)

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("What can it do?", color = textPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "BlaqClaw is your personal assistant. Type or speak — here is what it can do:",
                color = textSecondary, fontSize = 14.sp,
            )
            FEATURES.forEach { f ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(f.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(f.desc, color = textSecondary, fontSize = 13.sp)
                    f.examples.forEach { ex ->
                        Text("• $ex", color = accent, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

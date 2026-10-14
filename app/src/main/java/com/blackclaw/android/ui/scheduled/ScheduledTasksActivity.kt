package com.blackclaw.android.ui.scheduled

import android.os.Bundle
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import com.blackclaw.android.base.BaseActivity
import com.blackclaw.android.scheduler.ScheduledTaskManager
import com.blackclaw.android.automation.AutomationEngine
import com.blackclaw.android.automation.AutomationProfileEngine
import com.blackclaw.android.automation.AutomationProfileScheduler
import com.blackclaw.android.automation.AutomationProfileStore
import com.blackclaw.android.automation.AutomationProfileValidator
import com.blackclaw.android.automation.AutomationRuleStore
import com.blackclaw.android.automation.AutomationGeofenceManager
import com.blackclaw.android.automation.LocationSnapshotProvider
import com.blackclaw.android.automation.SavedPlaceStore
import com.blackclaw.android.automation.ExternalAutomationEntrypoint
import com.blackclaw.android.ui.chat.BlaqClawColors
import com.blackclaw.android.ui.chat.ComposeChatActivity
import com.blackclaw.android.ui.chat.ThemeManager
import com.blackclaw.android.ui.chat.ThemeManager.toComposeColors
import com.blackclaw.android.ui.design.ClawGlassBackdrop
import com.blackclaw.android.ui.design.ClawGlassCard
import com.blackclaw.android.ui.design.ClawGlassPill
import com.blackclaw.android.ui.design.ClawReveal
import com.blackclaw.android.ui.onboarding.PermissionExplanationDialog
import com.blackclaw.android.ui.onboarding.PermissionTopic
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduledTasksActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val tc = ThemeManager.getColors()
        window.statusBarColor = tc.toolbarBg
        val colors = with(ThemeManager) { tc.toComposeColors() }
        setContent {
            ClawGlassBackdrop(colors = colors) {
                ScheduledTasksScreen(colors = colors, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledTasksScreen(colors: BlaqClawColors, onBack: () -> Unit) {
    var tasks by remember { mutableStateOf(ScheduledTaskManager.listAll()) }
    var rules by remember { mutableStateOf(AutomationRuleStore.list()) }
    var profiles by remember { mutableStateOf(AutomationProfileStore.list()) }
    var places by remember { mutableStateOf(SavedPlaceStore.list()) }
    // Advanced flows are the canonical Tasker-like surface. Legacy rules remain
    // available for installed users, but should not be the first mental model.
    var selected by remember { mutableStateOf(2) }
    var showAddRule by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var showAgentFlowBuilder by remember { mutableStateOf(false) }
    var showSavePlace by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tasks = ScheduledTaskManager.listAll()
                rules = AutomationRuleStore.list()
                profiles = AutomationProfileStore.list()
                places = SavedPlaceStore.list()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Automations",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = colors.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    if (selected == 0) {
                        IconButton(onClick = { showAddTask = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New task", tint = colors.accent)
                        }
                        if (tasks.isNotEmpty()) {
                            IconButton(onClick = {
                                tasks.forEach { ScheduledTaskManager.cancel(ctx, it.id) }
                                tasks = ScheduledTaskManager.listAll()
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Delete all", tint = colors.textTertiary)
                            }
                        }
                    } else if (selected == 1) {
                        IconButton(onClick = { showAddRule = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New rule", tint = colors.accent)
                        }
                    } else if (selected == 2) {
                        IconButton(onClick = { showAgentFlowBuilder = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Create with BlaqClaw", tint = colors.accent)
                        }
                    } else if (selected == 3) {
                        IconButton(onClick = { showSavePlace = true }) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = "Save this place", tint = colors.accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ClawGlassCard(
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                elevated = false,
                radius = 24.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        Triple("Agenda", tasks.size, "⏱"),
                        Triple("Rules", rules.size, "⚡"),
                        Triple("Flujos", profiles.size, "✦"),
                        Triple("Places", places.size, "⌖"),
                    ).forEachIndexed { index, item ->
                        ClawGlassPill(
                            colors = colors,
                            selected = selected == index,
                            modifier = Modifier.weight(1f),
                            onClick = { selected = index },
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("${item.third} ${item.first}",
                                    color = if (selected == index) colors.accent else colors.textPrimary,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${item.second}", color = colors.textSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            if (selected == 0) {
                if (tasks.isEmpty()) EmptyState(colors, Modifier.weight(1f), onCreate = { showAddTask = true }) else LazyColumn(
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tasks, key = { it.id }) { task -> TaskCard(task, colors) {
                        ScheduledTaskManager.cancel(ctx, task.id); tasks = ScheduledTaskManager.listAll()
                    } }
                }
            } else if (selected == 1) {
                if (rules.isEmpty()) RuleEmptyState(colors, Modifier.weight(1f)) else LazyColumn(
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(rules, key = { it.id }) { rule -> RuleCard(rule, colors,
                        onToggle = {
                            AutomationRuleStore.setEnabled(rule.id, it)
                            AutomationGeofenceManager.sync(ctx)
                            rules = AutomationRuleStore.list()
                        },
                        onRun = { AutomationEngine.fire(ctx, rule) },
                        onDelete = {
                            AutomationRuleStore.delete(rule.id)
                            AutomationGeofenceManager.sync(ctx)
                            rules = AutomationRuleStore.list()
                        }) }
                }
            } else if (selected == 2) {
                if (profiles.isEmpty()) {
                    ProfileEmptyState(
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onCreateWithAgent = { showAgentFlowBuilder = true },
                        onCreateManual = { ctx.startActivity(AutomationProfileEditorActivity.editIntent(ctx)) },
                    )
                } else LazyColumn(
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item(key = "flow-hero") {
                        FlowHeroCard(
                            profiles = profiles,
                            colors = colors,
                            onCreateWithAgent = { showAgentFlowBuilder = true },
                            onCreateManual = { ctx.startActivity(AutomationProfileEditorActivity.editIntent(ctx)) },
                        )
                    }
                    itemsIndexed(profiles, key = { _, item -> item.id }) { index, profile ->
                        ClawReveal(index = index.coerceAtMost(8)) {
                            ProfileCard(profile, colors,
                                onToggle = {
                                    AutomationProfileStore.setEnabled(profile.id, it)
                                    AutomationProfileScheduler.sync(ctx)
                                    AutomationGeofenceManager.sync(ctx)
                                    profiles = AutomationProfileStore.list()
                                },
                                onRun = { AutomationProfileEngine.runNow(ctx, profile) },
                                onEdit = { ctx.startActivity(AutomationProfileEditorActivity.editIntent(ctx, profile.id)) },
                                onDelete = {
                                    AutomationProfileStore.delete(profile.id)
                                    AutomationProfileScheduler.sync(ctx)
                                    AutomationGeofenceManager.sync(ctx)
                                    profiles = AutomationProfileStore.list()
                                })
                        }
                    }
                }
            } else {
                SavedPlacesPane(
                    colors = colors,
                    places = places,
                    onAdd = { showSavePlace = true },
                    onDelete = { place ->
                        if (SavedPlaceStore.delete(place.id)) {
                            AutomationGeofenceManager.sync(ctx)
                            places = SavedPlaceStore.list()
                        }
                    },
                )
            }
        }
    }
    if (showAddRule) AddRuleDialog(colors, onDismiss = { showAddRule = false }) {
        rules = AutomationRuleStore.list(); showAddRule = false
    }
    if (showAddTask) CreateTaskSheet(
        colors = colors,
        onDismiss = { showAddTask = false },
        onSaved = {
            tasks = ScheduledTaskManager.listAll()
            showAddTask = false
        },
    )
    if (showSavePlace) SavePlaceDialog(
        colors = colors,
        onDismiss = { showSavePlace = false },
        onSaved = {
            places = SavedPlaceStore.list()
            showSavePlace = false
        },
    )
    if (showAgentFlowBuilder) AgentFlowBuilderDialog(
        colors = colors,
        onDismiss = { showAgentFlowBuilder = false },
        onCreate = { request ->
            showAgentFlowBuilder = false
            val task = buildString {
                append("Create a BlaqClaw automation based exactly on this user request: ")
                append(request.trim())
                append("\n\nIf the request uses a named place, resolve it first with saved_place and use its place_id; do not invent coordinates. Use the automation_profile tool. Check capabilities if you need to know available triggers or actions, validate the flow before saving it, and save it as a draft so the user can review it under Automations > Flows. Do not enable or run it without explicit local confirmation. Preserve the user's intent and prefer deterministic TOOL actions when a suitable tool exists.")
            }
            ctx.startActivity(Intent(ctx, ComposeChatActivity::class.java).apply {
                putExtra(ExternalAutomationEntrypoint.EXTRA_TASK, task)
            })
        },
    )
}

@Composable
private fun SavedPlacesPane(
    colors: BlaqClawColors,
    places: List<SavedPlaceStore.Place>,
    onAdd: () -> Unit,
    onDelete: (SavedPlaceStore.Place) -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    var permissionTopic by remember { mutableStateOf<PermissionTopic?>(null) }
    val openPermissionSettings = {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = colors.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Named places", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text(
                        "Save any name: home, bedroom, gym, school, or whatever you choose. Flows keep a stable ID and encrypt the coordinates.",
                        color = colors.textSecondary, fontSize = 12.sp,
                    )
                    if (!fine || !background) {
                        Text(
                            if (!fine) "Precise location is required to capture and detect places."
                            else "For reliable background entries/exits with BlaqClaw, allow location access ‘All the time’.",
                            color = colors.textTertiary, fontSize = 11.sp,
                        )
                        TextButton(onClick = {
                            permissionTopic = if (!fine) PermissionTopic.LOCATION else PermissionTopic.BACKGROUND_LOCATION
                        }) { Text("Open BlaqClaw permissions", color = colors.accent) }
                    }
                    Button(onClick = {
                        if (!fine) permissionTopic = PermissionTopic.LOCATION else onAdd()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AddLocationAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save my current location")
                    }
                }
            }
        }
        if (places.isEmpty()) item {
            Text("No places have been saved yet.", color = colors.textSecondary, modifier = Modifier.padding(12.dp))
        }
        items(places, key = { it.id }) { place ->
            ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth(), elevated = false, radius = 18.dp) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = colors.accent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(place.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            buildString {
                                append("Radius ${place.radiusM.toInt()} m · ID ${place.id}")
                                if (place.aliases.isNotEmpty()) append(" · ${place.aliases.joinToString()}")
                            },
                            color = colors.textSecondary, fontSize = 11.sp,
                        )
                    }
                    IconButton(onClick = { onDelete(place) }) {
                        Icon(Icons.Default.Delete, "Delete place", tint = colors.textTertiary)
                    }
                }
            }
        }
    }
    permissionTopic?.let { topic ->
        PermissionExplanationDialog(
            topic = topic,
            colors = colors,
            onDismiss = { permissionTopic = null },
            onContinue = {
                permissionTopic = null
                if (topic == PermissionTopic.LOCATION && ctx is Activity) {
                    ActivityCompat.requestPermissions(
                        ctx,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        4401,
                    )
                } else {
                    openPermissionSettings()
                }
            },
        )
    }
}

@Composable
private fun SavePlaceDialog(
    colors: BlaqClawColors,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("150") }
    var aliases by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = colors.surface.copy(alpha = .98f),
        title = { Text("Save this place", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Give it any name. BlaqClaw will understand it later in your automations.", color = colors.textSecondary, fontSize = 12.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Name: home, bedroom, gym…") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(radius, { radius = it.filter(Char::isDigit) }, label = { Text("Radius in meters") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(aliases, { aliases = it }, label = { Text("Optional aliases, comma-separated") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = colors.textTertiary, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && !saving, onClick = {
                val r = radius.toFloatOrNull()
                if (r == null || r !in 25f..100_000f) { error = "The radius must be between 25 and 100,000 m."; return@Button }
                saving = true; error = null
                scope.launch(Dispatchers.IO) {
                    val location = LocationSnapshotProvider.current(ctx).getOrElse {
                        withContext(Dispatchers.Main) { saving = false; error = it.message ?: "Could not get the location." }
                        return@launch
                    }
                    val result = SavedPlaceStore.save(
                        name = name,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusM = r,
                        aliases = aliases.split(',').map { it.trim() }.filter { it.isNotBlank() },
                    )
                    if (result.isSuccess) {
                        AutomationGeofenceManager.sync(ctx)
                        com.blackclaw.android.service.KeepAliveJobService.schedule(ctx)
                        withContext(Dispatchers.Main) { onSaved() }
                    } else withContext(Dispatchers.Main) {
                        saving = false; error = result.exceptionOrNull()?.message ?: "Could not save."
                    }
                }
            }) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } },
    )
}

@Composable
private fun RuleEmptyState(colors: BlaqClawColors, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚡", fontSize = 42.sp); Spacer(Modifier.height(12.dp))
            Text("No rules yet", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text("Create notification or location rules with +\nor ask BlaqClaw: “when X happens, do Y”",
                color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AgentFlowBuilderDialog(
    colors: BlaqClawColors,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var request by remember { mutableStateOf("") }
    val examples = remember {
        listOf(
            "When I get home, turn on Wi‑Fi and alert me",
            "If I receive a work notification, turn up the volume",
            "Every morning check my battery and alert me if it is low",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface.copy(alpha = .97f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                        .background(colors.accent.copy(alpha = .15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.accent) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Create with BlaqClaw", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Describe it in your own words", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "BlaqClaw will review its capabilities, build the flow, and save it as a draft. Nothing will be enabled without your confirmation.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                OutlinedTextField(
                    value = request,
                    onValueChange = { request = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("E.g. when I connect my headphones, open Spotify and set volume to 40%") },
                    label = { Text("What do you want to automate?") },
                )
                Text("Quick ideas", color = colors.textTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                examples.forEach { example ->
                    Surface(
                        onClick = { request = example },
                        color = colors.accent.copy(alpha = .08f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(.6.dp, colors.accent.copy(alpha = .22f)),
                    ) {
                        Text(example, color = colors.textSecondary, fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = request.isNotBlank(),
                onClick = { onCreate(request) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create draft", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
        },
    )
}

@Composable
private fun FlowHeroCard(
    profiles: List<AutomationProfileStore.Profile>,
    colors: BlaqClawColors,
    onCreateWithAgent: () -> Unit,
    onCreateManual: () -> Unit,
) {
    val active = profiles.count { it.enabled }
    val runs = profiles.sumOf { it.runCount }
    ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth(), radius = 26.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(colors.accent.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center,
                ) { Text("✦", color = colors.accent, fontSize = 26.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Smart workflows", color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Tell BlaqClaw what should happen and what to do; it will build the flow for you to review.",
                        color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowMetric("$active", "active", colors, Modifier.weight(1f))
                FlowMetric("${profiles.size - active}", "drafts", colors, Modifier.weight(1f))
                FlowMetric("$runs", "runs", colors, Modifier.weight(1f))
            }
            Button(
                onClick = onCreateWithAgent,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Create with BlaqClaw", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onCreateManual, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = colors.textSecondary)
                Spacer(Modifier.width(6.dp))
                    Text("Manual editor", color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun FlowMetric(value: String, label: String, colors: BlaqClawColors, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(colors.accent.copy(alpha = .08f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Column {
            Text(value, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(label, color = colors.textSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProfileEmptyState(
    colors: BlaqClawColors,
    modifier: Modifier = Modifier,
    onCreateWithAgent: () -> Unit,
    onCreateManual: () -> Unit,
) {
    Box(modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✦", color = colors.accent, fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text("Your first automation", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("Try: “when I get home, turn on Wi‑Fi and alert me”.\nBlaqClaw can create it as a draft for you to review before enabling it.",
                    color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(18.dp))
                Button(onClick = onCreateWithAgent, shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Create with BlaqClaw", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onCreateManual) {
                    Text("Open manual editor", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: AutomationProfileStore.Profile, colors: BlaqClawColors,
                        onToggle: (Boolean) -> Unit, onRun: () -> Unit, onEdit: () -> Unit,
                        onDelete: () -> Unit) {
    val validationErrors = remember(profile) { AutomationProfileValidator.validate(profile) }
    ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth(), radius = 22.dp, elevated = false) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🧩", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${if (profile.enabled) "ENABLED" else "DRAFT"} · ${profile.triggers.size} trigger(s) · ${profile.actions.size} action(s)",
                        color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Switch(checked = profile.enabled, onCheckedChange = onToggle)
            }
            if (profile.description.isNotBlank()) {
                Text(profile.description, color = colors.textSecondary, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 5.dp))
            }
            if (validationErrors.isNotEmpty()) {
            Text("Needs review: ${validationErrors.first()}", color = colors.textTertiary,
                    fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Text("Limit: ${if (profile.maxRunsPerDay == 0) "no daily limit" else "${profile.maxRunsPerDay}/day"} · cooldown ${profile.cooldownMs / 1000}s · maximum ${profile.maxRuntimeMs / 1000}s",
                color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            if (profile.lastStatus != "never") {
                Text("Last result: ${profile.lastStatus}${profile.lastError.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                    color = if (profile.lastStatus == "success") colors.accent else colors.textTertiary,
                    fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(enabled = validationErrors.isEmpty(), onClick = onRun) {
                    Text("Test", color = if (validationErrors.isEmpty()) colors.accent else colors.textTertiary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = colors.accent)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = colors.textTertiary) }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: AutomationRuleStore.Rule, colors: BlaqClawColors,
                     onToggle: (Boolean) -> Unit, onRun: () -> Unit, onDelete: () -> Unit) {
    ClawGlassCard(colors = colors, modifier = Modifier.fillMaxWidth(), radius = 20.dp, elevated = false) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (rule.trigger == AutomationRuleStore.Trigger.NOTIFICATION) "🔔" else "📍", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(rule.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(rule.trigger.name.lowercase().replace('_', ' '), color = colors.accent, fontSize = 11.sp)
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
            if (rule.match.isNotBlank()) Text("IF it contains: ${rule.match}", color = colors.textSecondary, fontSize = 12.sp)
            Text("THEN: ${rule.actionText}", color = colors.textPrimary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRun) { Text("Test", color = colors.accent) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = colors.textTertiary) }
            }
        }
    }
}

@Composable
private fun AddRuleDialog(colors: BlaqClawColors, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }; var match by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }; var pkg by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }; var lon by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf(AutomationRuleStore.Trigger.NOTIFICATION) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = colors.surface,
        title = { Text("New automation", color = colors.textPrimary) },
        text = { Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AutomationRuleStore.Trigger.entries.forEach { t -> FilterChip(selected = trigger == t,
                    onClick = { trigger = t }, label = { Text(when(t) {
                        AutomationRuleStore.Trigger.NOTIFICATION -> "Notification"
                        AutomationRuleStore.Trigger.LOCATION_ENTER -> "Arrive"
                        AutomationRuleStore.Trigger.LOCATION_EXIT -> "Leave"
                    }, fontSize = 11.sp) }) }
            }
            if (trigger == AutomationRuleStore.Trigger.NOTIFICATION) {
                OutlinedTextField(match, { match = it }, label = { Text("Contact or text") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pkg, { pkg = it }, label = { Text("Optional package (com.whatsapp)") }, modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedTextField(lat, { lat = it }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(lon, { lon = it }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(action, { action = it }, label = { Text("Actions in order") }, minLines = 3,
                placeholder = { Text("Turn off mobile data, turn on Wi-Fi, and verify") }, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && action.isNotBlank() &&
            (trigger == AutomationRuleStore.Trigger.NOTIFICATION || (lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null)),
            onClick = {
                AutomationRuleStore.create(name, trigger, match, pkg, action,
                    lat.toDoubleOrNull() ?: 0.0, lon.toDoubleOrNull() ?: 0.0)
                onSaved()
            }) { Text("Save", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun EmptyState(colors: BlaqClawColors, modifier: Modifier = Modifier, onCreate: () -> Unit = {}) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text("⏰", fontSize = 36.sp) }
            Spacer(Modifier.height(20.dp))
            Text("No scheduled tasks", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Ask the AI \"remind me about X tomorrow at 9\"\nand it will appear here.",
                fontSize = 13.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onCreate, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Create task", color = colors.background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class QuickTaskWhen { IN_30_MIN, TODAY_18, TOMORROW_09, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskSheet(
    colors: BlaqClawColors,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = remember { System.currentTimeMillis() }
    var text by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ScheduledTaskManager.Mode.TASK) }
    var selectedWhen by remember { mutableStateOf(QuickTaskWhen.IN_30_MIN) }
    var customTime by remember { mutableStateOf<Long?>(null) }
    var recurrence by remember { mutableStateOf(ScheduledTaskManager.Recurrence.ONCE) }
    var error by remember { mutableStateOf<String?>(null) }

    fun clock(hour: Int, minute: Int, dayOffset: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (dayOffset == 0 && calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun openCustomPicker() {
        val seed = Calendar.getInstance()
        val picker = DatePickerDialog(context, { _, year, month, day ->
            val date = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            TimePickerDialog(context, { _, hour, minute ->
                date.set(Calendar.HOUR_OF_DAY, hour)
                date.set(Calendar.MINUTE, minute)
                date.set(Calendar.SECOND, 0)
                date.set(Calendar.MILLISECOND, 0)
                customTime = date.timeInMillis
                selectedWhen = QuickTaskWhen.CUSTOM
            }, seed.get(Calendar.HOUR_OF_DAY), seed.get(Calendar.MINUTE), true).show()
        }, seed.get(Calendar.YEAR), seed.get(Calendar.MONTH), seed.get(Calendar.DAY_OF_MONTH))
        picker.datePicker.minDate = System.currentTimeMillis()
        picker.show()
    }

    val triggerAtMs = when (selectedWhen) {
        QuickTaskWhen.IN_30_MIN -> now + 30 * 60_000L
        QuickTaskWhen.TODAY_18 -> clock(18, 0)
        QuickTaskWhen.TOMORROW_09 -> clock(9, 0, 1)
        QuickTaskWhen.CUSTOM -> customTime ?: (now + 30 * 60_000L)
    }
    val dateFormat = remember { SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()) }
    val triggerLabel = dateFormat.format(Date(triggerAtMs))
    val eveningTrigger = clock(18, 0)
    val eveningLabel = if (eveningTrigger - now < 12 * 60 * 60_000L) "Today 18:00" else "Tomorrow 18:00"
    val canSave = text.trim().isNotEmpty() && triggerAtMs > System.currentTimeMillis()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textTertiary) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(colors.accent.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center,
                ) { Text("✦", color = colors.accent, fontSize = 25.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("New task", color = colors.textPrimary, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("It will run even if you close BlaqClaw", color = colors.textSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textTertiary)
                }
            }

            Text("What should it do?", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("e.g. check the weather and alert me if it will rain") },
                supportingText = { Text("Write it as you would say it to BlaqClaw", color = colors.textTertiary) },
                shape = RoundedCornerShape(16.dp),
            )

            Text("Execution type", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTaskChoice(
                    colors = colors,
                    selected = mode == ScheduledTaskManager.Mode.TASK,
                    icon = Icons.Default.PlayArrow,
                    title = "Run",
                    subtitle = "BlaqClaw will act",
                    modifier = Modifier.weight(1f),
                    onClick = { mode = ScheduledTaskManager.Mode.TASK },
                )
                QuickTaskChoice(
                    colors = colors,
                    selected = mode == ScheduledTaskManager.Mode.CHAT,
                    icon = Icons.Default.NotificationsActive,
                    title = "Notify me",
                    subtitle = "Notification only",
                    modifier = Modifier.weight(1f),
                    onClick = { mode = ScheduledTaskManager.Mode.CHAT },
                )
            }

            Text("When?", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTaskChoice(
                    colors = colors, selected = selectedWhen == QuickTaskWhen.IN_30_MIN,
                    icon = Icons.Default.Timer, title = "30 min", subtitle = "From now",
                    modifier = Modifier.weight(1f), onClick = { selectedWhen = QuickTaskWhen.IN_30_MIN },
                )
                QuickTaskChoice(
                    colors = colors, selected = selectedWhen == QuickTaskWhen.TODAY_18,
                    icon = Icons.Default.WbSunny, title = eveningLabel, subtitle = "Next time slot",
                    modifier = Modifier.weight(1f), onClick = { selectedWhen = QuickTaskWhen.TODAY_18 },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTaskChoice(
                    colors = colors, selected = selectedWhen == QuickTaskWhen.TOMORROW_09,
                    icon = Icons.Default.Event, title = "Tomorrow 09:00", subtitle = "At the start of the day",
                    modifier = Modifier.weight(1f), onClick = { selectedWhen = QuickTaskWhen.TOMORROW_09 },
                )
                QuickTaskChoice(
                    colors = colors, selected = selectedWhen == QuickTaskWhen.CUSTOM,
                    icon = Icons.Default.Schedule, title = "Choose date", subtitle = "Custom",
                    modifier = Modifier.weight(1f), onClick = ::openCustomPicker,
                )
            }

            Text("Repeat", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ScheduledTaskManager.Recurrence.ONCE to "Once",
                    ScheduledTaskManager.Recurrence.DAILY to "Every day",
                    ScheduledTaskManager.Recurrence.WEEKLY to "Every week",
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = recurrence == value,
                        onClick = { recurrence = value },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = .35f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Preview", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${if (mode == ScheduledTaskManager.Mode.TASK) "🤖 BlaqClaw will act" else "🔔 Notify me"} · $triggerLabel",
                        color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text.ifBlank { "Write a task to see it here" },
                        color = if (text.isBlank()) colors.textTertiary else colors.textSecondary,
                        fontSize = 13.sp,
                    )
                    if (recurrence != ScheduledTaskManager.Recurrence.ONCE) {
                        Text(
                            if (recurrence == ScheduledTaskManager.Recurrence.DAILY) "Repeats every day" else "Repeats every week",
                            color = colors.accent, fontSize = 11.sp,
                        )
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            Button(
                onClick = {
                    if (text.isBlank()) {
                        error = "Write what you want BlaqClaw to do."
                    } else if (triggerAtMs <= System.currentTimeMillis()) {
                        error = "Choose a future time."
                    } else {
                        val saved = ScheduledTaskManager.schedule(
                            context = context,
                            mode = mode,
                            text = text.trim(),
                            triggerAtMs = triggerAtMs,
                            recurrence = recurrence,
                        )
                        if (saved != null) {
                            onSaved()
                        } else {
                            error = "Could not securely save the task. Unlock the device and try again."
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.surface,
                    disabledContentColor = colors.textTertiary,
                ),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Schedule task", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickTaskChoice(
    colors: BlaqClawColors,
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (selected) colors.accent.copy(alpha = .16f) else colors.surface,
        label = "task-choice-container",
    )
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 1.2.dp else .5.dp,
            if (selected) colors.accent else colors.aiBubbleBorder,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textSecondary,
                modifier = Modifier.size(22.dp),
            )
            Text(title, color = if (selected) colors.accent else colors.textPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = colors.textSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduledTaskManager.ScheduledTask,
    colors: BlaqClawColors,
    onCancel: () -> Unit,
) {
    val df = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    val recurStr = when (task.recurrence) {
        ScheduledTaskManager.Recurrence.ONCE -> "once"
        ScheduledTaskManager.Recurrence.HOURLY -> "every hour"
        ScheduledTaskManager.Recurrence.DAILY -> "every day"
        ScheduledTaskManager.Recurrence.WEEKLY -> "every week"
        ScheduledTaskManager.Recurrence.INTERVAL -> "every ${task.intervalMs / 60_000}m"
    }
    val modeIcon = if (task.mode == ScheduledTaskManager.Mode.TASK) "🤖" else "💬"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.aiBubbleBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Text(modeIcon, fontSize = 22.sp) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(df.format(Date(task.triggerAtMs)),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(recurStr, fontSize = 11.sp, color = colors.accent)
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.textTertiary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(color = colors.aiBubble, shape = RoundedCornerShape(8.dp)) {
                Text("\"${task.text}\"", fontSize = 12.sp, color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

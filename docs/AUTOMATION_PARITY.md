# BlackClaw Automation — design, parity, and limits

Estado: 2026-09-03 · motor de perfiles schema v2.

This document describes BlackClaw's automation engine and the comparison that guided its design against Tasker, MacroDroid, and Automate. The goal is a powerful, predictable Android automation core without turning the LLM into the event loop: the model can create or edit a flow, but triggers, conditions, limits, and deterministic actions run locally.

## Principios

1. **The agent designs; Android executes.** A persisted profile does not need an LLM connection to evaluate events or run deterministic tools.
2. **Places are semantic and reusable.** They are not limited to `home` and `work`: users can define `partner's home`, `my room`, `gym`, `school`, `warehouse`, and more.
3. **Stable identity, private coordinates.** Profiles reference `place_id`; latitude/longitude remain in encrypted storage and the local runtime. IDs sent to Play Services are opaque hashes, not serialized coordinates.
4. **Fail closed.** Invalid profiles do not run. Sensitive actions retain explicit confirmation. Webhooks and external automation use BlackClaw's hardened policies/tokens.
5. **Hybrid reliability.** Google Play Services geofencing is the low-power primary path; the existing location checker is a best-effort fallback when Play Services or background permission is unavailable.

## Semantic places

`SavedPlaceStore` guarda un documento cifrado mediante `SecretStore` con:

- ID estable.
- Nombre libre elegido por el usuario.
- Aliases opcionales.
- Latitud/longitud.
- Radio.
- SSIDs asociados opcionales.
- Metadatos de creación/actualización.

Name resolution ignores case, accents, and punctuation for comparison while preserving the original name. It first looks for an exact normalized match and only uses a partial match when the result is unique; ambiguity requires resolving by exact name or ID instead of guessing.

Conversation examples:

```text
User: BlackClaw, this place is my partner's home.
BlackClaw: saved_place(operation=save_here, name="partner's home")

User: When I arrive at my partner's home, set the volume to 30%.
BlackClaw: saved_place(operation=resolve, name="casa de mi novia")
           automation_profile(... trigger location_enter {place_id: ...} ...)

User: This is my room; also save it as bedroom.
BlackClaw: saved_place(operation=save_here, name="my room", aliases="bedroom")
```

El modelo recibe nombre, ID, aliases y radio, pero no necesita recibir las coordenadas guardadas para volver a usar un lugar.

## Geofencing and location

The runtime registers up to 100 active targets, Android's documented per-app/user geofence limit. Identical targets are deduplicated and combine their enter/exit transitions.

- Primary: Google Play Services `GeofencingClient`.
- Fallback: last known location on the `GeofenceChecker` tick.
- Re-armed on app start, device restart, and profile/rule/place changes.
- No synthetic `initial enter/exit` occurs when registering a geofence; a confirmed transition is expected.
- `ACCESS_BACKGROUND_LOCATION` is requested only so location automation can reliably trigger BlackClaw in the background.
- The UI explains the permission and offers access to Settings; it does not claim the geofence is active when permission is missing.
- Los request IDs de Play Services son hashes opacos (`bcg:...`).

Android recomienda normalmente radios de aproximadamente 100–150 m para geofencing general; BlackClaw permite radios menores cuando el usuario sabe que dispone de buena precisión interior, pero la UI usa 150 m como valor inicial.

Referencias Android:

- https://developer.android.com/develop/sensors-and-location/location/geofencing
- https://developer.android.com/develop/sensors-and-location/location/battery/scenarios
- https://developer.android.com/develop/background-work/background-tasks/broadcasts

## Trigger catalog

El schema v2 soporta:

- `manual`
- `time`
- `interval`
- `notification`
- `location_enter`
- `location_exit`
- `app_foreground`
- `app_closed`
- `connectivity`
- `battery`
- `charging`
- `screen`
- `headset`
- `bluetooth`
- `wifi`
- `call_state`
- `sms_received`
- `boot`
- `airplane_mode`
- `power_save`
- `device_idle`
- `usb`
- `storage`
- `timezone`
- `locale`
- `webhook`

Triggers in the same profile are OR: any one can start profile evaluation. Schedules and intervals use `AlarmManager`; connectivity and several modern states use context-registered receivers because Android no longer allows relying on an implicit manifest receiver.

## Conditions

Current types:

- time window
- weekday
- app
- connectivity/transport
- battery level
- charging
- screen
- variable
- notification
- location inside/outside a place
- Wi-Fi/SSID
- Bluetooth
- headset
- airplane mode
- power saving
- device idle

The global combination can be:

- `ALL`
- `ANY`
- `NONE`
- `XOR`

Each condition can also be inverted with `negate`. For more specific branches, an `IF` action can evaluate a condition and run `then`/`else`; `IF` and `LOOP` are depth- and count-limited to prevent unbounded flows.

## Actions, variables, and templates

Acciones:

- `tool`
- `agent_task`
- `run_routine`
- `notify`
- `set_variable`
- `wait`
- `if`
- `loop`

Persistent engine variables are encrypted. Variable conditions support `exists`, `equals`, `not_equals`, `contains`, `regex`, `gt`, `gte`, `lt`, and `lte`.

Las cadenas de acciones pueden usar:

```text
{{event.key}}
{{var.nombre}}
{{profile.name}}
{{profile.id}}
{{event.type}}
{{now_ms}}
```

Expansion also applies recursively to tool parameters. Location events do not expose latitude/longitude to these templates.

## Execution limits

Each profile can define:

- cooldown
- maximum runs per day
- maximum run time
- concurrency policy `SKIP_IF_RUNNING`, `QUEUE`, or `REPLACE`
- prior confirmation for sensitive actions

Additionally:

- loops: maximum 20 iterations
- nested IF/LOOP depth: bounded
- arbitrary privileged tools are excluded from the safe automation catalog
- the execution origin is marked `AUTOMATION` so `ToolRiskPolicy` maintains the same security boundary

## Comparison with other apps

The comparison used public documentation current as of 2026-09-03.

| Área | Tasker | MacroDroid | Automate | BlackClaw |
|---|---|---|---|---|
| Evento → acciones | Profiles/Contexts → Tasks | Trigger → Actions | Bloques conectados | Trigger(s) → acciones ordenadas |
| Multiple events | Yes | Yes | Yes, by blocks/fibers | Yes, OR triggers |
| Conditions | Contexts/states | Constraints | Decision blocks | 16 types + ALL/ANY/NONE/XOR + negate |
| Branches | Task/If | If/Else + constraints | YES/NO graph | Bounded IF then/else |
| Loops | Yes | Yes | Graph/blocks | Bounded LOOP |
| Variables | Amplias, locales/globales | Tipadas y globales/locales | Por fibra + expresiones | Persistentes cifradas + comparadores + plantillas |
| Geofences reutilizables | Location contexts | Zonas con nombre | Location blocks | Lugares semánticos libres + aliases + ID estable |
| Enter/exit | Sí | Sí | Sí | Sí, Play Services + fallback |
| Visual UI | Profile/task editor | Macro editor | Block graph | WHEN/IF/DO builder + natural-language creation |
| Concurrencia | Dependiente de tasks | Macros concurrentes | Fibras | skip/queue/replace |
| Extensibilidad | Plugins/Intents | Plugins/Intents | Extension apps | ToolRegistry + webhooks + Tasker/ADB + agente |
| Restart | Profiles/tasks reactivate | Persistent macros | Persisted fibers | schedules/geofences re-arm; encrypted stores persist |
| Place privacy | Depends on app/config | Local geofence data | Flow data | encrypted coordinates, prompts without coordinates, opaque IDs |

Referencias de comparación:

- Tasker Userguide: https://tasker.joaoapps.com/userguide/en/
- MacroDroid Geofence Trigger: https://macrodroidforum.com/wiki/index.php/Trigger%3A_Geofence_Trigger
- MacroDroid Geofences: https://macrodroidforum.com/wiki/index.php/Geofences
- MacroDroid Geofence Constraint: https://macrodroidforum.com/wiki/index.php/Constraint%3A_Geofence
- Automate Flow/Fiber: https://www.llamalab.com/automate/doc/flow.html
- Automate Variables: https://www.llamalab.com/automate/doc/variable.html

## What BlackClaw does not try to copy yet

“Parity” here means a comparable automation core, not duplicating decades of ecosystem one-to-one.

- Tasker still has a large plugin/Scenes ecosystem.
- MacroDroid has more specialized types and complex variables such as arrays/dictionaries.
- Automate has an arbitrary graph with fibers/forks and more general expressions.
- BlackClaw deliberately keeps IF/LOOP and privileged execution bounded so an AI-generated automation cannot become an unlimited background shell.

BlackClaw's advantage is that users do not need to translate their intent into an editor first: they can say “this place is X” or “when I arrive at X, if it is night and I am not charging, do Y”; the agent transforms it into a deterministic structure that then runs locally.

## Archivos principales

```text
app/src/main/java/com/blackclaw/android/automation/
  AutomationProfileStore.kt
  AutomationProfileEngine.kt
  AutomationProfileScheduler.kt
  AutomationGeofenceManager.kt
  AutomationSystemReceiver.kt
  LocationSnapshotProvider.kt
  SavedPlaceStore.kt

app/src/main/java/com/blackclaw/android/tool/impl/
  AutomationProfileTool.kt
  AutomationRuleTool.kt
  SavedPlaceTool.kt

app/src/main/java/com/blackclaw/android/ui/scheduled/
  ScheduledTasksActivity.kt
  AutomationProfileEditorActivity.kt
```

## Ejemplo de perfil

```json
{
  "name": "Llegada a casa de mi novia",
  "triggers": [
    {"type": "location_enter", "params": {"place_id": "<id-estable>"}}
  ],
  "conditions": [
    {"type": "time_window", "params": {"start": "18:00", "end": "02:00"}},
    {"type": "power_save", "params": {"value": false}}
  ],
  "condition_logic": "all",
  "actions": [
    {"type": "tool", "params": {"tool": "set_volume", "params": {"level": 30}}},
    {"type": "notify", "params": {"text": "Llegaste a {{event.place}}"}}
  ]
}
```

JSON is a transport/editing format; ordinary users can create the same flow by speaking to BlackClaw or using **Automations → Flows/Places**.

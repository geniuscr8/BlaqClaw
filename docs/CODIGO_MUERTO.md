# Dead code inventory — 2026-08-06

This audit does not delete code. It separates genuinely unreachable code from
common Android false positives (manifest entries, reflection, serialization, and
constants that the compiler inlines).

## Method and scope

1. Exact references in production, tests, the manifest, XML, assets, and documentation.
2. Review of dynamic-reachability points: manifest components, tool registration,
   reflection, `ServiceLoader`, class loading, and dynamic resource resolution.
3. Confirmation with the R8 (`usage.txt`) and `shrinkResources`
   (`resources.txt`) reports from a minified `release` variant.
4. Inspection of the flow that replaced each candidate when one exists.

The app has no dynamic class loading, `ServiceLoader`, or
`Resources.getIdentifier`. The only reflection found targets `android.media`
framework classes in `RecognizeSongTool`. Tools are instantiated explicitly in
`ToolRegistry.registerAllTools`; therefore an unregistered `BaseTool` cannot be
invoked by the agent. Persistent JSON is converted manually with `JSONObject`,
not through generic Gson for these candidates.

R8 is reachability evidence, not the only proof: a constants class may appear
as removed because its `const val` values were already inlined into callers.

## Removed

These entries have no production caller, are not Android components or dynamic
load targets, and R8 marks them unreachable in `release`.

| Archivo / símbolo | Evidencia adicional |
|---|---|
| `base/AbstractBaseActivity.kt` | No tiene subclases; la base usada es `BaseActivity`. |
| `adb/AdbIo.kt` | Framing ADB sin callers; el flujo ADB vivo usa libadb. |
| `channel/wechat/WeChatTypes.kt` → `SendMessageReq` | Nunca se construye ni serializa; `WeChatSender` construye el payload con `JSONObject`. |
| `utils/func/XFunc0.java` | Interfaz callback sin implementación ni caller. |
| `utils/func/XFunc1.java` | Interfaz callback genérica sin implementación ni caller. |
| `game/GameTouchRecorder.kt` | Grabador legacy mediante `getevent`, sin callers. El tool vivo abre `GameAutoclickerOverlay`, sin ADB/Shizuku. |
| `tool/AppCache.kt` | Caché de apps sin callers. |
| `agent/TaskShortcuts.kt` | Enrutador directo de comandos sin caller; el agente usa el pipeline y tools actuales. |
| `tool/impl/mobile/SearchAppInStoreTool.java` | El único tool de su clase que no se registra en `ToolRegistry`; tampoco aparece por nombre en prompts, pruebas o assets. |

## Partially removed

`ui/assist/QuickAssistTaskUi.kt` retains two generations of UI. The activity
only calls `QuickAssistTaskReducer.toolLabel()`. R8 removed the rest of its
reducer and card types.

`AssistTaskState`, `AssistStepState`, `AssistTaskStep`, `AssistTaskCard`,
`start`, `reduce`, `cancel`, `fail`, and their helpers and tests can be removed.
`toolLabel` must be kept (or moved to a small object) because
`QuickAssistActivity` uses it for spoken/visible state.

`TouchGestureParser` and `TouchDeviceInfo` were also removed with their tests:
after `GameTouchRecorder` disappeared, they had no production consumers.

## Removed resources

There are no references in code/XML/the manifest or dynamic resource resolution.
Additionally, `app/build/outputs/mapping/release/resources.txt` marks each one
as `is not reachable` with `shrinkResources` enabled.

```
drawable/bg_btn_send_circle.xml       drawable/bg_bubble_assistant.xml
drawable/bg_bubble_user.xml           drawable/bg_chat_input.xml
drawable/bg_system_chip.xml            drawable/bg_tag_recommend.xml
drawable/blackclaw_icon_small.png      drawable/ic_checkbox_checked.xml
drawable/ic_lan_config.xml             drawable/ic_menu_hamburger.xml
drawable/ic_rocket.xml                 drawable/ic_send_arrow.xml
drawable/ic_visibility_on.xml          drawable/icon_book_open.xml
drawable/icon_credits.xml              drawable/icon_current_model.xml
drawable/icon_exit.xml                 drawable/icon_up_circle.xml
drawable/icon_waring.xml               drawable/splash_background.xml
font/syncopate_bold.ttf                layout/activity_settings.xml
layout/activity_theme.xml              menu/menu_chat_toolbar.xml
```

## False positives and exclusions

Do not delete something only because it appears lightly used:

- `DiscordConstants` appears in `usage.txt`, but is not dead code: its
  constants are used by Discord clients and Kotlin inlines them.
- Activities, services, receivers y providers declarados en `AndroidManifest.xml`.
- Tools registrados en `ToolRegistry`: aunque el buscador textual no tenga un
  caller tradicional, el LLM los invoca por nombre.
- DTOs que participen en Gson/reflexión y assets/playbooks sin una revisión de
  su cargador concreto.
- Una clase que R8 enumera junto con algunos miembros: puede significar que solo
  eliminó esos miembros, no que la clase completa sea innecesaria.

## Pending verification

When stabilizing the next improvements, run `testDebugUnitTest` and a
`release` build, followed by a visual smoke test of chat, settings, and splash.
They were not run during this removal by explicit request.

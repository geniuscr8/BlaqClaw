<div align="center">

# BlaqClaw

**A local-first AI assistant that can operate your Android phone.**

BlaqClaw combines an LLM with Android accessibility, OCR, voice, automation,
and device-control tools. Describe what you want in plain English and BlaqClaw
can inspect the screen, choose an action, verify the result, and continue until
the task is complete.

[![Platform](https://img.shields.io/badge/platform-Android%209%2B-3DDC84?logo=android&logoColor=white)](#requirements)
[![Build](https://github.com/geniuscr8/BlaqClaw/actions/workflows/build.yml/badge.svg)](https://github.com/geniuscr8/BlaqClaw/actions/workflows/build.yml)
[![Language](https://img.shields.io/badge/Kotlin%20%2F%20Java-7F52FF?logo=kotlin&logoColor=white)](#technology)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

### [Download the latest APK](https://github.com/geniuscr8/BlaqClaw/releases/latest)

<sub>BlaqClaw is currently distributed as a sideloaded beta APK. Android 9+ is required.</sub>

</div>

> **Beta notice:** BlaqClaw is under active development. Android behavior varies
> between manufacturers, and some capabilities require additional permissions or
> privileged access. Review every permission and automation before enabling it.

## Release notes

### v1.3.5

- Completed the English-only migration for command parsing, voice recognition,
  wake words, emergency commands, aliases, and training flows.
- Removed the bundled Spanish voice model and Spanish runtime command paths.
- Updated tests and release validation for the English command experience.

### v1.3.4

- Added the Control Center for setup health, encrypted backup and restore,
  automation templates, privacy checks, and local action history.
- Refreshed the BlaqClaw theme and launcher identity.
- Hardened signed APK/AAB releases with tests, lint, checksums, mapping files,
  and stable universal APK publishing.
- Added dependency update monitoring and CodeQL security scanning.

## Contents

- [What BlaqClaw does](#what-blaqclaw-does)
- [Example requests](#example-requests)
- [Models](#models)
- [Privacy and safety](#privacy-and-safety)
- [Requirements](#requirements)
- [Installation](#installation)
- [Building](#building)
- [Project structure](#project-structure)
- [Contributing](#contributing)
- [Reporting bugs](#reporting-bugs)
- [License](#license)

For the production privacy overview, see [`PRIVACY.md`](PRIVACY.md). For signed
release operations, see [`docs/PRODUCTION_RELEASE_CHECKLIST.md`](docs/PRODUCTION_RELEASE_CHECKLIST.md).

## What BlaqClaw does

### Control apps through the Android UI

BlaqClaw uses the accessibility tree, screen capture, and OCR to work with
installed apps without requiring a custom integration for every app. It can:

- open apps, navigate, tap, long-press, swipe, scroll, drag, and type;
- read visible text and controls, including custom-rendered screens through OCR;
- verify tool results and screen changes before reporting success;
- use deep links and deterministic shortcuts for common navigation, music,
  maps, messaging, shopping, and ride-hailing tasks.

### Voice and assistant mode

- Offline wake-word and speech recognition through Vosk.
- Streaming responses with text-to-speech support.
- Optional Android default-assistant integration, including lock-screen flows
  where supported by the device.
- Continuous conversation, whisper mode, and voice activity detection.

### Built-in assistant hub

BlaqClaw includes native surfaces for:

- reminders and alarms, including recurring items and notification delivery;
- notes, calendar events, alerts, shopping lists, and promises;
- finance entries, budgets, recurring bills, and spending summaries;
- location-based reminders and a home-screen widget;
- draft replies that require the user to review and send them.

### Automation

Create and manage automations from the app or through natural-language requests.
The automation engine supports scheduled tasks, notifications, app and device
state, connectivity, battery, calls, messages, boot, location, geofences,
conditions, variables, branches, cooldowns, run limits, and safe concurrency
modes.

Automations run locally after they are configured. Sensitive actions require
explicit confirmation or local approval, depending on the operation.

### Android Auto and remote channels

- A voice-first Android Auto surface for navigation, music, calls,
  notifications, and questions.
- Optional remote control through supported Telegram, Discord, and WeChat
  channels.
- External automation entry points for compatible Tasker, MacroDroid, and ADB
  workflows.

### Device tools and privileged access

The built-in tool catalog includes device status, notifications, contacts,
calendar, SMS, calls, files, clipboard, network, volume, brightness, Wi-Fi,
Bluetooth, flashlight, QR utilities, translation, web access, and more.

Optional privileged backends provide additional capabilities:

- self-paired ADB over Android Wireless Debugging;
- Shizuku, when installed and authorized;
- a persistent internal terminal;
- guarded shell and game-control operations.

Privileged access is optional. The core chat and many app-control features do
not require root or a computer.

## Example requests

```text
Open WhatsApp and draft a message to Mom saying I will be 20 minutes late.

Read my notifications and summarize anything important.

Set a reminder to call the dentist tomorrow at 5 PM.

Search YouTube for lo-fi music and play the first result.

Turn on battery saver if my battery is below 20 percent.

Every weekday at 8 AM, read the weather to me.

Save this location as home and remind me to buy groceries when I arrive.
```

BlaqClaw asks for confirmation when an action could be destructive, costly,
privacy-sensitive, or difficult to undo. It does not treat model text alone as
proof that an action succeeded.

## Models

BlaqClaw supports local and cloud model workflows:

- **Local models:** compatible LiteRT-LM models can run on the device. Local
  inference is useful when privacy or offline operation matters.
- **BlaqClaw Free:** the app can expose the configured no-key free-model route
  when that service is available.
- **Bring your own provider:** configure OpenAI, Anthropic, Google Gemini,
  Groq, DeepSeek, Cerebras, or another OpenAI-compatible endpoint.
- **Automatic routing:** choose a configured cloud model when online and fall
  back to a downloaded local model when offline, where supported.

Model availability, memory requirements, provider limits, and pricing can
change. Review the active model and endpoint in **Settings → Model** before
using a cloud provider.

## Privacy and safety

In local mode, inference can run entirely on the phone. In cloud mode, prompts,
screen context, and tool data may be sent to the provider you selected. Read the
provider's terms before enabling cloud access.

API keys and sensitive app data are stored in local app storage. Never commit
keys, keystores, `local.properties`, or provider credentials to Git.

Several capabilities require powerful permissions:

- **Accessibility:** read and operate the visible UI.
- **Notification access:** inspect notifications and support proactive flows.
- **Overlay:** display the floating control and assistant surfaces.
- **Microphone:** voice input and wake-word features.
- **Screen capture:** OCR and visual perception.
- **Location:** location reminders and geofences.
- **Wireless debugging or Shizuku:** optional privileged tools.

Grant only the permissions required for the features you intend to use. Review
automation profiles and proactive actions regularly.

## Requirements

- Android 9 / API 28 or newer.
- Android Studio or the Android command-line SDK for development.
- JDK 17 for building.
- Android SDK Platform 36 and Build Tools 35.0.0 for the project build.
- Approximately 4 GB of RAM for smaller local models; larger models require
  substantially more memory.

## Installation

1. Download a signed APK from the [Releases](https://github.com/geniuscr8/BlaqClaw/releases)
   page or download the `complete-debug-apks` artifact from a successful
   [GitHub Actions build](https://github.com/geniuscr8/BlaqClaw/actions/workflows/build.yml).
2. Enable installation from the source you are using when Android asks.
3. Open BlaqClaw and complete the guided setup.
4. Enable only the permissions and optional services needed by your workflow.
5. Configure a local model or a cloud provider in **Settings → Model**.

Debug APKs are intended for testing and are not a substitute for a signed
release build.

## Building

Clone the repository and build the debug APK:

```bash
git clone https://github.com/geniuscr8/BlaqClaw.git
cd BlaqClaw
./gradlew assembleDebug
```

The output is written to:

```text
app/build/outputs/apk/debug/
```

Run unit tests and lint with:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Install a debug APK on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/*.apk
```

### Release signing

Release builds require a keystore. Supply signing values through environment
variables or local signing configuration; never commit the keystore or
passwords:

```text
KEYSTORE_FILE=/absolute/path/to/release.keystore
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Then run:

```bash
./gradlew assembleRelease
```

See [`RELEASING.md`](RELEASING.md) for the release checklist.

### GitHub Actions

The [`Build Debug APK`](.github/workflows/build.yml) workflow runs on pushes
and pull requests targeting `main` or `master`, and can also be started
manually. It performs the following checks:

1. installs the Android SDK;
2. builds the debug APK;
3. uploads the APK as the `complete-debug-apks` artifact;
4. runs unit tests;
5. runs Android lint and uploads diagnostic reports.

The APK is uploaded before tests so a test failure does not discard a usable
debug build. A red workflow still means the code needs attention before it is
treated as a verified release.

## Project structure

```text
app/src/main/java/com/blackclaw/android/
├── agent/        Model clients, prompts, agent loop, tool selection
├── assistant/    Reminders, alarms, notes, finance, and assistant storage
├── automation/   Profiles, triggers, conditions, variables, and runtime
├── car/          Android Auto integration
├── channel/      Remote messaging channels
├── perception/   Screen capture and OCR
├── proactive/    Notification-driven assistant actions
├── service/      Accessibility, notification, and foreground services
├── shizuku/      Optional privileged backend
├── tool/         Device, UI, network, file, and app tools
├── ui/           Compose screens, settings, chat, themes, and skills
└── utils/        Storage, logging, matching, and shared helpers

app/src/main/res/       Android resources and localized strings
app/src/test/            JVM unit tests
docs/                    Architecture and skill documentation
scripts/                 ADB and smoke-test helpers
.github/workflows/       Continuous integration workflows
```

## Technology

| Area | Technology |
| --- | --- |
| Language and build | Kotlin, Java, Gradle, Android Gradle Plugin |
| UI | Jetpack Compose, Material 3 |
| Local inference | LiteRT-LM-compatible models |
| Cloud inference | OpenAI-compatible and provider-specific clients |
| Speech | Vosk and Android voice services |
| Perception | Android screen capture and ML Kit OCR |
| Networking and storage | OkHttp, Retrofit, Gson, MMKV, NanoHTTPD |
| Privileged control | libadb-android and optional Shizuku |

## Contributing

Contributions are welcome. Before opening a pull request:

1. create a focused branch from `master`;
2. keep user-facing text and documentation in English;
3. do not commit secrets, API keys, keystores, or generated build output;
4. run the relevant tests and lint checks;
5. describe the change, verification steps, and device/OEM details for Android
   behavior changes.

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the full workflow and coding
guidelines.

## Reporting bugs

Please include:

- device model, Android version, and manufacturer skin;
- BlaqClaw version and model/provider configuration;
- exact reproduction steps and expected behavior;
- relevant logs or the debug report generated from **Settings → About**.

Open an issue through [GitHub Issues](https://github.com/geniuscr8/BlaqClaw/issues).
Remove API keys, personal messages, screenshots, and other private data before
attaching diagnostic files.

## License

BlaqClaw is released under the [Apache License 2.0](LICENSE).
